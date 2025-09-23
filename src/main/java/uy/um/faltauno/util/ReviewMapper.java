package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.entity.Review;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuarioQueCalifica.id", target = "usuarioQueCalificaId")
    @Mapping(source = "usuarioCalificado.id", target = "usuarioCalificadoId")
    ReviewDTO toDTO(Review review);

    @Mapping(source = "partidoId", target = "partido.id")
    @Mapping(source = "usuarioQueCalificaId", target = "usuarioQueCalifica.id")
    @Mapping(source = "usuarioCalificadoId", target = "usuarioCalificado.id")
    Review toEntity(ReviewDTO dto);
}
