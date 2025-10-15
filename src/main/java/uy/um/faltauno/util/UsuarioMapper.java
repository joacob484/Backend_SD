package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"

    // MapStruct mapea byte[] OK por defecto; mapeos explícitos incluidos por claridad
    @Mapping(source = "fotoPerfil", target = "fotoPerfil")
    @Mapping(source = "fechaNacimiento", target = "fechaNacimiento", qualifiedByName = "localDateToString")
    UsuarioDTO toDTO(Usuario usuario);

    @Mapping(source = "fotoPerfil", target = "fotoPerfil")
    @Mapping(source = "fechaNacimiento", target = "fechaNacimiento", qualifiedByName = "stringToLocalDate")
    Usuario toEntity(UsuarioDTO dto);

    // Conversores para LocalDate <-> String
    @Named("localDateToString")
    default String mapLocalDateToString(LocalDate date) {
        return date != null ? date.format(FORMATTER) : null;
    }

    @Named("stringToLocalDate")
    default LocalDate mapStringToLocalDate(String date) {
        return date != null && !date.isBlank() ? LocalDate.parse(date, FORMATTER) : null;
    }
}