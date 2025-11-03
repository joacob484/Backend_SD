package uy.um.faltauno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import uy.um.faltauno.dto.PendingReviewResponse;
import uy.um.faltauno.dto.PerfilDTO;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.util.UsuarioMapper;
import uy.um.faltauno.entity.Amistad;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Mensaje;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.AmistadRepository;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.MensajeRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.ReviewRepository;
import uy.um.faltauno.repository.UsuarioRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AmistadRepository amistadRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioMapper usuarioMapper;
    private final ReviewRepository reviewRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PartidoRepository partidoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Encuentra el ID de un usuario por email SIN cargar LOBs.
     * Usa la proyección AuthProjection que solo trae id, email, password.
     */
    public UUID findUserIdByEmail(String email) {
        return usuarioRepository.findAuthProjectionByEmail(email)
                .map(UsuarioRepository.AuthProjection::getId)
                .orElse(null);
    }

    /**
     * Verifica la validez de una cédula de identidad uruguaya usando el algoritmo oficial.
     * 
     * Algoritmo:
     * - Las cédulas tienen 7 u 8 dígitos numéricos
     * - El último dígito es un dígito verificador calculado con módulo 10
     * - Se multiplican los primeros 7 dígitos por pesos [2,9,8,7,6,3,4]
     * - Se suma y se calcula módulo 10
     * - El dígito verificador es (10 - resto) % 10
     * 
     * @param cedula Cédula a verificar (puede incluir puntos, guiones, etc.)
     * @return true si la cédula es válida, false en caso contrario
     */
    public boolean verificarCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            return false;
        }

        // ⚡ NUEVO: Validar longitud antes de limpiar (prevenir inputs maliciosos)
        // Una cédula válida debe tener entre 7 y 20 caracteres (permitiendo puntos/guiones)
        if (cedula.length() < 7 || cedula.length() > 20) {
            return false;
        }

        // Limpiar: solo dígitos
        String clean = cedula.replaceAll("[^\\d]", "");
        
        // ⚡ Validar longitud después de limpiar
        if (clean.length() < 7 || clean.length() > 8) {
            return false;
        }
        
        // Completar con ceros a la izquierda si tiene 7 dígitos
        while (clean.length() < 8) {
            clean = "0" + clean;
        }

        int[] pesos = {2, 9, 8, 7, 6, 3, 4};
        int[] digitos = clean.chars().map(c -> c - '0').toArray();

        int suma = 0;
        for (int i = 0; i < pesos.length; i++) {
            suma += digitos[i] * pesos[i];
        }

        int resto = suma % 10;
        int verificadorCalculado = (resto == 0) ? 0 : 10 - resto;

        return verificadorCalculado == digitos[7];
    }

    @Transactional
    public UsuarioDTO saveCedulaForUser(UUID usuarioId, String cedula) {
        if (usuarioId == null) throw new RuntimeException("Usuario no encontrado");
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setCedula(cedula);
        usuario = usuarioRepository.save(usuario);

        UsuarioDTO dto = usuarioMapper.toDTO(usuario);
        dto.setPassword(null);
        return dto;
    }

    @Transactional
    public UsuarioDTO createUsuario(UsuarioDTO dto) {
        if (dto.getEmail() == null || dto.getPassword() == null) {
            throw new IllegalArgumentException("Email y password son requeridos");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setProvider("LOCAL");
        usuario.setCreatedAt(LocalDateTime.now());

        usuario = usuarioRepository.save(usuario);

        // Enviar email de bienvenida de forma asíncrona
        try {
            emailService.enviarEmailBienvenida(usuario);
        } catch (Exception e) {
            // No fallar el registro si el email falla
            // El log se maneja en EmailService
        }

        UsuarioDTO out = usuarioMapper.toDTO(usuario);
        out.setPassword(null);
        return out;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usuarios", key = "#id")
    public UsuarioDTO getUsuario(UUID id) {
        // Primero verificar si el usuario existe (incluyendo soft-deleted)
        if (usuarioRepository.existsByIdIncludingDeleted(id)) {
            // Si existe pero no lo encuentra en findById, significa que está soft-deleted
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
            if (usuarioOpt.isEmpty()) {
                throw new RuntimeException("Usuario eliminado");
            }
            
            UsuarioDTO dto = usuarioMapper.toDTO(usuarioOpt.get());
            
            // Asegurar que los campos calculados están correctos
            dto.setPerfilCompleto(dto.getPerfilCompleto());
            dto.setCedulaVerificada(dto.getCedulaVerificada());
            
            return dto;
        }
        
        // Si no existe en absoluto
        throw new RuntimeException("Usuario no encontrado");
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "usuarios", key = "#usuarioId"),
        @CacheEvict(value = "sugerencias", allEntries = true)
    })
    public Usuario actualizarPerfil(UUID usuarioId, PerfilDTO perfilDTO) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ Validaciones de campos
        if (perfilDTO.getNombre() != null && perfilDTO.getNombre().length() > 100) {
            throw new IllegalArgumentException("Nombre demasiado largo (máx 100 caracteres)");
        }
        if (perfilDTO.getApellido() != null && perfilDTO.getApellido().length() > 100) {
            throw new IllegalArgumentException("Apellido demasiado largo (máx 100 caracteres)");
        }

        usuario.setNombre(perfilDTO.getNombre());
        usuario.setApellido(perfilDTO.getApellido());
        usuario.setCelular(perfilDTO.getCelular());
        usuario.setPosicion(perfilDTO.getPosicion());
        
        // ✅ Validar altura (frontend envía en cm, convertir a metros)
        if (perfilDTO.getAltura() != null && !perfilDTO.getAltura().isEmpty()) {
            try {
                Double alturaCm = Double.valueOf(perfilDTO.getAltura());
                // Validar rango en cm (100-250 cm razonable)
                if (alturaCm < 100.0 || alturaCm > 250.0) {
                    throw new IllegalArgumentException("Altura debe estar entre 100cm y 250cm");
                }
                // Convertir cm → metros para almacenar
                usuario.setAltura(alturaCm / 100.0);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Altura inválida");
            }
        } else {
            usuario.setAltura(null);
        }
        
        // ✅ Validar peso (30kg - 200kg razonable)
        if (perfilDTO.getPeso() != null && !perfilDTO.getPeso().isEmpty()) {
            try {
                Double peso = Double.valueOf(perfilDTO.getPeso());
                if (peso < 30.0 || peso > 200.0) {
                    throw new IllegalArgumentException("Peso debe estar entre 30kg y 200kg");
                }
                usuario.setPeso(peso);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Peso inválido");
            }
        } else {
            usuario.setPeso(null);
        }
        
        // Mapear género si está presente
        if (perfilDTO.getGenero() != null && !perfilDTO.getGenero().isBlank()) {
            usuario.setGenero(perfilDTO.getGenero());
        }

        try {
            String fechaStr = perfilDTO.getFechaNacimiento();
            if (fechaStr != null && !fechaStr.isBlank()) {
                LocalDate ld = LocalDate.parse(fechaStr);
                // ✅ Validar edad razonable (13-120 años)
                LocalDate hoy = LocalDate.now();
                if (ld.isAfter(hoy.minusYears(13))) {
                    throw new IllegalArgumentException("Debes tener al menos 13 años");
                }
                if (ld.isBefore(hoy.minusYears(120))) {
                    throw new IllegalArgumentException("Fecha de nacimiento inválida");
                }
                usuario.setFechaNacimiento(ld);
            }
        } catch (DateTimeParseException dtpe) {
            throw new IllegalArgumentException("Formato de fecha_nacimiento inválido. Use yyyy-MM-dd");
        } catch (IllegalArgumentException e) {
            throw e; // Re-lanzar validaciones de edad
        } catch (Exception ignore) {
            // si PerfilDTO no contiene fechaNacimiento, se ignora
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public void subirFoto(UUID usuarioId, MultipartFile file) throws IOException {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ FIX: Validar tamaño de archivo (máx 5MB para evitar OOM)
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("La foto no puede superar 5MB");
        }
        
        // ✅ Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/jpeg") && 
                                    !contentType.startsWith("image/png") && 
                                    !contentType.startsWith("image/jpg"))) {
            throw new IllegalArgumentException("Solo se permiten imágenes JPEG o PNG");
        }

        usuario.setFotoPerfil(file.getBytes());
        usuarioRepository.save(usuario);
    }

    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public Usuario marcarCedula(UUID usuarioId, String cedula) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setCedula(cedula);
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> getAllUsuarios() {
        // Usar findAllActive en lugar de findAll para excluir usuarios eliminados
        return usuarioRepository.findAllActive()
                .stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PendingReviewResponse> obtenerPendingReviews(UUID userId) {
        List<Inscripcion> misInscripciones = inscripcionRepository.findByUsuarioId(userId);

        Map<UUID, List<UsuarioMinDTO>> pendientesPorPartido = new HashMap<>();

        for (Inscripcion miInsc : misInscripciones) {
            Partido partido = miInsc.getPartido();
            if (partido == null) continue;

            UUID partidoId = partido.getId();
            List<Inscripcion> inscDelPartido = inscripcionRepository.findByPartidoId(partidoId);

            for (Inscripcion inscOtro : inscDelPartido) {
                if (inscOtro.getUsuario() == null) continue;
                UUID otroUsuarioId = inscOtro.getUsuario().getId();
                if (userId.equals(otroUsuarioId)) continue;

                boolean yaReseñado = reviewRepository.existsByPartido_IdAndUsuarioQueCalifica_IdAndUsuarioCalificado_Id(
                        partidoId,
                        userId,
                        otroUsuarioId
                );

                if (!yaReseñado) {
                    Usuario u = inscOtro.getUsuario();
                    // ✅ FIX: No acceder a fotoPerfil LAZY aquí - causa LazyInitializationException
                    // Solo pasar null, el frontend cargará la foto con endpoint separado
                    UsuarioMinDTO um = new UsuarioMinDTO(u.getId(), u.getNombre(), u.getApellido(), null);
                    pendientesPorPartido.computeIfAbsent(partidoId, k -> new ArrayList<>()).add(um);
                }
            }
        }

        List<PendingReviewResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, List<UsuarioMinDTO>> entry : pendientesPorPartido.entrySet()) {
            UUID partidoId = entry.getKey();
            List<UsuarioMinDTO> jugadoresPendientes = entry.getValue();

            Partido partido = partidoRepository.findById(partidoId).orElse(null);
            if (partido == null) continue;

            PendingReviewResponse pr = new PendingReviewResponse();
            pr.setPartido_id(partido.getId());
            pr.setTipo_partido(partido.getTipoPartido());
            pr.setFecha(partido.getFecha() != null ? partido.getFecha().toString() : null);
            pr.setNombre_ubicacion(partido.getNombreUbicacion());
            pr.setJugadores_pendientes(jugadoresPendientes);

            result.add(pr);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerSolicitudesAmistadPendientes(UUID userId) {
        List<Amistad> pendientes = amistadRepository.findByAmigoIdAndEstado(userId, "PENDIENTE");
        return pendientes.stream().map(a -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("requesterId", a.getUsuarioId());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerMensajesNoLeidos(UUID userId) {
        List<Mensaje> mensajes = mensajeRepository.findByDestinatarioIdAndLeido(userId, false);
        return mensajes.stream().map(m -> {
            Map<String,Object> mm = new HashMap<>();
            mm.put("id", m.getId());
            mm.put("senderId", m.getRemitenteId());
            mm.put("message", m.getContenido());
            mm.put("createdAt", m.getCreatedAt());
            return mm;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerInvitaciones(UUID userId) {
        List<Inscripcion> pendientes = inscripcionRepository.findByUsuario_IdAndEstado(userId, Inscripcion.EstadoInscripcion.PENDIENTE);
        return pendientes.stream().map(i -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", i.getId());
            m.put("usuarioId", i.getUsuario().getId());
            m.put("matchId", i.getPartido().getId());
            m.put("title", "Invitación a partido");
            m.put("message", "Te han invitado al partido " + i.getPartido().getNombreUbicacion());
            m.put("time", i.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerActualizacionesPartidos(UUID userId) {
        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(userId);
        List<Map<String,Object>> updates = new ArrayList<>();
        for (Inscripcion insc : inscripciones) {
            Partido p = insc.getPartido();
            if (p == null) continue;
            Map<String,Object> m = new HashMap<>();
            m.put("partidoId", p.getId());
            m.put("tipo_partido", p.getTipoPartido());
            m.put("nombre_ubicacion", p.getNombreUbicacion());
            m.put("fecha", p.getFecha());
            m.put("hora", p.getHora());
            m.put("inscripcionId", insc.getId());
            m.put("inscripcionEstado", insc.getEstado());
            updates.add(m);
        }
        return updates;
    }

    @Transactional(readOnly = true)
    public Usuario findUsuarioEntityById(UUID id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    /**
     * Busca usuario por email (carga TODA la entidad incluyendo LOB).
     * SOLO usar dentro de transacciones activas.
     */
    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public void deleteUsuario(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (usuario.getDeletedAt() != null) {
            throw new IllegalStateException("Usuario ya eliminado");
        }
        
        // 1️⃣ Cancelar todos los partidos que organiza el usuario
        java.util.List<Partido> partidosOrganizados = partidoRepository.findByOrganizador_Id(id);
        int partidosCancelados = 0;
        for (Partido partido : partidosOrganizados) {
            // Solo cancelar partidos disponibles
            if ("DISPONIBLE".equals(partido.getEstado())) {
                partido.setEstado("CANCELADO");
                partidoRepository.save(partido);
                partidosCancelados++;
                log.info("🔴 Partido cancelado (usuario eliminado): partidoId={}, tipo={}", 
                    partido.getId(), partido.getTipoPartido());
            }
        }
        
        // 2️⃣ Soft delete: marcar usuario como eliminado
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
        
        log.info("✅ Usuario soft-deleted: id={}, email={}, partidosCancelados={}", 
            id, usuario.getEmail(), partidosCancelados);
    }

    /**
     * Recuperar usuario eliminado (dentro del plazo de 30 días)
     * 
     * @param email Email del usuario a recuperar
     * @throws IllegalArgumentException si no existe usuario eliminado con ese email
     * @throws IllegalStateException si ya pasaron 30 días desde la eliminación
     */
    @Transactional
    public Usuario recoverDeletedUser(String email) {
        Usuario usuario = usuarioRepository.findDeletedByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe usuario eliminado con ese email"));
        
        // Verificar que no hayan pasado 30 días
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        if (usuario.getDeletedAt().isBefore(cutoffDate)) {
            throw new IllegalStateException("El plazo de recuperación (30 días) ha expirado. Debe crear una cuenta nueva.");
        }
        
        // Restaurar cuenta: quitar marca de eliminación
        usuario.setDeletedAt(null);
        usuarioRepository.save(usuario);
        
        log.info("✅ Usuario recuperado: id={}, email={}", usuario.getId(), email);
        return usuario;
    }

    /**
     * Verificar si existe un usuario eliminado recuperable
     * 
     * @param email Email a verificar
     * @return true si existe y está dentro del plazo de 30 días
     */
    @Transactional(readOnly = true)
    public boolean hasRecoverableDeletedUser(String email) {
        Optional<Usuario> deletedUser = usuarioRepository.findDeletedByEmail(email);
        if (deletedUser.isEmpty()) {
            return false;
        }
        
        // Verificar que no hayan pasado 30 días
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return deletedUser.get().getDeletedAt().isAfter(cutoffDate);
    }

    /**
     * Cleanup: eliminar físicamente usuarios eliminados hace más de 30 días.
     * Ejecutado automáticamente por scheduled task.
     * 
     * @return Cantidad de usuarios eliminados físicamente
     */
    @Transactional
    public int cleanupExpiredDeletedUsers() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        java.util.List<Usuario> expiredUsers = usuarioRepository.findExpiredDeletedUsers(cutoffDate);
        
        if (expiredUsers.isEmpty()) {
            log.debug("🧹 Cleanup: No hay usuarios eliminados expirados");
            return 0;
        }
        
        log.info("🧹 Cleanup: Eliminando físicamente {} usuarios expirados (eliminados hace >30 días)", expiredUsers.size());
        
        // Eliminar físicamente cada usuario
        for (Usuario user : expiredUsers) {
            try {
                log.info("🗑️ Eliminando físicamente: id={}, email={}, deletedAt={}", 
                         user.getId(), user.getEmail(), user.getDeletedAt());
                usuarioRepository.delete(user);
            } catch (Exception e) {
                // Si falla por foreign keys, loggear pero continuar
                log.warn("⚠️ No se pudo eliminar usuario {}: {}", user.getId(), e.getMessage());
            }
        }
        
        log.info("✅ Cleanup completado: {} usuarios eliminados físicamente", expiredUsers.size());
        return expiredUsers.size();
    }

    @Transactional
    /**
     * Crea o actualiza un usuario desde Google OAuth.
     * IMPORTANTE: Los usuarios de Google OAuth NO tienen contraseña (password = null).
     * Solo pueden autenticarse mediante OAuth, no mediante email/password.
     * 
     * @param email Email del usuario desde Google
     * @param name Nombre completo del usuario desde Google
     * @param attrs Atributos adicionales de Google (sub, picture, etc.)
     * @return Usuario creado o actualizado
     */
    public Usuario upsertGoogleUser(String email, String name, Map<String, Object> attrs) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email de Google inválido");
        }

        log.info("[OAuth] 🔍 Buscando usuario existente por email: {}", email);
        Optional<Usuario> existenteOpt = usuarioRepository.findByEmail(email);

        Usuario u = existenteOpt.orElseGet(() -> {
            log.info("[OAuth] ✨ Usuario nuevo - creando registro para: {}", email);
            return new Usuario();
        });
        
        if (existenteOpt.isPresent()) {
            log.info("[OAuth] ♻️ Usuario existente encontrado - actualizando datos para: {}", email);
        }
        
        u.setEmail(email);
        
        // IMPORTANTE: NO seteamos password - los usuarios OAuth no tienen contraseña
        // Solo pueden autenticarse mediante el flujo OAuth
        
        // Marcar como usuario de Google OAuth
        u.setProvider("GOOGLE");

        // name (si viene)
        if (name != null && !name.isBlank()) {
            u.setNombre(name);
        }

        // Campos opcionales: no fallar si no existen en la entidad
        // (si tu entidad no tiene estos setters, simplemente quitá las líneas)
        Object sub = attrs != null ? attrs.get("sub") : null;
        if (sub != null) safeSet(u, "setProviderSub", sub.toString());

        Object picture = attrs != null ? attrs.get("picture") : null;
        if (picture != null) safeSet(u, "setFotoUrl", picture.toString());

        // Podés marcar verificado si tu modelo lo contempla
        safeSet(u, "setEmailVerificado", true);

        // Si tenés un enum/provider, setealo (si no existe el método, no pasa nada)
        safeSet(u, "setProveedor", enumValueOfSafely("GOOGLE", "uy.um.faltauno.model.AuthProvider"));
        
        // Setear createdAt si es nuevo usuario
        if (u.getCreatedAt() == null) {
            u.setCreatedAt(LocalDateTime.now());
        }

        log.info("[OAuth] 💾 Guardando usuario en DB: {}", email);
        Usuario saved = usuarioRepository.save(u);
        
        // ⚡ CRÍTICO: Forzar flush para asegurar que los datos se persisten INMEDIATAMENTE
        // Esto previene problemas de timing donde el frontend intenta leer el usuario
        // antes de que la transacción se complete
        usuarioRepository.flush();
        
        log.info("[OAuth] ✅ Usuario guardado exitosamente - ID: {}, Email: {}", saved.getId(), saved.getEmail());
        log.info("[OAuth] 🔍 Verificando que el usuario fue guardado correctamente...");
        
        // Verificar que el usuario realmente existe en la DB
        boolean exists = usuarioRepository.existsById(saved.getId());
        if (!exists) {
            log.error("[OAuth] ❌ ERROR CRÍTICO: Usuario NO existe en DB después de save+flush!");
            throw new RuntimeException("Error al persistir usuario OAuth");
        }
        
        log.info("[OAuth] ✓ Verificación exitosa - Usuario persiste en DB");
        return saved;
    }

    /**
     * Obtiene amigos sugeridos para un usuario.
     * Retorna amigos reales + sugerencias inteligentes.
     */
    public List<UsuarioDTO> obtenerAmigosSugeridos(UUID usuarioId) {
        // Validar que el usuario existe
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        
        // Obtener amigos reales del usuario
        List<Usuario> amigosReales = amistadRepository.findAmigosByUsuarioId(usuarioId)
                .stream()
                .map(amistad -> {
                    UUID amigoId = amistad.getUsuarioId().equals(usuarioId) 
                            ? amistad.getAmigoId() 
                            : amistad.getUsuarioId();
                    return usuarioRepository.findById(amigoId).orElse(null);
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());

        // Si tiene amigos reales, retornarlos
        if (!amigosReales.isEmpty()) {
            return amigosReales.stream()
                    .map(usuarioMapper::toDTO)
                    .peek(dto -> dto.setPassword(null))
                    .collect(Collectors.toList());
        }

        // Si no tiene amigos, sugerir usuarios inteligentemente
        return obtenerSugerenciasInteligentes(usuarioId);
    }

    /**
     * Sugerencias inteligentes de amistad basadas en:
     * - Partidos en común
     * - Nivel de habilidad similar
     * - Proximidad geográfica
     */
    @Cacheable(value = "sugerencias", key = "#usuarioId")
    private List<UsuarioDTO> obtenerSugerenciasInteligentes(UUID usuarioId) {
        // Validar que el usuario existe
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // FUTURO: Implementar filtrado inteligente usando nivel, ubicación, historial de partidos, etc.
        // Por ahora, recomendaciones básicas son suficientes para el MVP

        // Obtener todos los usuarios ACTIVOS (excluye soft-deleted automáticamente)
        List<Usuario> candidatos = usuarioRepository.findAllActive().stream()
                .filter(u -> !u.getId().equals(usuarioId))
                .filter(u -> u.getNombre() != null && u.getEmail() != null)
                .collect(Collectors.toList());

        // Obtener IDs de amigos actuales y solicitudes pendientes para excluirlos
        List<UUID> idsExcluir = new java.util.ArrayList<>();
        idsExcluir.add(usuarioId);
        
        amistadRepository.findAmigosByUsuarioId(usuarioId).forEach(amistad -> {
            UUID amigoId = amistad.getUsuarioId().equals(usuarioId) 
                    ? amistad.getAmigoId() 
                    : amistad.getUsuarioId();
            idsExcluir.add(amigoId);
        });

        // Excluir solicitudes pendientes enviadas
        amistadRepository.findByUsuarioIdAndEstado(usuarioId, "PENDIENTE")
                .forEach(amistad -> idsExcluir.add(amistad.getAmigoId()));

        // Excluir solicitudes pendientes recibidas
        amistadRepository.findByAmigoIdAndEstado(usuarioId, "PENDIENTE")
                .forEach(amistad -> idsExcluir.add(amistad.getUsuarioId()));

        // Filtrar candidatos
        List<Usuario> sugerencias = candidatos.stream()
                .filter(u -> !idsExcluir.contains(u.getId()))
                .limit(20)
                .collect(Collectors.toList());

        return sugerencias.stream()
                .map(usuarioMapper::toDTO)
                .peek(dto -> dto.setPassword(null))
                .collect(Collectors.toList());
    }

    // ---- utilidades reflectivas seguras (evitan romper si el campo no existe en tu entidad) ----

    private void safeSet(Object target, String setterName, Object value) {
        try {
            var m = target.getClass().getMethod(setterName, value.getClass());
            m.invoke(target, value);
        } catch (NoSuchMethodException e) {
            // intentar con tipos más generales (Boolean/boolean, etc.)
            try {
                if (value instanceof Boolean) {
                    var m = target.getClass().getMethod(setterName, boolean.class);
                    m.invoke(target, (boolean) value);
                    return;
                }
                if (value instanceof Integer) {
                    var m = target.getClass().getMethod(setterName, int.class);
                    m.invoke(target, (int) value);
                    return;
                }
            } catch (Exception ignored) {}
            // si no existe el setter, lo ignoramos
        } catch (Exception ignored) {
        }
    }

    private Object enumValueOfSafely(String name, String enumFqn) {
        try {
            Class<?> enumCls = Class.forName(enumFqn);
            if (enumCls.isEnum()) {
                @SuppressWarnings({"rawtypes","unchecked"})
                Object val = Enum.valueOf((Class<? extends Enum>) enumCls, name);
                return val;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ================================
    // Preferencias de notificación
    // ================================

    /**
     * Obtener preferencias de notificación del usuario
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationPreferences(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("matchInvitations", usuario.getNotifEmailInvitaciones() != null ? usuario.getNotifEmailInvitaciones() : true);
        preferences.put("friendRequests", usuario.getNotifEmailSolicitudesAmistad() != null ? usuario.getNotifEmailSolicitudesAmistad() : true);
        preferences.put("matchUpdates", usuario.getNotifEmailActualizacionesPartido() != null ? usuario.getNotifEmailActualizacionesPartido() : true);
        preferences.put("reviewRequests", usuario.getNotifEmailSolicitudesReview() != null ? usuario.getNotifEmailSolicitudesReview() : true);
        preferences.put("newMessages", usuario.getNotifEmailNuevosMensajes() != null ? usuario.getNotifEmailNuevosMensajes() : false);
        preferences.put("generalUpdates", usuario.getNotifEmailGenerales() != null ? usuario.getNotifEmailGenerales() : false);

        return preferences;
    }

    /**
     * Actualizar preferencias de notificación del usuario
     */
    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public Map<String, Object> updateNotificationPreferences(UUID usuarioId, Map<String, Boolean> preferences) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (preferences.containsKey("matchInvitations")) {
            usuario.setNotifEmailInvitaciones(preferences.get("matchInvitations"));
        }
        if (preferences.containsKey("friendRequests")) {
            usuario.setNotifEmailSolicitudesAmistad(preferences.get("friendRequests"));
        }
        if (preferences.containsKey("matchUpdates")) {
            usuario.setNotifEmailActualizacionesPartido(preferences.get("matchUpdates"));
        }
        if (preferences.containsKey("reviewRequests")) {
            usuario.setNotifEmailSolicitudesReview(preferences.get("reviewRequests"));
        }
        if (preferences.containsKey("newMessages")) {
            usuario.setNotifEmailNuevosMensajes(preferences.get("newMessages"));
        }
        if (preferences.containsKey("generalUpdates")) {
            usuario.setNotifEmailGenerales(preferences.get("generalUpdates"));
        }

        usuarioRepository.save(usuario);

        return getNotificationPreferences(usuarioId);
    }
}