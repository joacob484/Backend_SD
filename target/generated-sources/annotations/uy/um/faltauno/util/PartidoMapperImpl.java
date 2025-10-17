package uy.um.faltauno.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.PartidoDTO;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-17T10:49:36-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class PartidoMapperImpl implements PartidoMapper {

    @Override
    public PartidoDTO toDto(Partido partido) {
        if ( partido == null ) {
            return null;
        }

        PartidoDTO partidoDTO = new PartidoDTO();

        partidoDTO.setOrganizadorId( partidoOrganizadorId( partido ) );
        partidoDTO.setOrganizadorNombre( partidoOrganizadorNombre( partido ) );
        partidoDTO.setId( partido.getId() );
        partidoDTO.setTipoPartido( partido.getTipoPartido() );
        partidoDTO.setGenero( partido.getGenero() );
        partidoDTO.setFecha( partido.getFecha() );
        partidoDTO.setHora( partido.getHora() );
        partidoDTO.setDuracionMinutos( partido.getDuracionMinutos() );
        partidoDTO.setNombreUbicacion( partido.getNombreUbicacion() );
        partidoDTO.setDireccionUbicacion( partido.getDireccionUbicacion() );
        partidoDTO.setLatitud( partido.getLatitud() );
        partidoDTO.setLongitud( partido.getLongitud() );
        partidoDTO.setCantidadJugadores( partido.getCantidadJugadores() );
        partidoDTO.setPrecioTotal( partido.getPrecioTotal() );
        partidoDTO.setDescripcion( partido.getDescripcion() );

        return partidoDTO;
    }

    @Override
    public List<PartidoDTO> toDtoList(List<Partido> partidos) {
        if ( partidos == null ) {
            return null;
        }

        List<PartidoDTO> list = new ArrayList<PartidoDTO>( partidos.size() );
        for ( Partido partido : partidos ) {
            list.add( toDto( partido ) );
        }

        return list;
    }

    @Override
    public Partido toEntity(PartidoDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Partido partido = new Partido();

        partido.setOrganizador( dtoToUsuario( dto ) );
        partido.setId( dto.getId() );
        partido.setTipoPartido( dto.getTipoPartido() );
        partido.setGenero( dto.getGenero() );
        partido.setFecha( dto.getFecha() );
        partido.setHora( dto.getHora() );
        partido.setDuracionMinutos( dto.getDuracionMinutos() );
        partido.setNombreUbicacion( dto.getNombreUbicacion() );
        partido.setDireccionUbicacion( dto.getDireccionUbicacion() );
        partido.setLatitud( dto.getLatitud() );
        partido.setLongitud( dto.getLongitud() );
        partido.setCantidadJugadores( dto.getCantidadJugadores() );
        partido.setPrecioTotal( dto.getPrecioTotal() );
        partido.setDescripcion( dto.getDescripcion() );

        afterUpdate( partido, dto );

        return partido;
    }

    @Override
    public List<Partido> toEntityList(List<PartidoDTO> dtos) {
        if ( dtos == null ) {
            return null;
        }

        List<Partido> list = new ArrayList<Partido>( dtos.size() );
        for ( PartidoDTO partidoDTO : dtos ) {
            list.add( toEntity( partidoDTO ) );
        }

        return list;
    }

    @Override
    public void updateEntityFromDto(PartidoDTO dto, Partido entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getId() != null ) {
            entity.setId( dto.getId() );
        }
        if ( dto.getTipoPartido() != null ) {
            entity.setTipoPartido( dto.getTipoPartido() );
        }
        if ( dto.getGenero() != null ) {
            entity.setGenero( dto.getGenero() );
        }
        if ( dto.getFecha() != null ) {
            entity.setFecha( dto.getFecha() );
        }
        if ( dto.getHora() != null ) {
            entity.setHora( dto.getHora() );
        }
        if ( dto.getDuracionMinutos() != null ) {
            entity.setDuracionMinutos( dto.getDuracionMinutos() );
        }
        if ( dto.getNombreUbicacion() != null ) {
            entity.setNombreUbicacion( dto.getNombreUbicacion() );
        }
        if ( dto.getDireccionUbicacion() != null ) {
            entity.setDireccionUbicacion( dto.getDireccionUbicacion() );
        }
        if ( dto.getLatitud() != null ) {
            entity.setLatitud( dto.getLatitud() );
        }
        if ( dto.getLongitud() != null ) {
            entity.setLongitud( dto.getLongitud() );
        }
        if ( dto.getCantidadJugadores() != null ) {
            entity.setCantidadJugadores( dto.getCantidadJugadores() );
        }
        if ( dto.getPrecioTotal() != null ) {
            entity.setPrecioTotal( dto.getPrecioTotal() );
        }
        if ( dto.getDescripcion() != null ) {
            entity.setDescripcion( dto.getDescripcion() );
        }

        afterUpdate( entity, dto );
    }

    private UUID partidoOrganizadorId(Partido partido) {
        if ( partido == null ) {
            return null;
        }
        Usuario organizador = partido.getOrganizador();
        if ( organizador == null ) {
            return null;
        }
        UUID id = organizador.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String partidoOrganizadorNombre(Partido partido) {
        if ( partido == null ) {
            return null;
        }
        Usuario organizador = partido.getOrganizador();
        if ( organizador == null ) {
            return null;
        }
        String nombre = organizador.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }
}
