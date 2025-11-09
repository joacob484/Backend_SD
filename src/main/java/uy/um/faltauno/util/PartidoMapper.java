package uy.um.faltauno.util;

import org.mapstruct.*;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PartidoMapper {

    // Entity -> DTO
    @Mapping(target = "id", source = "id") // ⬅️ fuerza el mapeo del ID
    @Mapping(source = "organizador.id", target = "organizadorId")
    @Mapping(source = "organizador", target = "organizador", qualifiedByName = "usuarioToMin")
    @Mapping(target = "precioPorJugador", expression = "java(calcularPrecioPorJugador(partido))")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(target = "jugadoresActuales", ignore = true)
    @Mapping(target = "jugadores", ignore = true)
    @Mapping(target = "solicitudesPendientes", ignore = true)
    PartidoDTO toDto(Partido partido);

    List<PartidoDTO> toDtoList(List<Partido> partidos);

    // DTO -> Entity
    @Mapping(target = "id", ignore = true) // ⬅️ no sobreescribas el PK al actualizar
    @Mapping(target = "organizador", source = "organizadorId", qualifiedByName = "idToUsuario")
    @Mapping(target = "createdAt", ignore = true)
    Partido toEntity(PartidoDTO dto);

    List<Partido> toEntityList(List<PartidoDTO> dtos);

    // UPDATE parcial
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizador", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(PartidoDTO dto, @MappingTarget Partido entity);

    // Métodos auxiliares
    @Named("usuarioToMin")
    default UsuarioMinDTO usuarioToMin(Usuario usuario) {
        if (usuario == null) return null;
        String fotoPerfil = null;
        if (usuario.getFotoPerfil() != null) {
            try {
                fotoPerfil = java.util.Base64.getEncoder().encodeToString(usuario.getFotoPerfil());
            } catch (Exception e) {
                // Log silently, return null
            }
        }
        return new UsuarioMinDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            fotoPerfil,
            usuario.getDeletedAt() // Incluir deletedAt para que el frontend sepa si está eliminado
        );
    }
    
    @Named("idToUsuario")
    default Usuario idToUsuario(UUID id) {
        if (id == null) return null;
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
    
    default BigDecimal calcularPrecioPorJugador(Partido partido) {
        if (partido == null || partido.getPrecioTotal() == null || 
            partido.getCantidadJugadores() == null || partido.getCantidadJugadores() == 0) {
            return BigDecimal.ZERO;
        }
        
        return partido.getPrecioTotal().divide(
            BigDecimal.valueOf(partido.getCantidadJugadores()),
            2,
            java.math.RoundingMode.HALF_UP
        );
    }
}