package uy.um.faltauno.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uy.um.faltauno.dto.PhotoValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para validar fotos de perfil usando Google Cloud Vision API
 * Valida:
 * - Exactamente 1 rostro visible
 * - Contenido apropiado (no adulto, violento, etc.)
 * 
 * @version 1.1 - Google Cloud Vision API integration with proper error handling
 */
@Service
@Slf4j
public class PhotoValidationService {

    @Value("${google.vision.credentials-path:}")
    private String visionCredentialsPath;

    /**
     * Valida una foto de perfil
     * @param file Archivo de imagen a validar
     * @return Resultado de validación
     */
    public PhotoValidationResult validatePhoto(MultipartFile file) {
        try {
            log.info("[PhotoValidation] Validating photo: {}, size: {} bytes", 
                file.getOriginalFilename(), file.getSize());

            // Convertir archivo a ByteString para Vision API
            ByteString imgBytes = ByteString.copyFrom(file.getBytes());
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // Crear cliente de Vision API
            try (ImageAnnotatorClient vision = createVisionClient()) {
                
                // 1. Detectar calidad de imagen e iluminación
                PhotoValidationResult imageQualityResult = detectImageQuality(vision, img);
                if (!imageQualityResult.isValid()) {
                    return imageQualityResult;
                }

                // 2. Detectar rostros
                PhotoValidationResult faceResult = detectFaces(vision, img);
                if (!faceResult.isValid()) {
                    return faceResult;
                }

                // 3. Detectar contenido inapropiado
                PhotoValidationResult safeSearchResult = detectInappropriateContent(vision, img);
                if (!safeSearchResult.isValid()) {
                    return safeSearchResult;
                }

                // ✅ Todo válido
                log.info("[PhotoValidation] Photo validated successfully: 1 face, appropriate content");
                return PhotoValidationResult.builder()
                    .valid(true)
                    .hasFace(true)
                    .faceCount(1)
                    .isAppropriate(true)
                    .confidence(faceResult.getConfidence())
                    .message("Foto validada correctamente")
                    .build();
            }

        } catch (Exception e) {
            // ⚠️ Capturar TODAS las excepciones (IOException, RuntimeException, etc.)
            log.error("[PhotoValidation] Error validating photo", e);
            
            // ⚠️ CRITICAL: NO permitir fotos si la validación falla
            // Si Google Cloud Vision API no está configurado o falla, RECHAZAR la foto
            String errorMessage = "No se pudo validar la foto. ";
            
            if (e.getMessage() != null && e.getMessage().contains("credentials")) {
                errorMessage += "El servicio de validación no está configurado correctamente.";
                log.error("[PhotoValidation] ❌ Google Cloud Vision API credentials not configured");
            } else if (e.getMessage() != null && e.getMessage().contains("quota")) {
                errorMessage += "Se ha excedido el límite de validaciones. Intenta más tarde.";
                log.error("[PhotoValidation] ❌ Google Cloud Vision API quota exceeded");
            } else {
                errorMessage += "Por favor intenta con otra foto o contacta soporte.";
            }
            
            return PhotoValidationResult.builder()
                .valid(false) // ✅ RECHAZAR si hay error
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(false)
                .confidence(0.0)
                .message(errorMessage)
                .reason("API_ERROR")
                .build();
        }
    }

    private ImageAnnotatorClient createVisionClient() throws IOException {
        if (StringUtils.hasText(visionCredentialsPath)) {
            Path credentialsPath = Path.of(visionCredentialsPath);
            if (!Files.exists(credentialsPath)) {
                log.warn("[PhotoValidation] Credentials path {} does not exist. Falling back to ADC", visionCredentialsPath);
            } else {
                GoogleCredentials credentials;
                try (InputStream input = Files.newInputStream(credentialsPath)) {
                    credentials = GoogleCredentials.fromStream(input)
                        .createScoped(ImageAnnotatorSettings.getDefaultServiceScopes());
                }

                ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
                log.info("[PhotoValidation] Using custom Vision credentials file at {}", visionCredentialsPath);
                return ImageAnnotatorClient.create(settings);
            }
        }

        log.info("[PhotoValidation] Using default application credentials for Vision API");
        return ImageAnnotatorClient.create();
    }

