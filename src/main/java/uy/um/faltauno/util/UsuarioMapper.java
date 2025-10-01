package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uy.um.faltauno.dto.UsuarioDTO;
import uy.um.faltauno.entity.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    // MapStruct mapea byte[] OK por defecto; mapeos explícitos incluidos por claridad
    @Mapping(source = "fotoPerfil", target = "fotoPerfil")
    UsuarioDTO toDTO(Usuario usuario);

    @Mapping(source = "fotoPerfil", target = "fotoPerfil")
    Usuario toEntity(UsuarioDTO dto);
}