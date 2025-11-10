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
import uy.um.faltauno.entity.ChatVisit;
import uy.um.faltauno.entity.Contacto;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Mensaje;
import uy.um.faltauno.entity.Notificacion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.PasswordResetToken;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.SolicitudPartido;
import uy.um.faltauno.entity.Usuario;
import uy.um.faltauno.repository.AmistadRepository;
import uy.um.faltauno.repository.ChatVisitRepository;
import uy.um.faltauno.repository.ContactoRepository;
import uy.um.faltauno.repository.InscripcionRepository;
import uy.um.faltauno.repository.MensajeRepository;
import uy.um.faltauno.repository.NotificacionRepository;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.repository.PasswordResetTokenRepository;
import uy.um.faltauno.repository.ReviewRepository;
import uy.um.faltauno.repository.SolicitudPartidoRepository;
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
    private final ContactoRepository contactoRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SolicitudPartidoRepository solicitudPartidoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ChatVisitRepository chatVisitRepository;

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
        if (usuarioId == null) throw new IllegalArgumentException("Usuario no encontrado");
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

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

        // Mapear DTO a entidad (incluye nombre, apellido, celular, fechaNacimiento, etc.)
        Usuario usuario = usuarioMapper.toEntity(dto);
        
        // 🔍 DEBUG: Verificar foto
        log.info("[UsuarioService] 🔍 createUsuario - Foto recibida en DTO: {}", 
            dto.getFotoPerfil() != null ? "SÍ (" + dto.getFotoPerfil().length() + " chars)" : "NO");
        log.info("[UsuarioService] 🔍 createUsuario - Foto en entidad después de mapper: {}", 
            usuario.getFotoPerfil() != null ? "SÍ (" + usuario.getFotoPerfil().length + " bytes)" : "NO");
        
        // 🔍 DEBUG: Log para verificar qué path se toma
        log.info("[UsuarioService] 🔍 createUsuario - emailVerified: {} | password presente: {}", 
            dto.getEmailVerified(), 
            dto.getPassword() != null);
        
        // Si ya está verificado, el password ya viene hasheado del pre-registro
        // Si no está verificado (OAuth), no hay password
        if (dto.getEmailVerified() != null && dto.getEmailVerified()) {
            // Password ya viene hasheado de VerificationService - NO encriptar de nuevo
            log.info("[UsuarioService] 🔍 Usando password hash del pre-registro (NO re-encriptando)");
            usuario.setPassword(dto.getPassword());
        } else {
            // Para registros directos (no deberían existir, pero por si acaso)
            log.info("[UsuarioService] 🔍 Encriptando password (registro directo u OAuth)");
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        // 🔍 DEBUG: Log del password final guardado
        log.info("[UsuarioService] 🔍 Password guardado en BD (primeros 20 chars): {}", 
            usuario.getPassword() != null ? usuario.getPassword().substring(0, Math.min(20, usuario.getPassword().length())) : "null");
        
        // Asegurar campos requeridos
        if (usuario.getProvider() == null) {
            usuario.setProvider("LOCAL");
        }
        if (usuario.getCreatedAt() == null) {
            usuario.setCreatedAt(LocalDateTime.now());
        }

        usuario = usuarioRepository.save(usuario);
        
        // 🔍 DEBUG: Verificar que la foto se guardó en BD
        log.info("[UsuarioService] 🔍 createUsuario - Usuario guardado. Foto en BD: {}", 
            usuario.getFotoPerfil() != null ? "SÍ (" + usuario.getFotoPerfil().length + " bytes)" : "NO");

        // Enviar email de bienvenida de forma asíncrona
        try {
            emailService.enviarEmailBienvenida(usuario);
        } catch (Exception e) {
            // No fallar el registro si el email falla
            // El log se maneja en EmailService
        }

        UsuarioDTO out = usuarioMapper.toDTO(usuario);
        
        // 🔍 DEBUG: Verificar que hasFotoPerfil se calcula correctamente
        log.info("[UsuarioService] 🔍 createUsuario - DTO retornado. hasFotoPerfil: {} | fotoPerfil en DTO: {}", 
            out.getHasFotoPerfil(),
            out.getFotoPerfil() != null ? "SÍ (" + out.getFotoPerfil().length() + " chars)" : "NO");
        
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
                throw new IllegalArgumentException("Usuario eliminado");
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // ⚡ CRITICAL FIX: Force eager loading of foto WITHIN transaction
            // fotoPerfil is LAZY, so we must access it before session closes
            byte[] fotoPerfilBytes = usuario.getFotoPerfil(); // Trigger lazy load
            
            UsuarioDTO dto = usuarioMapper.toDTO(usuario);
            
            // ⚡ CRITICAL: Asegurar que los campos calculados están correctos
            dto.setPerfilCompleto(dto.getPerfilCompleto());
            dto.setCedulaVerificada(dto.getCedulaVerificada());
            
            // ⚡ CRITICAL FIX: Force hasFotoPerfil to be calculated correctly
            dto.setHasFotoPerfil(fotoPerfilBytes != null && fotoPerfilBytes.length > 0);
            
            return dto;
        }
        
        // Si no existe en absoluto
        throw new IllegalArgumentException("Usuario no encontrado");
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "usuarios", key = "#usuarioId"),
        @CacheEvict(value = "sugerencias", allEntries = true)
    })
    public Usuario actualizarPerfil(UUID usuarioId, PerfilDTO perfilDTO) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // ✅ Validaciones de campos
        if (perfilDTO.getNombre() != null && perfilDTO.getNombre().length() > 100) {
            throw new IllegalArgumentException("Nombre demasiado largo (máx 100 caracteres)");
        }
        if (perfilDTO.getApellido() != null && perfilDTO.getApellido().length() > 100) {
            throw new IllegalArgumentException("Apellido demasiado largo (máx 100 caracteres)");
        }

        // ✅ FIX: Limpiar nombre si contiene apellido duplicado (tanto del request como histórico)
        String nombre = perfilDTO.getNombre();
        String apellido = perfilDTO.getApellido();
        
        // 🔧 Limpieza de datos históricos: si el nombre ACTUAL del usuario contiene apellido duplicado, limpiarlo
        if (usuario.getNombre() != null && apellido != null && !apellido.isBlank()) {
            if (usuario.getNombre().trim().endsWith(" " + apellido.trim())) {
                String nombreLimpio = usuario.getNombre().substring(0, usuario.getNombre().lastIndexOf(" " + apellido.trim())).trim();
                log.info("[UsuarioService] 🧹 Limpieza automática de datos históricos - Usuario {}: nombre '{}' → '{}'", 
                    usuarioId, usuario.getNombre(), nombreLimpio);
                usuario.setNombre(nombreLimpio);
            }
        }
        
        // Validación de nuevo nombre del request
        if (nombre != null && apellido != null && !apellido.isBlank()) {
            // Si el nombre termina con el apellido, quitarlo
            if (nombre.trim().endsWith(" " + apellido.trim())) {
                nombre = nombre.substring(0, nombre.lastIndexOf(" " + apellido.trim())).trim();
                log.debug("[UsuarioService] Apellido duplicado removido del nombre: {} → {}", 
                    perfilDTO.getNombre(), nombre);
            }
        }
        
        // ⚡ CRÍTICO: Solo actualizar si vienen datos válidos (NO sobreescribir con vacíos)
        if (nombre != null && !nombre.trim().isEmpty()) {
            usuario.setNombre(nombre);
        }
        if (apellido != null && !apellido.trim().isEmpty()) {
            usuario.setApellido(apellido);
        }
        
        // ⚡ CRÍTICO: Solo actualizar celular si viene en el request (no sobrescribir con null)
        if (perfilDTO.getCelular() != null) {
            usuario.setCelular(perfilDTO.getCelular());
        }
        
        // ⚡ CRÍTICO: Solo actualizar posicion si viene en el request
        if (perfilDTO.getPosicion() != null && !perfilDTO.getPosicion().isBlank()) {
            usuario.setPosicion(perfilDTO.getPosicion());
        }
        
        // ✅ Validar altura (frontend envía en cm, backend almacena en cm)
        if (perfilDTO.getAltura() != null && !perfilDTO.getAltura().isEmpty()) {
            try {
                Double alturaCm = Double.valueOf(perfilDTO.getAltura());
                // Validar rango razonable (100-250 cm)
                if (alturaCm < 100.0 || alturaCm > 250.0) {
                    throw new IllegalArgumentException("Altura debe estar entre 100cm y 250cm");
                }
                usuario.setAltura(alturaCm);
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

    Usuario saved = usuarioRepository.save(usuario);
    // Forzar flush para visibilidad inmediata en lecturas subsecuentes
    usuarioRepository.flush();
    return saved;
    }

    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public void subirFoto(UUID usuarioId, MultipartFile file) throws IOException {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

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
    // Forzar flush para asegurar visibilidad inmediata en lecturas posteriores
    usuarioRepository.flush();
    }

    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public Usuario marcarCedula(UUID usuarioId, String cedula) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
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

    /**
     * Buscar usuarios por números de teléfono (para sincronización de contactos)
     * Normaliza los números antes de buscar
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> buscarPorTelefonos(List<String> telefonos) {
        if (telefonos == null || telefonos.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Buscando usuarios para {} números", telefonos.size());

        // Normalizar números: quitar espacios, + inicial, etc. para buscar
        List<String> telefonosNormalizados = telefonos.stream()
                .map(tel -> tel.replaceAll("[\\s\\-\\(\\)\\+]", "")) // Quitar formato
                .filter(tel -> !tel.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        log.info("Números normalizados: {}", telefonosNormalizados.size());

        // Buscar en la base de datos
        // Hacemos búsqueda flexible: buscar tanto con + como sin +
        List<Usuario> usuarios = usuarioRepository.findAllActive().stream()
                .filter(u -> {
                    if (u.getCelular() == null || u.getCelular().isEmpty()) {
                        return false;
                    }
                    String celularNormalizado = u.getCelular().replaceAll("[\\s\\-\\(\\)\\+]", "");
                    
                    // Buscar coincidencia exacta o coincidencia de sufijo (últimos 8-10 dígitos)
                    return telefonosNormalizados.stream().anyMatch(tel -> {
                        // Coincidencia exacta
                        if (celularNormalizado.equals(tel)) {
                            return true;
                        }
                        // Coincidencia de sufijo (últimos 8-10 dígitos)
                        int minLength = Math.min(8, Math.min(celularNormalizado.length(), tel.length()));
                        if (celularNormalizado.length() >= minLength && tel.length() >= minLength) {
                            String sufijoCelular = celularNormalizado.substring(celularNormalizado.length() - minLength);
                            String sufijoTel = tel.substring(tel.length() - minLength);
                            return sufijoCelular.equals(sufijoTel);
                        }
                        return false;
                    });
                })
                .collect(Collectors.toList());

        log.info("Encontrados {} usuarios", usuarios.size());

        return usuarios.stream()
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
        // Con la nueva arquitectura, las invitaciones pendientes están en solicitud_partido
        // Si aún se necesita este método, debería consultar solicitudPartidoRepository
        // Por ahora retornar lista vacía
        return Collections.emptyList();
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
            m.put("inscripcionEstado", "ACEPTADO");  // Siempre ACEPTADO en la tabla inscripcion
            updates.add(m);
        }
        return updates;
    }

    @Transactional(readOnly = true)
    public Usuario findUsuarioEntityById(UUID id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    /**
     * Crear o actualizar usuario después de que se verifique el email (flow resiliente).
     * - Si el usuario NO existe: crea uno nuevo usando el passwordHash proporcionado (ya encriptado)
     * - Si el usuario ya existe: actualiza campos de perfil disponibles y marca emailVerified = true
     * Devuelve la entidad Usuario resultante (persistida)
     */
    @Transactional
    public Usuario createOrUpdateUserAfterVerification(String email, String passwordHash, UsuarioDTO profileDto) {
        Usuario existing = usuarioRepository.findByEmail(email).orElse(null);

        if (existing != null) {
            log.info("[UsuarioService] createOrUpdateUserAfterVerification - Usuario EXISTE: {} -> actualizando perfil y marcando emailVerified", email);
            // Actualizar campos si vienen en profileDto
            if (profileDto != null) {
                if (profileDto.getNombre() != null) existing.setNombre(profileDto.getNombre());
                if (profileDto.getApellido() != null) existing.setApellido(profileDto.getApellido());
                if (profileDto.getFechaNacimiento() != null) {
                    try {
                        existing.setFechaNacimiento(LocalDate.parse(profileDto.getFechaNacimiento(), UsuarioMapper.FORMATTER));
                    } catch (Exception e) {
                        log.warn("[UsuarioService] Fecha de nacimiento inválida al actualizar: {}", profileDto.getFechaNacimiento());
                    }
                }
                if (profileDto.getCelular() != null) existing.setCelular(profileDto.getCelular());
            }

            // Marcar email verificado
            existing.setEmailVerified(true);

            // Si no tenía password (OAuth), establecer el passwordHash del pre-registro
            if (existing.getPassword() == null && passwordHash != null) {
                existing.setPassword(passwordHash);
            }

            // Persistir cambios
            usuarioRepository.save(existing);
            // Forzar flush para garantizar visibilidad inmediata (evita race conditions)
            usuarioRepository.flush();
            return existing;
        }

        // No existía: crear nuevo usuario mínimo usando el DTO de perfil y el passwordHash
        log.info("[UsuarioService] createOrUpdateUserAfterVerification - Usuario NO existe: {} -> creando nuevo usuario", email);
        UsuarioDTO dto = new UsuarioDTO();
        dto.setEmail(email);
        dto.setPassword(passwordHash);
        dto.setEmailVerified(true);
        if (profileDto != null) {
            dto.setNombre(profileDto.getNombre());
            dto.setApellido(profileDto.getApellido());
            dto.setFechaNacimiento(profileDto.getFechaNacimiento());
            dto.setCelular(profileDto.getCelular());
        }

    UsuarioDTO created = createUsuario(dto);
    // Garantizar persistencia inmediata
    usuarioRepository.flush();
    return findUsuarioEntityById(created.getId());
    }

    /**
     * Busca usuario por email (carga TODA la entidad incluyendo LOB).
     * SOLO usar dentro de transacciones activas.
     */
    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    /**
     * Verifica si existe un usuario con el email dado (activo, no eliminado).
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByCelular(String celular) {
        return usuarioRepository.existsByCelular(celular);
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
        try {
            Optional<Usuario> deletedUser = usuarioRepository.findDeletedByEmail(email);
            if (deletedUser.isEmpty()) {
                return false;
            }
            
            // Validación defensiva: verificar que deletedAt no sea null
            LocalDateTime deletedAt = deletedUser.get().getDeletedAt();
            if (deletedAt == null) {
                log.warn("[UsuarioService] Usuario marcado como eliminado pero deletedAt es null: {}", email);
                return false;
            }
            
            // Verificar que no hayan pasado 30 días
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            return deletedAt.isAfter(cutoffDate);
        } catch (Exception e) {
            log.error("[UsuarioService] Error verificando usuario eliminado recuperable para {}", email, e);
            return false; // En caso de error, asumir que no es recuperable
        }
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

        // ✅ OAuth solo guarda el email
        // Nombre/apellido se completarán en profile-setup
        // Pero guardamos datos de Google como sugerencias pre-llenadas
        
        // Guardar nombre completo de Google temporalmente (para profile-setup)
        if (name != null && !name.isBlank() && (u.getNombre() == null || u.getNombre().isBlank())) {
            // Si usuario no tiene nombre aún, separar el de Google como sugerencia
            String[] parts = name.trim().split("\\s+", 2);
            u.setNombre(parts[0]); // Primera palabra como nombre
            if (parts.length > 1 && (u.getApellido() == null || u.getApellido().isBlank())) {
                u.setApellido(parts[1]); // Resto como apellido
            }
            log.debug("[OAuth] Sugerencia de nombre desde Google: {} {}", parts[0], parts.length > 1 ? parts[1] : "");
        }

        // Campos opcionales: no fallar si no existen en la entidad
        // (si tu entidad no tiene estos setters, simplemente quitá las líneas)
        Object sub = attrs != null ? attrs.get("sub") : null;
        if (sub != null) safeSet(u, "setProviderSub", sub.toString());

        Object picture = attrs != null ? attrs.get("picture") : null;
        if (picture != null) safeSet(u, "setFotoUrl", picture.toString());

        // Marcar email como verificado (usuarios OAuth vienen verificados por Google)
        u.setEmailVerified(true);

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
            throw new IllegalStateException("Error al persistir usuario OAuth");
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
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

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
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

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
    
    // ==================== MÉTODOS DE ADMINISTRADOR ====================
    
    /**
     * Listar todos los usuarios incluso eliminados (solo para admin)
     * Usa mapper ligero sin foto base64 para mejor performance
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodosInclusoEliminados() {
        log.info("[ADMIN] Listando todos los usuarios (incluso eliminados)");
        
        List<Usuario> usuarios = usuarioRepository.findAll();
        
        // ⚡ OPTIMIZACIÓN: Usar mapper sin foto para listados
        return usuarios.stream()
                .map(usuarioMapper::toDTOWithoutPhoto)
                .collect(Collectors.toList());
    }
    
    /**
     * Contar usuarios activos (no eliminados)
     */
    @Transactional(readOnly = true)
    public long contarUsuariosActivos() {
        return usuarioRepository.countActiveUsers();
    }
    
    /**
     * Contar usuarios con actividad reciente (últimos N días)
     */
    @Transactional(readOnly = true)
    public long contarUsuariosConActividadReciente(int dias) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(dias);
        return usuarioRepository.countByLastActivityAtAfter(cutoffDate);
    }
    
    /**
     * Contar registros recientes (últimos N días)
     */
    @Transactional(readOnly = true)
    public long contarRegistrosRecientes(int dias) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(dias);
        return usuarioRepository.countByCreatedAtAfter(cutoffDate);
    }
    
    /**
     * Contar usuarios eliminados (soft deleted)
     */
    @Transactional(readOnly = true)
    public long contarUsuariosEliminados() {
        return usuarioRepository.countByDeletedAtIsNotNull();
    }
    
    /**
     * Contar usuarios baneados
     */
    @Transactional(readOnly = true)
    public long contarUsuariosBaneados() {
        return usuarioRepository.countByBannedAtIsNotNull();
    }
    
    /**
     * Eliminar permanentemente un usuario (hard delete)
     * Elimina en cascada TODOS los datos relacionados:
     * - Contactos (propios y referencias)
     * - Password reset tokens
     * - Notificaciones  
     * - Solicitudes de partido
     * - Amistades
     * - Mensajes (via DB CASCADE)
     * - Reviews dadas y recibidas
     * - Inscripciones a partidos
     * - Partidos organizados (con sus inscripciones, reviews, solicitudes)
     */
    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public void eliminarPermanentemente(String usuarioId) {
        log.warn("[ADMIN] ⚠️ Iniciando eliminación PERMANENTE de usuario {} y TODOS sus datos", usuarioId);
        
        UUID uuid = UUID.fromString(usuarioId);
        // 🔥 IMPORTANTE: Buscar incluyendo soft-deleted para poder eliminar usuarios ya marcados como eliminados
        Usuario usuario = usuarioRepository.findByIdIncludingDeleted(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        log.info("[ADMIN] 📋 Usuario: {} {} ({}) - Soft deleted: {}", 
                usuario.getNombre(), usuario.getApellido(), usuario.getEmail(), 
                usuario.getDeletedAt() != null ? "SÍ (" + usuario.getDeletedAt() + ")" : "NO");
        
        // 0️⃣ CONTACTOS
        log.info("[ADMIN] 📇 Eliminando contactos...");
        List<Contacto> contactosPropios = contactoRepository.findByUsuarioId(uuid);
        log.info("[ADMIN]   → {} contactos propios", contactosPropios.size());
        contactoRepository.deleteAll(contactosPropios);
        
        // Limpiar referencias a este usuario en contactos de otros
        List<Contacto> contactosQueApuntanAEsteUsuario = contactoRepository.findAll().stream()
            .filter(c -> c.getUsuarioApp() != null && c.getUsuarioApp().getId().equals(uuid))
            .collect(Collectors.toList());
        log.info("[ADMIN]   → {} contactos de otros apuntan a este usuario", contactosQueApuntanAEsteUsuario.size());
        contactosQueApuntanAEsteUsuario.forEach(c -> {
            c.setUsuarioApp(null);
            c.setIsOnApp(false);
        });
        contactoRepository.saveAll(contactosQueApuntanAEsteUsuario);
        
        // 1️⃣ PASSWORD RESET TOKENS
        log.info("[ADMIN] 🔑 Eliminando tokens de recuperación...");
        passwordResetTokenRepository.invalidarTokensDelUsuario(usuario);
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll().stream()
            .filter(t -> t.getUsuario().getId().equals(uuid))
            .collect(Collectors.toList());
        log.info("[ADMIN]   → {} tokens encontrados", tokens.size());
        passwordResetTokenRepository.deleteAll(tokens);
        
        // 2️⃣ NOTIFICACIONES
        log.info("[ADMIN] 🔔 Eliminando notificaciones...");
        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(uuid);
        log.info("[ADMIN]   → {} notificaciones", notificaciones.size());
        notificacionRepository.deleteAll(notificaciones);
        
        // 2.5️⃣ CHAT VISITS
        log.info("[ADMIN] 👁️ Eliminando visitas de chat...");
        List<ChatVisit> chatVisits = chatVisitRepository.findByUsuarioId(uuid);
        log.info("[ADMIN]   → {} visitas de chat", chatVisits.size());
        chatVisitRepository.deleteAll(chatVisits);
        
        // 3️⃣ SOLICITUDES DE PARTIDO
        log.info("[ADMIN] 📋 Eliminando solicitudes de partido...");
        List<SolicitudPartido> solicitudes = solicitudPartidoRepository.findByUsuarioId(uuid);
        log.info("[ADMIN]   → {} solicitudes", solicitudes.size());
        solicitudPartidoRepository.deleteAll(solicitudes);
        
        // 4️⃣ AMISTADES
        log.info("[ADMIN] 👥 Eliminando amistades...");
        List<Amistad> amistades = amistadRepository.findAmigosByUsuarioId(uuid);
        List<Amistad> solicitudesEnviadas = amistadRepository.findByUsuarioIdAndEstado(uuid, "PENDIENTE");
        List<Amistad> solicitudesRecibidas = amistadRepository.findByAmigoIdAndEstado(uuid, "PENDIENTE");
        
        int totalAmistades = amistades.size() + solicitudesEnviadas.size() + solicitudesRecibidas.size();
        log.info("[ADMIN]   → {} amistades y solicitudes", totalAmistades);
        if (totalAmistades > 0) {
            amistadRepository.deleteAll(amistades);
            amistadRepository.deleteAll(solicitudesEnviadas);
            amistadRepository.deleteAll(solicitudesRecibidas);
        }
        
        // 5️⃣ MENSAJES - Confiar en CASCADE DELETE de PostgreSQL
        log.info("[ADMIN] 💬 Mensajes: confiando en CASCADE DELETE de base de datos");
        
        // 6️⃣ REVIEWS
        log.info("[ADMIN] ⭐ Eliminando reviews...");
        List<Review> reviewsHechas = reviewRepository.findByUsuarioQueCalifica_Id(uuid);
        List<Review> reviewsRecibidas = reviewRepository.findByUsuarioCalificado_Id(uuid);
        int totalReviews = reviewsHechas.size() + reviewsRecibidas.size();
        log.info("[ADMIN]   → {} reviews ({} hechas, {} recibidas)", 
                totalReviews, reviewsHechas.size(), reviewsRecibidas.size());
        if (totalReviews > 0) {
            reviewRepository.deleteAll(reviewsHechas);
            reviewRepository.deleteAll(reviewsRecibidas);
        }
        
        // 7️⃣ INSCRIPCIONES
        log.info("[ADMIN] 📝 Eliminando inscripciones...");
        List<Inscripcion> inscripciones = inscripcionRepository.findByUsuarioId(uuid);
        log.info("[ADMIN]   → {} inscripciones", inscripciones.size());
        if (!inscripciones.isEmpty()) {
            inscripcionRepository.deleteAll(inscripciones);
        }
        
        // 8️⃣ PARTIDOS ORGANIZADOS
        log.info("[ADMIN] ⚽ Eliminando partidos organizados...");
        List<Partido> partidosOrganizados = partidoRepository.findByOrganizador_Id(uuid);
        log.info("[ADMIN]   → {} partidos organizados", partidosOrganizados.size());
        if (!partidosOrganizados.isEmpty()) {
            for (Partido partido : partidosOrganizados) {
                // Eliminar solicitudes del partido
                List<SolicitudPartido> solicitudesPartido = solicitudPartidoRepository.findByPartidoId(partido.getId());
                if (!solicitudesPartido.isEmpty()) {
                    log.info("[ADMIN]     → Eliminando {} solicitudes del partido {}", 
                            solicitudesPartido.size(), partido.getId());
                    solicitudPartidoRepository.deleteAll(solicitudesPartido);
                }
                
                // Eliminar inscripciones del partido
                List<Inscripcion> inscripcionesPartido = inscripcionRepository.findByPartidoId(partido.getId());
                if (!inscripcionesPartido.isEmpty()) {
                    log.info("[ADMIN]     → Eliminando {} inscripciones del partido {}", 
                            inscripcionesPartido.size(), partido.getId());
                    inscripcionRepository.deleteAll(inscripcionesPartido);
                }
                
                // Eliminar visitas de chat del partido
                List<ChatVisit> chatVisitsPartido = chatVisitRepository.findByPartidoId(partido.getId());
                if (!chatVisitsPartido.isEmpty()) {
                    log.info("[ADMIN]     → Eliminando {} visitas de chat del partido {}", 
                            chatVisitsPartido.size(), partido.getId());
                    chatVisitRepository.deleteAll(chatVisitsPartido);
                }
                
                // Eliminar reviews del partido
                List<Review> reviewsPartido = reviewRepository.findByPartido_Id(partido.getId());
                if (!reviewsPartido.isEmpty()) {
                    log.info("[ADMIN]     → Eliminando {} reviews del partido {}", 
                            reviewsPartido.size(), partido.getId());
                    reviewRepository.deleteAll(reviewsPartido);
                }
            }
            // Eliminar los partidos
            partidoRepository.deleteAll(partidosOrganizados);
        }
        
        // 9️⃣ USUARIO
        log.warn("[ADMIN] 🗑️ Eliminando usuario {} DEFINITIVAMENTE", usuarioId);
        usuarioRepository.delete(usuario);
        
        log.warn("[ADMIN] ✅ Usuario {} y TODOS sus datos eliminados permanentemente", usuarioId);
    }
    
    /**
     * Cambiar rol de un usuario
     */
    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public UsuarioDTO cambiarRol(String usuarioId, String nuevoRol) {
        log.warn("[ADMIN] Cambiando rol de usuario {} a {}", usuarioId, nuevoRol);
        
        UUID uuid = UUID.fromString(usuarioId);
        Usuario usuario = usuarioRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
        
        return usuarioMapper.toDTO(usuario);
    }
    
    /**
     * Banear un usuario
     */
    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public UsuarioDTO banUser(String usuarioId, String adminId, String reason) {
        log.warn("[ADMIN] Usuario {} baneando a usuario {}", adminId, usuarioId);
        
        UUID uuid = UUID.fromString(usuarioId);
        UUID adminUuid = UUID.fromString(adminId);
        
        Usuario usuario = usuarioRepository.findByIdIncludingDeleted(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (usuario.getBannedAt() != null) {
            log.warn("[ADMIN] Usuario {} ya está baneado", usuarioId);
            throw new IllegalStateException("El usuario ya está baneado");
        }
        
        usuario.setBannedAt(LocalDateTime.now());
        usuario.setBanReason(reason);
        usuario.setBannedBy(adminUuid);
        
        // Incrementar token version para invalidar sesiones activas
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        
        usuarioRepository.save(usuario);
        
        log.warn("[ADMIN] Usuario {} baneado exitosamente. Razón: {}", usuarioId, reason);
        
        return usuarioMapper.toDTO(usuario);
    }
    
    /**
     * Desbanear un usuario
     */
    @Transactional
    @CacheEvict(value = "usuarios", key = "#usuarioId")
    public UsuarioDTO unbanUser(String usuarioId, String adminId) {
        log.warn("[ADMIN] Usuario {} desbaneando a usuario {}", adminId, usuarioId);
        
        UUID uuid = UUID.fromString(usuarioId);
        
        Usuario usuario = usuarioRepository.findByIdIncludingDeleted(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (usuario.getBannedAt() == null) {
            log.warn("[ADMIN] Usuario {} no está baneado", usuarioId);
            throw new IllegalStateException("El usuario no está baneado");
        }
        
        usuario.setBannedAt(null);
        usuario.setBanReason(null);
        usuario.setBannedBy(null);
        
        usuarioRepository.save(usuario);
        
        log.warn("[ADMIN] Usuario {} desbaneado exitosamente", usuarioId);
        
        return usuarioMapper.toDTO(usuario);
    }
    
    /**
     * Verificar si un usuario está baneado
     */
    @Transactional(readOnly = true)
    public boolean isUserBanned(String usuarioId) {
        UUID uuid = UUID.fromString(usuarioId);
        return usuarioRepository.findByIdIncludingDeleted(uuid)
                .map(u -> u.getBannedAt() != null)
                .orElse(false);
    }
}