    /**
     * Detecta calidad de imagen e iluminación usando Image Properties
     * Rechaza fotos con muy poca luz o calidad degradada
     */
    private PhotoValidationResult detectImageQuality(ImageAnnotatorClient vision, Image img) {
        Feature imagePropertiesFeature = Feature.newBuilder()
            .setType(Feature.Type.IMAGE_PROPERTIES)
            .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(imagePropertiesFeature)
            .setImage(img)
            .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        if (responses.isEmpty() || !responses.get(0).hasImagePropertiesAnnotation()) {
            log.warn("[PhotoValidation] No ImageProperties response");
            // Continuar sin esta validación si no está disponible
            return PhotoValidationResult.builder()
                .valid(true)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("Calidad no verificada")
                .build();
        }

        var imageProperties = responses.get(0).getImagePropertiesAnnotation();
        var dominantColors = imageProperties.getDominantColors();

        if (dominantColors.getColorsCount() == 0) {
            log.warn("[PhotoValidation] No color information available");
            return PhotoValidationResult.builder()
                .valid(true)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("Calidad no verificada")
                .build();
        }

        // Calcular brillo promedio de la imagen
        float totalBrightness = 0;
        float totalScore = 0;
        
        for (var colorInfo : dominantColors.getColorsList()) {
            var color = colorInfo.getColor();
            float score = colorInfo.getScore();
            
            // Calcular luminosidad usando la fórmula estándar
            float brightness = (0.299f * color.getRed() + 0.587f * color.getGreen() + 0.114f * color.getBlue());
            totalBrightness += brightness * score;
            totalScore += score;
        }

        float averageBrightness = totalBrightness / totalScore;
        log.info("[PhotoValidation] Image average brightness: {}", averageBrightness);

        // Rechazar imágenes muy oscuras (brightness < 60) o muy brillantes (brightness > 220)
        // Rangos más estrictos para asegurar buena visibilidad facial
        if (averageBrightness < 60) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("La foto está muy oscura. Busca mejor iluminación natural o artificial y vuelve a intentar.")
                .reason("POOR_LIGHTING")
                .build();
        }

