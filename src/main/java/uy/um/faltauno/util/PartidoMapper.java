package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.dto.PartidoDTO;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PartidoMapper {

    // Entity -> DTO
    @Mapping(source = "organizador.id", target = "organizadorId")
    @Mapping(source = "organizador.nombre", target = "organizadorNombre")
    PartidoDTO toDto(Partido partido);

    List<PartidoDTO> toDtoList(List<Partido> partidos);

    // DTO -> Entity
    @Mapping(source = "organizadorId", target = "organizador.id")
    @Mapping(source = "organizadorNombre", target = "organizador.nombre")
    Partido toEntity(PartidoDTO dto);

    List<Partido> toEntityList(List<PartidoDTO> dtos);
}