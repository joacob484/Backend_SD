package uy.um.faltauno.util;

import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-16T10:04:02-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class InscripcionMapperImpl implements InscripcionMapper {

    @Override
    public InscripcionDTO toDTO(Inscripcion inscripcion) {
        if ( inscripcion == null ) {
            return null;
        }

        InscripcionDTO.InscripcionDTOBuilder inscripcionDTO = InscripcionDTO.builder();

        inscripcionDTO.partidoId( inscripcionPartidoId( inscripcion ) );
        inscripcionDTO.usuarioId( inscripcionUsuarioId( inscripcion ) );
        inscripcionDTO.estado( inscripcion.getEstado() );
        inscripcionDTO.id( inscripcion.getId() );

        return inscripcionDTO.build();
    }

    @Override
    public Inscripcion toEntity(InscripcionDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Inscripcion.InscripcionBuilder inscripcion = Inscripcion.builder();

        inscripcion.estado( dto.getEstado() );
        inscripcion.id( dto.getId() );

        return inscripcion.build();
    }

    private UUID inscripcionPartidoId(Inscripcion inscripcion) {
        if ( inscripcion == null ) {
            return null;
        }
        Partido partido = inscripcion.getPartido();
        if ( partido == null ) {
            return null;
        }
        UUID id = partido.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID inscripcionUsuarioId(Inscripcion inscripcion) {
        if ( inscripcion == null ) {
            return null;
        }
        Usuario usuario = inscripcion.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        UUID id = usuario.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
