package uy.um.faltauno.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uy.um.faltauno.dto.PhotoValidationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para validar fotos de perfil usando Google Cloud Vision API
 * Valida:
 * - Exactamente 1 rostro visible
 * - Contenido apropiado (no adulto, violento, etc.)
 * 
 * @version 1.1 - Google Cloud Vision API integration with proper error handling
 * 
 * TEMPORARILY DISABLED - Will re-enable after successful deployment
 */
// @Service  // TODO: Re-enable after deployment succeeds
@Slf4j
public class PhotoValidationService {

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
            try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
                
                // 1. Detectar rostros
                PhotoValidationResult faceResult = detectFaces(vision, img);
                if (!faceResult.isValid()) {
                    return faceResult;
                }

                // 2. Detectar contenido inapropiado
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

    /**
     * Detecta rostros en la imagen
     * Requiere EXACTAMENTE 1 rostro
     */
    private PhotoValidationResult detectFaces(ImageAnnotatorClient vision, Image img) {
        Feature faceDetectionFeature = Feature.newBuilder()
            .setType(Feature.Type.FACE_DETECTION)
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
                .message("No se detectó ningún rostro en la foto")
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
                .message("Se detectaron " + faceCount + " rostros. Por favor sube una foto con una sola persona")
                .reason("MULTIPLE_FACES")
                .build();
        }

        // Exactamente 1 rostro - calcular confianza
        FaceAnnotation face = res.getFaceAnnotations(0);
        double confidence = face.getDetectionConfidence();

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
        // VERY_LIKELY o LIKELY = bloquear
        boolean hasAdultContent = isLikelyOrVeryLikely(annotation.getAdult());
        boolean hasViolentContent = isLikelyOrVeryLikely(annotation.getViolence());
        boolean hasRacyContent = isLikelyOrVeryLikely(annotation.getRacy());

        log.info("[PhotoValidation] SafeSearch - Adult: {}, Violence: {}, Racy: {}", 
            annotation.getAdult(), annotation.getViolence(), annotation.getRacy());

        if (hasAdultContent || hasViolentContent || hasRacyContent) {
            String reason = hasAdultContent ? "ADULT_CONTENT" : 
                           hasViolentContent ? "VIOLENT_CONTENT" : "RACY_CONTENT";
            
            return PhotoValidationResult.builder()
                .valid(false)
                .hasFace(true)
                .faceCount(1)
                .isAppropriate(false)
                .confidence(0.0)
                .message("La foto contiene contenido inapropiado. Por favor elige otra imagen")
                .reason(reason)
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
     * Helper para verificar si el nivel de confianza es LIKELY o VERY_LIKELY
     */
    private boolean isLikelyOrVeryLikely(Likelihood likelihood) {
        return likelihood == Likelihood.LIKELY || likelihood == Likelihood.VERY_LIKELY;
    }
}
