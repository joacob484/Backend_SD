package uy.um.faltauno.util;

import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.dto.InscripcionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuario.id", target = "usuarioId")
    InscripcionDTO toDTO(Inscripcion inscripcion);

    @Mapping(source = "partidoId", target = "partido.id")
    @Mapping(source = "usuarioId", target = "usuario.id")
    Inscripcion toEntity(InscripcionDTO dto);
}