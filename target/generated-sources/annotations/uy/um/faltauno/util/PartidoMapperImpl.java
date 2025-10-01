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
    date = "2025-09-30T18:43:32-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.43.0.v20250819-1513, environment: Java 21.0.8 (Eclipse Adoptium)"
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
        partidoDTO.setDescripcion( partido.getDescripcion() );
        partidoDTO.setDireccionUbicacion( partido.getDireccionUbicacion() );
        partidoDTO.setDuracionMinutos( partido.getDuracionMinutos() );
        partidoDTO.setEstado( partido.getEstado() );
        partidoDTO.setFecha( partido.getFecha() );
        partidoDTO.setGenero( partido.getGenero() );
        partidoDTO.setHora( partido.getHora() );
        partidoDTO.setId( partido.getId() );
        partidoDTO.setJugadoresActuales( partido.getJugadoresActuales() );
        partidoDTO.setLatitud( partido.getLatitud() );
        partidoDTO.setLongitud( partido.getLongitud() );
        partidoDTO.setMaxJugadores( partido.getMaxJugadores() );
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

        Partido.PartidoBuilder partido = Partido.builder();

        partido.organizador( partidoDTOToUsuario( dto ) );
        partido.descripcion( dto.getDescripcion() );
        partido.direccionUbicacion( dto.getDireccionUbicacion() );
        partido.duracionMinutos( dto.getDuracionMinutos() );
        partido.estado( dto.getEstado() );
        partido.fecha( dto.getFecha() );
        partido.genero( dto.getGenero() );
        partido.hora( dto.getHora() );
        partido.id( dto.getId() );
        partido.jugadoresActuales( dto.getJugadoresActuales() );
        partido.latitud( dto.getLatitud() );
        partido.longitud( dto.getLongitud() );
        partido.maxJugadores( dto.getMaxJugadores() );
        partido.nombreUbicacion( dto.getNombreUbicacion() );
        partido.precioTotal( dto.getPrecioTotal() );
        partido.tipoPartido( dto.getTipoPartido() );

        return partido.build();
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

    protected Usuario partidoDTOToUsuario(PartidoDTO partidoDTO) {
        if ( partidoDTO == null ) {
            return null;
        }

        Usuario.UsuarioBuilder usuario = Usuario.builder();

        usuario.id( partidoDTO.getOrganizadorId() );
        usuario.nombre( partidoDTO.getOrganizadorNombre() );

        return usuario.build();
    }
}
