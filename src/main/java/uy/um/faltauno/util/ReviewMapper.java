package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.entity.Review;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuarioQueCalifica.id", target = "usuarioQueCalificaId")
    @Mapping(source = "usuarioCalificado.id", target = "usuarioCalificadoId")
    @Mapping(target = "promedio", ignore = true) // Se calcula en getPromedio()
    ReviewDTO toDTO(Review review);

    @Mapping(target = "partido", ignore = true)
    @Mapping(target = "usuarioQueCalifica", ignore = true)
    @Mapping(target = "usuarioCalificado", ignore = true)
    Review toEntity(ReviewDTO dto);
}