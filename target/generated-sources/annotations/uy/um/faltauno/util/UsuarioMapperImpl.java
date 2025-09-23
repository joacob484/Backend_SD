package uy.um.faltauno.util;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-09-23T20:21:06-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.43.0.v20250819-1513, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public UsuarioDTO toDTO(Usuario usuario) {
        if ( usuario == null ) {
            return null;
        }

        UsuarioDTO.UsuarioDTOBuilder usuarioDTO = UsuarioDTO.builder();

        usuarioDTO.altura( usuario.getAltura() );
        usuarioDTO.apellido( usuario.getApellido() );
        usuarioDTO.cedula( usuario.getCedula() );
        usuarioDTO.celular( usuario.getCelular() );
        usuarioDTO.edad( usuario.getEdad() );
        usuarioDTO.email( usuario.getEmail() );
        usuarioDTO.fotoPerfil( usuario.getFotoPerfil() );
        usuarioDTO.id( usuario.getId() );
        usuarioDTO.nombre( usuario.getNombre() );
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

        usuario.altura( dto.getAltura() );
        usuario.apellido( dto.getApellido() );
        usuario.cedula( dto.getCedula() );
        usuario.celular( dto.getCelular() );
        usuario.edad( dto.getEdad() );
        usuario.email( dto.getEmail() );
        usuario.fotoPerfil( dto.getFotoPerfil() );
        usuario.id( dto.getId() );
        usuario.nombre( dto.getNombre() );
        usuario.peso( dto.getPeso() );
        usuario.posicion( dto.getPosicion() );

        return usuario.build();
    }
}
