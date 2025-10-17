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
    date = "2025-10-17T10:39:21-0300",
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
        partidoDTO.setCantidadJugadores( partido.getCantidadJugadores() );
        partidoDTO.setDescripcion( partido.getDescripcion() );
        partidoDTO.setDireccionUbicacion( partido.getDireccionUbicacion() );
        partidoDTO.setDuracionMinutos( partido.getDuracionMinutos() );
        partidoDTO.setFecha( partido.getFecha() );
        partidoDTO.setGenero( partido.getGenero() );
        partidoDTO.setHora( partido.getHora() );
        partidoDTO.setId( partido.getId() );
        partidoDTO.setLatitud( partido.getLatitud() );
        partidoDTO.setLongitud( partido.getLongitud() );
        partidoDTO.setNombreUbicacion( partido.getNombreUbicacion() );
        partidoDTO.setPrecioTotal( partido.getPrecioTotal() );
        partidoDTO.setTipoPartido( partido.getTipoPartido() );

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
        partido.setCantidadJugadores( dto.getCantidadJugadores() );
        partido.setDescripcion( dto.getDescripcion() );
        partido.setDireccionUbicacion( dto.getDireccionUbicacion() );
        partido.setDuracionMinutos( dto.getDuracionMinutos() );
        partido.setFecha( dto.getFecha() );
        partido.setGenero( dto.getGenero() );
        partido.setHora( dto.getHora() );
        partido.setId( dto.getId() );
        partido.setLatitud( dto.getLatitud() );
        partido.setLongitud( dto.getLongitud() );
        partido.setNombreUbicacion( dto.getNombreUbicacion() );
        partido.setPrecioTotal( dto.getPrecioTotal() );
        partido.setTipoPartido( dto.getTipoPartido() );

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

        if ( dto.getCantidadJugadores() != null ) {
            entity.setCantidadJugadores( dto.getCantidadJugadores() );
        }
        if ( dto.getDescripcion() != null ) {
            entity.setDescripcion( dto.getDescripcion() );
        }
        if ( dto.getDireccionUbicacion() != null ) {
            entity.setDireccionUbicacion( dto.getDireccionUbicacion() );
        }
        if ( dto.getDuracionMinutos() != null ) {
            entity.setDuracionMinutos( dto.getDuracionMinutos() );
        }
        if ( dto.getFecha() != null ) {
            entity.setFecha( dto.getFecha() );
        }
        if ( dto.getGenero() != null ) {
            entity.setGenero( dto.getGenero() );
        }
        if ( dto.getHora() != null ) {
            entity.setHora( dto.getHora() );
        }
        if ( dto.getId() != null ) {
            entity.setId( dto.getId() );
        }
        if ( dto.getLatitud() != null ) {
            entity.setLatitud( dto.getLatitud() );
        }
        if ( dto.getLongitud() != null ) {
            entity.setLongitud( dto.getLongitud() );
        }
        if ( dto.getNombreUbicacion() != null ) {
            entity.setNombreUbicacion( dto.getNombreUbicacion() );
        }
        if ( dto.getPrecioTotal() != null ) {
            entity.setPrecioTotal( dto.getPrecioTotal() );
        }
        if ( dto.getTipoPartido() != null ) {
            entity.setTipoPartido( dto.getTipoPartido() );
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
