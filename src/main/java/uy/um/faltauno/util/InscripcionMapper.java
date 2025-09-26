package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.entity.Inscripcion;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {
    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuario.id", target = "usuarioId")
    InscripcionDTO toDTO(Inscripcion inscripcion);

    @Mapping(target = "partido", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Inscripcion toEntity(InscripcionDTO dto);
}