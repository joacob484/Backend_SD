package uy.um.faltauno.util;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-17T10:49:36-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251001-1143, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public UsuarioDTO toDTO(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UsuarioDTO.UsuarioDTOBuilder usuarioDTO = UsuarioDTO.builder();

        usuarioDTO.fotoPerfil( mapBytesToBase64( usuario.getFotoPerfil() ) );
        usuarioDTO.fechaNacimiento( mapLocalDateToString( usuario.getFechaNacimiento() ) );
        usuarioDTO.altura( usuario.getAltura() );
        usuarioDTO.apellido( usuario.getApellido() );
        usuarioDTO.cedula( usuario.getCedula() );
        usuarioDTO.celular( usuario.getCelular() );
        usuarioDTO.email( usuario.getEmail() );
        usuarioDTO.id( usuario.getId() );
        usuarioDTO.nombre( usuario.getNombre() );
        usuarioDTO.password( usuario.getPassword() );
        usuarioDTO.peso( usuario.getPeso() );
        usuarioDTO.posicion( usuario.getPosicion() );

        return usuarioDTO.build();
    }

    @Override
    public Usuario toEntity(UsuarioDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Usuario.UsuarioBuilder usuario = Usuario.builder();

        usuario.fotoPerfil( mapBase64ToBytes( dto.getFotoPerfil() ) );
        usuario.fechaNacimiento( mapStringToLocalDate( dto.getFechaNacimiento() ) );
        usuario.altura( dto.getAltura() );
        usuario.apellido( dto.getApellido() );
        usuario.cedula( dto.getCedula() );
        usuario.celular( dto.getCelular() );
        usuario.email( dto.getEmail() );
        usuario.id( dto.getId() );
        usuario.nombre( dto.getNombre() );
        usuario.password( dto.getPassword() );
        usuario.peso( dto.getPeso() );
        usuario.posicion( dto.getPosicion() );

        return usuario.build();
    }
}
