package uy.um.faltauno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uy.um.faltauno.dto.ContactoDTO;
import uy.um.faltauno.entity.Contacto;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ContactoMapper {
    
    @Mapping(target = "usuarioAppId", expression = "java(mapUsuarioAppId(contacto))")
    @Mapping(target = "fotoPerfil", ignore = true) // La foto se maneja por separado
    @Mapping(target = "email", expression = "java(mapEmail(contacto))")
    ContactoDTO toDTO(Contacto contacto);
    
    List<ContactoDTO> toDTOList(List<Contacto> contactos);
    
    default Long mapUsuarioAppId(Contacto contacto) {
        if (contacto == null || contacto.getUsuarioApp() == null || contacto.getUsuarioApp().getId() == null) {
            return null;
        }
        UUID uuid = contacto.getUsuarioApp().getId();
        return uuid.getMostSignificantBits() & Long.MAX_VALUE;
    }
    
    default String mapEmail(Contacto contacto) {
        if (contacto == null || contacto.getUsuarioApp() == null) {
            return null;
        }
        return contacto.getUsuarioApp().getEmail();
    }
}
