package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uy.um.faltauno.dto.ReviewDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Review;
import uy.um.faltauno.entity.Usuario;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuarioQueCalifica.id", target = "usuarioQueCalificaId")
    @Mapping(source = "usuarioCalificado.id", target = "usuarioCalificadoId")
    @Mapping(source = "usuarioCalificado", target = "usuarioCalificado", qualifiedByName = "usuarioToMin")
    @Mapping(target = "promedio", ignore = true) // Se calcula en getPromedio()
    ReviewDTO toDTO(Review review);

    @Mapping(target = "partido", ignore = true)
    @Mapping(target = "usuarioQueCalifica", ignore = true)
    @Mapping(target = "usuarioCalificado", ignore = true)
    Review toEntity(ReviewDTO dto);
    
    @Named("usuarioToMin")
    default UsuarioMinDTO usuarioToMin(Usuario usuario) {
        if (usuario == null) return null;
        String fotoPerfil = null;
        if (usuario.getFotoPerfil() != null) {
            try {
                fotoPerfil = java.util.Base64.getEncoder().encodeToString(usuario.getFotoPerfil());
            } catch (Exception e) {
                // Silently handle encoding errors
            }
        }
        return new UsuarioMinDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            fotoPerfil
        );
    }
}