package uy.um.faltauno.util;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.Usuario;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-16T11:20:56-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewDTO toDTO(Review review) {
        if ( review == null ) {
            return null;
        }

        ReviewDTO.ReviewDTOBuilder reviewDTO = ReviewDTO.builder();

        reviewDTO.partidoId( reviewPartidoId( review ) );
        reviewDTO.usuarioQueCalificaId( reviewUsuarioQueCalificaId( review ) );
        reviewDTO.usuarioCalificadoId( reviewUsuarioCalificadoId( review ) );
        reviewDTO.comentario( review.getComentario() );
        reviewDTO.companerismo( review.getCompanerismo() );
        reviewDTO.deportividad( review.getDeportividad() );
        reviewDTO.id( review.getId() );
        reviewDTO.nivel( review.getNivel() );

        return reviewDTO.build();
    }

    @Override
    public Review toEntity(ReviewDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Review.ReviewBuilder review = Review.builder();

        review.comentario( dto.getComentario() );
        review.companerismo( dto.getCompanerismo() );
        review.deportividad( dto.getDeportividad() );
        review.id( dto.getId() );
        review.nivel( dto.getNivel() );

        return review.build();
    }

    private UUID reviewPartidoId(Review review) {
        if ( review == null ) {
            return null;
        }
        Partido partido = review.getPartido();
        if ( partido == null ) {
            return null;
        }
        UUID id = partido.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID reviewUsuarioQueCalificaId(Review review) {
        if ( review == null ) {
            return null;
        }
        Usuario usuarioQueCalifica = review.getUsuarioQueCalifica();
        if ( usuarioQueCalifica == null ) {
            return null;
        }
        UUID id = usuarioQueCalifica.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID reviewUsuarioCalificadoId(Review review) {
        if ( review == null ) {
            return null;
        }
        Usuario usuarioCalificado = review.getUsuarioCalificado();
        if ( usuarioCalificado == null ) {
            return null;
        }
        UUID id = usuarioCalificado.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
