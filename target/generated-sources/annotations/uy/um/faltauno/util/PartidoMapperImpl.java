package uy.um.faltauno.util;

import java.util.UUID;
import javax.annotation.processing.Generated;
import uy.um.faltauno.dto.PartidoDto;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-09-23T20:16:49-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.43.0.v20250819-1513, environment: Java 21.0.8 (Eclipse Adoptium)"
)
public class PartidoMapperImpl implements PartidoMapper {

    @Override
    public PartidoDto toDto(Partido partido) {
        if ( partido == null ) {
            return null;
        }

        PartidoDto partidoDto = new PartidoDto();

        partidoDto.setOrganizadorId( partidoOrganizadorId( partido ) );
        partidoDto.setDescripcion( partido.getDescripcion() );
        partidoDto.setDireccionUbicacion( partido.getDireccionUbicacion() );
        partidoDto.setDuracionMinutos( partido.getDuracionMinutos() );
        partidoDto.setFecha( partido.getFecha() );
        partidoDto.setGenero( partido.getGenero() );
        partidoDto.setHora( partido.getHora() );
        partidoDto.setId( partido.getId() );
        partidoDto.setMaxJugadores( partido.getMaxJugadores() );
        partidoDto.setNombreUbicacion( partido.getNombreUbicacion() );
        partidoDto.setPrecioTotal( partido.getPrecioTotal() );
        partidoDto.setTipoPartido( partido.getTipoPartido() );

        return partidoDto;
    }

    @Override
    public Partido toEntity(PartidoDto dto) {
        if ( dto == null ) {
            return null;
        }

        Partido.PartidoBuilder partido = Partido.builder();

        partido.organizador( partidoDtoToUsuario( dto ) );
        partido.descripcion( dto.getDescripcion() );
        partido.direccionUbicacion( dto.getDireccionUbicacion() );
        partido.duracionMinutos( dto.getDuracionMinutos() );
        partido.fecha( dto.getFecha() );
        partido.genero( dto.getGenero() );
        partido.hora( dto.getHora() );
        partido.id( dto.getId() );
        partido.maxJugadores( dto.getMaxJugadores() );
        partido.nombreUbicacion( dto.getNombreUbicacion() );
        partido.precioTotal( dto.getPrecioTotal() );
        partido.tipoPartido( dto.getTipoPartido() );

        return partido.build();
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

    protected Usuario partidoDtoToUsuario(PartidoDto partidoDto) {
        if ( partidoDto == null ) {
            return null;
        }

        Usuario.UsuarioBuilder usuario = Usuario.builder();

        usuario.id( partidoDto.getOrganizadorId() );

        return usuario.build();
    }
}
