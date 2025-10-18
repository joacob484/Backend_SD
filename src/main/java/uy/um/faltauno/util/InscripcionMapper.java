package uy.um.faltauno.util;

import org.mapstruct.*;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Usuario;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {
    
    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    
    /**
     * Convertir Entity a DTO
     */
    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuario.id", target = "usuarioId")
    @Mapping(source = "usuario", target = "usuario", qualifiedByName = "toUsuarioMinDTO")
    @Mapping(source = "createdAt", target = "fechaSolicitud", qualifiedByName = "instantToString")
    InscripcionDTO toDTO(Inscripcion inscripcion);

    /**
     * Convertir DTO a Entity (sin relaciones)
     */
    @Mapping(target = "partido", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Inscripcion toEntity(InscripcionDTO dto);
    
    /**
     * Convertir Usuario a UsuarioMinDTO
     */
    @Named("toUsuarioMinDTO")
    default UsuarioMinDTO toUsuarioMinDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioMinDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getFotoPerfil()
        );
    }
    
    /**
     * Convertir Instant a String
     */
    @Named("instantToString")
    default String instantToString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return FORMATTER.format(instant);
    }
}