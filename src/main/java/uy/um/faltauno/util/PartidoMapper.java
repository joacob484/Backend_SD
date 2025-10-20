package uy.um.faltauno.util;

import org.mapstruct.*;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PartidoMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // =========================
    // Entity -> DTO
    // =========================
    @Mapping(source = "organizador.id", target = "organizadorId")
    @Mapping(source = "organizador", target = "organizador", qualifiedByName = "usuarioToMin")
    @Mapping(target = "precioPorJugador", expression = "java(calcularPrecioPorJugador(partido))")
    @Mapping(target = "jugadoresActuales", ignore = true) // Se setea manualmente en el servicio
    @Mapping(target = "jugadores", ignore = true) // Se setea manualmente en el servicio
    @Mapping(target = "solicitudesPendientes", ignore = true) // Se setea manualmente en el servicio
    PartidoDTO toDto(Partido partido);

    List<PartidoDTO> toDtoList(List<Partido> partidos);

    // =========================
    // DTO -> Entity
    // =========================
    @Mapping(target = "organizador", source = "organizadorId", qualifiedByName = "idToUsuario")
    Partido toEntity(PartidoDTO dto);

    List<Partido> toEntityList(List<PartidoDTO> dtos);

    // =========================
    // UPDATE parcial (PATCH)
    // =========================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizador", ignore = true) // No permitir cambiar organizador
    void updateEntityFromDto(PartidoDTO dto, @MappingTarget Partido entity);

    // =========================
    // Métodos auxiliares
    // =========================
    
    /**
     * Convierte Usuario a UsuarioMinDTO
     */
    @Named("usuarioToMin")
    default UsuarioMinDTO usuarioToMin(Usuario usuario) {
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
     * Convierte UUID a Usuario (solo con ID)
     */
    @Named("idToUsuario")
    default Usuario idToUsuario(UUID id) {
        if (id == null) {
            return null;
        }
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
    
    /**
     * Calcula el precio por jugador
     */
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