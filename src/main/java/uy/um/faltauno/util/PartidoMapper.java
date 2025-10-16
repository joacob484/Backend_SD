package uy.um.faltauno.util;

import org.mapstruct.*;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

import java.util.List;
import java.util.UUID;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PartidoMapper {

    // =========================
    // Entity -> DTO
    // =========================
    @Mapping(source = "organizador.id",     target = "organizadorId")
    @Mapping(source = "organizador.nombre", target = "organizadorNombre")
    PartidoDTO toDto(Partido partido);

    List<PartidoDTO> toDtoList(List<Partido> partidos);

    // =========================
    // DTO -> Entity
    // =========================
    // Creamos/inyectamos el Usuario organizador a partir del DTO completo
    @Mapping(target = "organizador", source = "dto", qualifiedByName = "dtoToUsuario")
    Partido toEntity(PartidoDTO dto);

    List<Partido> toEntityList(List<PartidoDTO> dtos);

    // =========================
    // UPDATE parcial (PATCH)
    // =========================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PartidoDTO dto, @MappingTarget Partido entity);

    // Después del update parcial, acomodamos el organizador en base a los campos del DTO (si vinieron)
    @AfterMapping
    default void afterUpdate(@MappingTarget Partido entity, PartidoDTO dto) {
        if (dto == null) return;

        // Si se envía organizadorId, seteamos/creamos el Usuario;
        // si explícitamente viene null, lo dejamos como está (regla de "ignore nulls").
        if (dto.getOrganizadorId() != null) {
            if (entity.getOrganizador() == null) {
                entity.setOrganizador(new Usuario());
            }
            entity.getOrganizador().setId(dto.getOrganizadorId());
            // Si viene nombre, lo seteamos; si no viene, no tocamos el existente.
            if (dto.getOrganizadorNombre() != null) {
                entity.getOrganizador().setNombre(dto.getOrganizadorNombre());
            }
        }
    }

    // =========================
    // Helpers
    // =========================
    @Named("dtoToUsuario")
    default Usuario dtoToUsuario(PartidoDTO dto) {
        if (dto == null) return null;
        UUID id = dto.getOrganizadorId();
        String nombre = dto.getOrganizadorNombre();
        if (id == null) return null;

        Usuario u = new Usuario();
        u.setId(id);
        if (nombre != null) {
            u.setNombre(nombre);
        }
        return u;
    }
}
