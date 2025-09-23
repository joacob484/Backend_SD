package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import uy.um.faltauno.dto.PartidoDto;
import uy.um.faltauno.entity.Partido;

@Mapper
public interface PartidoMapper {
    PartidoMapper INSTANCE = Mappers.getMapper(PartidoMapper.class);

    @Mapping(source = "organizador.id", target = "organizadorId")
    PartidoDto toDto(Partido partido);

    @Mapping(source = "organizadorId", target = "organizador.id")
    Partido toEntity(PartidoDto dto);
}
