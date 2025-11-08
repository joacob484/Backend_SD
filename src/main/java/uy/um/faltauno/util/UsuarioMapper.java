package uy.um.faltauno.util;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"

    // ----------------------
    // ENTITY → DTO
    // ----------------------
    @Mapping(source = "fotoPerfil", target = "fotoPerfil", qualifiedByName = "bytesToBase64")
    @Mapping(source = "fechaNacimiento", target = "fechaNacimiento", qualifiedByName = "localDateToString")
    @Mapping(target = "cedulaVerificada", ignore = true) // Calculated in DTO getter
    @Mapping(target = "perfilCompleto", ignore = true) // Calculated in DTO getter
    @Mapping(target = "hasFotoPerfil", ignore = true) // Set in @AfterMapping
    UsuarioDTO toDTO(Usuario usuario);

    /**
     * ⚡ CRÍTICO: Post-procesamiento para setear campos calculados explícitamente
     * 
     * Esto previene inconsistencias donde el backend envía perfilCompleto=null
     * y el frontend lo interpreta como false, causando loops de "completar perfil".
     * 
     * Al forzar el cálculo aquí, garantizamos que SIEMPRE se envíe un valor booleano real.
     */
    @AfterMapping
    default void setCalculatedFields(@MappingTarget UsuarioDTO dto, Usuario usuario) {
        // ✅ Forzar cálculo de campos calculados para que NUNCA sean null
        dto.setPerfilCompleto(dto.getPerfilCompleto());
        dto.setCedulaVerificada(dto.getCedulaVerificada());

        // ✅ Forzar presence flag de foto para evitar que el frontend tenga que inferir
        // Se expone hasFotoPerfil = usuario.fotoPerfil != null
        dto.setHasFotoPerfil(usuario != null && usuario.getFotoPerfil() != null && usuario.getFotoPerfil().length > 0);
    }

    // ----------------------
    // DTO → ENTITY
    // ----------------------
    @Mapping(source = "fotoPerfil", target = "fotoPerfil", qualifiedByName = "base64ToBytes")
    @Mapping(source = "fechaNacimiento", target = "fechaNacimiento", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "createdAt", ignore = true) // Gestionado por @CreationTimestamp
    @Mapping(target = "deletedAt", ignore = true) // Gestionado por el servicio
    @Mapping(target = "lastActivityAt", ignore = true) // Gestionado automáticamente
    @Mapping(target = "tokenVersion", ignore = true) // Gestionado por el servicio
    @Mapping(target = "verificationCodeExpiresAt", ignore = true) // Gestionado por el servicio
    @Mapping(target = "provider", ignore = true) // No viene del DTO, se gestiona en el servicio
    @Mapping(target = "notifEmailGenerales", ignore = true) // Valores por defecto en la entidad
    @Mapping(target = "notifEmailSolicitudesAmistad", ignore = true)
    @Mapping(target = "notifEmailInvitaciones", ignore = true)
    @Mapping(target = "notifEmailActualizacionesPartido", ignore = true)
    @Mapping(target = "notifEmailNuevosMensajes", ignore = true)
    @Mapping(target = "notifEmailSolicitudesReview", ignore = true)
    Usuario toEntity(UsuarioDTO dto);

    // Conversores para LocalDate <-> String
    @Named("localDateToString")
    default String mapLocalDateToString(LocalDate date) {
        return date != null ? date.format(FORMATTER) : null;
    }

    @Named("stringToLocalDate")
    default LocalDate mapStringToLocalDate(String date) {
        return (date != null && !date.isBlank()) ? LocalDate.parse(date, FORMATTER) : null;
    }

    // Conversores para byte[] <-> Base64
    @Named("bytesToBase64")
    default String mapBytesToBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            System.out.println("[UsuarioMapper] 🔍 bytesToBase64: bytes es null o vacío");
            return null;
        }
        
        String base64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("[UsuarioMapper] 🔍 bytesToBase64: Conversión exitosa. " + 
            bytes.length + " bytes → " + base64.length() + " chars");
        return base64;
    }

    @Named("base64ToBytes")
    default byte[] mapBase64ToBytes(String base64) {
        try {
            if (base64 == null || base64.isBlank()) {
                System.out.println("[UsuarioMapper] 🔍 base64ToBytes: base64 es null o vacío");
                return null;
            }
            
            byte[] bytes = Base64.getDecoder().decode(base64);
            System.out.println("[UsuarioMapper] 🔍 base64ToBytes: Conversión exitosa. " + 
                base64.length() + " chars → " + bytes.length + " bytes");
            return bytes;
        } catch (IllegalArgumentException e) {
            // ⚠️ Si el string no es Base64 válido, devuelve null para evitar 400
            System.out.println("[UsuarioMapper] ⚠️ base64ToBytes: ERROR - String no es Base64 válido: " + e.getMessage());
            return null;
        }
    }
}