        if (averageBrightness > 220) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("La foto está sobreexpuesta. Reduce la iluminación directa y vuelve a intentar.")
                .reason("OVEREXPOSED")
                .build();
        }

        // Verificar si hay demasiados colores muy oscuros o muy claros (indica contraluz o mala calidad)
        int veryDarkColors = 0;
        int veryBrightColors = 0;
        
        for (var colorInfo : dominantColors.getColorsList()) {
            var color = colorInfo.getColor();
            float brightness = (0.299f * color.getRed() + 0.587f * color.getGreen() + 0.114f * color.getBlue());
            
            if (brightness < 50) veryDarkColors++;
            if (brightness > 230) veryBrightColors++;
        }

        // Si más del 40% de los colores dominantes son extremos, rechazar (más estricto)
        int totalColors = dominantColors.getColorsCount();
        if ((veryDarkColors + veryBrightColors) > totalColors * 0.4) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("La foto tiene contraluz o iluminación despareja. Busca luz frontal uniforme y evita ventanas de fondo.")
                .reason("BAD_LIGHTING_CONDITIONS")
                .build();
        }

        log.info("[PhotoValidation] Image quality check passed - brightness: {}", averageBrightness);
        
        return PhotoValidationResult.builder()
            .valid(true)
            .hasFace(false)
            .faceCount(0)
            .isAppropriate(true)
            .confidence(1.0)
            .message("Calidad de imagen aceptable")
            .build();
    }

    /**
     * Detecta rostros en la imagen
     * Requiere EXACTAMENTE 1 rostro con validaciones avanzadas de calidad
     */
    private PhotoValidationResult detectFaces(ImageAnnotatorClient vision, Image img) {
        Feature faceDetectionFeature = Feature.newBuilder()
            .setType(Feature.Type.FACE_DETECTION)
            .setMaxResults(10) // Detectar hasta 10 rostros para mejor análisis
            .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(faceDetectionFeature)
            .setImage(img)
            .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        if (responses.isEmpty()) {
            log.warn("[PhotoValidation] No response from Vision API");
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("No se pudo analizar la imagen")
                .reason("NO_RESPONSE")
                .build();
        }

        AnnotateImageResponse res = responses.get(0);
        
        if (res.hasError()) {
            log.error("[PhotoValidation] Vision API error: {}", res.getError().getMessage());
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("Error al analizar la imagen")
                .reason("API_ERROR")
                .build();
        }

        int faceCount = res.getFaceAnnotationsCount();
        log.info("[PhotoValidation] Detected {} face(s)", faceCount);

        // ⚡ CRITICAL: Require EXACTLY 1 face
        if (faceCount == 0) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(false)
                .faceCount(0)
                .isAppropriate(true)
                .confidence(0.0)
                .message("No se detectó ningún rostro en la foto. Asegúrate de que tu cara sea visible y esté bien iluminada.")
                .reason("NO_FACE")
                .build();
        }

        if (faceCount > 1) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(faceCount)
                .isAppropriate(true)
                .confidence(0.0)
                .message("Se detectaron " + faceCount + " rostros. Por favor sube una foto con una sola persona.")
                .reason("MULTIPLE_FACES")
                .build();
        }

        // Exactamente 1 rostro - validaciones avanzadas de calidad
        FaceAnnotation face = res.getFaceAnnotations(0);
        double confidence = face.getDetectionConfidence();
        
        log.info("[PhotoValidation] Face detection confidence: {}", confidence);

        // 1. Verificar confianza mínima de detección (muy estricta: 0.75 para asegurar nitidez)
        if (confidence < 0.75) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(true)
                .confidence(confidence)
                .message("La foto está borrosa o poco clara. Asegúrate de que tu cara esté enfocada y con buena iluminación.")
                .reason("LOW_CONFIDENCE")
                .build();
        }

        // 2. Verificar que el rostro esté centrado y visible (ángulos muy estrictos)
        float panAngle = face.getPanAngle();
        float tiltAngle = face.getTiltAngle();
        float rollAngle = face.getRollAngle();
        
        log.info("[PhotoValidation] Face angles - Pan: {}, Tilt: {}, Roll: {}", panAngle, tiltAngle, rollAngle);
        
        // Rechazar ángulos extremos (muy estricto: >25 grados para pan/tilt, >15 para roll)
        if (Math.abs(panAngle) > 25 || Math.abs(tiltAngle) > 25 || Math.abs(rollAngle) > 15) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(true)
                .confidence(confidence)
                .message("Tu rostro debe estar completamente de frente a la cámara. Mantén la cabeza recta y el teléfono nivelado.")
                .reason("EXTREME_ANGLE")
                .build();
        }

        // 3. Verificar que los ojos y boca sean visibles (landmarks) - muy estricto: mínimo 10 para asegurar claridad
        int visibleLandmarks = face.getLandmarksCount();
        log.info("[PhotoValidation] Visible facial landmarks: {}", visibleLandmarks);
        
        if (visibleLandmarks < 10) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(true)
                .confidence(confidence)
                .message("Tu rostro no se ve con suficiente claridad. Evita gafas oscuras, mascarillas o accesorios. Asegúrate de tener buena iluminación frontal.")
                .reason("FACE_OCCLUDED")
                .build();
        }

        // 4. Verificar emociones extremas o expresiones no apropiadas para foto de perfil
        Likelihood anger = face.getAngerLikelihood();
        Likelihood sorrow = face.getSorrowLikelihood();
        
        log.info("[PhotoValidation] Emotions - Anger: {}, Sorrow: {}, Joy: {}", 
            anger, sorrow, face.getJoyLikelihood());
        
        if (anger == Likelihood.VERY_LIKELY || sorrow == Likelihood.VERY_LIKELY) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(true)
                .confidence(confidence)
                .message("Por favor usa una foto con expresión neutra o sonriente para tu perfil.")
                .reason("INAPPROPRIATE_EXPRESSION")
                .build();
        }

        // ✅ Todas las validaciones pasaron
        log.info("[PhotoValidation] Face validation passed - confidence: {}, landmarks: {}", confidence, visibleLandmarks);
        
        return PhotoValidationResult.builder()
            .valid(true)
            .hasFace(true)
            .faceCount(1)
            .isAppropriate(true)
            .confidence(confidence)
            .message("Rostro detectado correctamente")
            .build();
    }

    /**
     * Detecta contenido inapropiado usando SafeSearch
     * Con umbrales estrictos para fotos de perfil
     */
    private PhotoValidationResult detectInappropriateContent(ImageAnnotatorClient vision, Image img) {
        Feature safeSearchFeature = Feature.newBuilder()
            .setType(Feature.Type.SAFE_SEARCH_DETECTION)
            .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(safeSearchFeature)
            .setImage(img)
            .build();

        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        if (responses.isEmpty() || !responses.get(0).hasSafeSearchAnnotation()) {
            log.warn("[PhotoValidation] No SafeSearch response");
            // Permitir si no hay respuesta (graceful degradation)
            return PhotoValidationResult.builder()
                .valid(true)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(true)
                .confidence(0.0)
                .message("Contenido no verificado")
                .build();
        }

        SafeSearchAnnotation annotation = responses.get(0).getSafeSearchAnnotation();

        // Verificar niveles de contenido inapropiado
        // VERY_LIKELY, LIKELY o POSSIBLE = bloquear (más estricto)
        // Para fotos de perfil, ser muy estricto con el contenido
        boolean hasAdultContent = isPossibleLikelyOrVeryLikely(annotation.getAdult());
        boolean hasViolentContent = isPossibleLikelyOrVeryLikely(annotation.getViolence());
        boolean hasRacyContent = isPossibleLikelyOrVeryLikely(annotation.getRacy());
        boolean hasSpoofContent = isPossibleLikelyOrVeryLikely(annotation.getSpoof());

        log.info("[PhotoValidation] SafeSearch - Adult: {}, Violence: {}, Racy: {}, Spoof: {}", 
            annotation.getAdult(), annotation.getViolence(), annotation.getRacy(), annotation.getSpoof());

        if (hasAdultContent) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(false)
                .confidence(0.0)
                .message("La foto contiene contenido adulto. Usa una foto apropiada para tu perfil.")
                .reason("ADULT_CONTENT")
                .build();
        }
        
        if (hasViolentContent) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(false)
                .confidence(0.0)
                .message("La foto contiene contenido violento. Elige una foto apropiada.")
                .reason("VIOLENT_CONTENT")
                .build();
        }
        
        if (hasRacyContent) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(false)
                .confidence(0.0)
                .message("La foto contiene contenido sugerente. Por favor elige otra imagen.")
                .reason("RACY_CONTENT")
                .build();
        }
        
        if (hasSpoofContent) {
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(false)
                .confidence(0.0)
                .message("La foto parece ser falsa o modificada. Usa una foto real.")
                .reason("SPOOF_CONTENT")
                .build();
        }

        return PhotoValidationResult.builder()
            .valid(true)
            .hasFace(true)
            .faceCount(1)
            .isAppropriate(true)
            .confidence(1.0)
            .message("Contenido apropiado")
            .build();
    }

    /**
     * Helper para verificar si el nivel de confianza es POSSIBLE, LIKELY o VERY_LIKELY
     * Más estricto para fotos de perfil
     */
    private boolean isPossibleLikelyOrVeryLikely(Likelihood likelihood) {
        return likelihood == Likelihood.POSSIBLE || 
               likelihood == Likelihood.LIKELY || 
               likelihood == Likelihood.VERY_LIKELY;
    }
}
