package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
    UsuarioDTO toDTO(Usuario usuario);

    // ----------------------
    // DTO → ENTITY
    // ----------------------
    @Mapping(source = "fotoPerfil", target = "fotoPerfil", qualifiedByName = "base64ToBytes")
    @Mapping(source = "fechaNacimiento", target = "fechaNacimiento", qualifiedByName = "stringToLocalDate")
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
        return (bytes != null && bytes.length > 0) ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    @Named("base64ToBytes")
    default byte[] mapBase64ToBytes(String base64) {
        try {
            return (base64 != null && !base64.isBlank()) ? Base64.getDecoder().decode(base64) : null;
        } catch (IllegalArgumentException e) {
            // ⚠️ Si el string no es Base64 válido, devuelve null para evitar 400
            return null;
        }
    }
}