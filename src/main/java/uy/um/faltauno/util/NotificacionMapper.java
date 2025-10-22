package uy.um.faltauno.util;

import org.mapstruct.Mapper;
import uy.um.faltauno.dto.NotificacionDTO;
import uy.um.faltauno.entity.Notificacion;

@Mapper(componentModel = "spring")
public interface NotificacionMapper {
    
    NotificacionDTO toDTO(Notificacion notificacion);
    
    Notificacion toEntity(NotificacionDTO dto);
}
