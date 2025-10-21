package uy.um.faltauno.util;

import org.mapstruct.*;
import uy.um.faltauno.dto.InscripcionDTO;
import uy.um.faltauno.dto.UsuarioMinDTO;
import uy.um.faltauno.entity.Inscripcion;
import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.entity.Usuario;

import java.time.Duration;
import java.time.Instant;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface InscripcionMapper {
    
    @Mapping(source = "partido.id", target = "partidoId")
    @Mapping(source = "usuario.id", target = "usuarioId")
    @Mapping(source = "estado", target = "estado")  // ✅ MapStruct maneja el enum automáticamente
    @Mapping(source = "usuario", target = "usuario", qualifiedByName = "toUsuarioMinDTO")
    @Mapping(source = "partido", target = "partido", qualifiedByName = "toPartidoMinDTO")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "fechaAceptacion", target = "fechaAceptacion")
    @Mapping(source = "fechaRechazo", target = "fechaRechazo")
    @Mapping(source = "fechaCancelacion", target = "fechaCancelacion")
    @Mapping(target = "tiempoTranscurrido", expression = "java(calcularTiempoTranscurrido(inscripcion.getCreatedAt()))")
    @Mapping(target = "puedeCancelar", expression = "java(inscripcion.isActiva())")
    @Mapping(target = "puedeAceptar", expression = "java(inscripcion.isPendiente())")
    @Mapping(target = "puedeRechazar", expression = "java(inscripcion.isPendiente())")
    InscripcionDTO toDTO(Inscripcion inscripcion);

    @Mapping(target = "partido", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "fechaAceptacion", ignore = true)
    @Mapping(target = "fechaRechazo", ignore = true)
    @Mapping(target = "fechaCancelacion", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "comentario", ignore = true)
    @Mapping(target = "motivoRechazo", ignore = true)
    Inscripcion toEntity(InscripcionDTO dto);
    
    @Named("toUsuarioMinDTO")
    default UsuarioMinDTO toUsuarioMinDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioMinDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getFotoPerfil()
        );
    }
    
    @Named("toPartidoMinDTO")
    default InscripcionDTO.PartidoMinDTO toPartidoMinDTO(Partido partido) {
        if (partido == null) {
            return null;
        }
        
        String organizadorNombre = null;
        if (partido.getOrganizador() != null) {
            Usuario org = partido.getOrganizador();
            organizadorNombre = (org.getNombre() != null ? org.getNombre() : "") + " " + 
                              (org.getApellido() != null ? org.getApellido() : "");
            organizadorNombre = organizadorNombre.trim();
            if (organizadorNombre.isEmpty()) {
                organizadorNombre = null;
            }
        }
        
        return InscripcionDTO.PartidoMinDTO.builder()
                .id(partido.getId())
                .tipoPartido(partido.getTipoPartido())
                .genero(partido.getGenero())
                .fecha(partido.getFecha() != null ? partido.getFecha().toString() : null)
                .hora(partido.getHora() != null ? partido.getHora().toString() : null)
                .nombreUbicacion(partido.getNombreUbicacion())
                .estado(partido.getEstado())
                .organizadorNombre(organizadorNombre)
                .build();
    }
    
    /**
     * Calcular tiempo transcurrido desde la creación
     */
    default String calcularTiempoTranscurrido(Instant createdAt) {
        if (createdAt == null) {
            return null;
        }
        
        Instant ahora = Instant.now();
        Duration duracion = Duration.between(createdAt, ahora);
        
        long segundos = duracion.getSeconds();
        
        if (segundos < 60) {
            return "Hace " + segundos + " segundo" + (segundos != 1 ? "s" : "");
        }
        
        long minutos = segundos / 60;
        if (minutos < 60) {
            return "Hace " + minutos + " minuto" + (minutos != 1 ? "s" : "");
        }
        
        long horas = minutos / 60;
        if (horas < 24) {
            return "Hace " + horas + " hora" + (horas != 1 ? "s" : "");
        }
        
        long dias = horas / 24;
        if (dias < 30) {
            return "Hace " + dias + " día" + (dias != 1 ? "s" : "");
        }
        
        long meses = dias / 30;
        if (meses < 12) {
            return "Hace " + meses + " mes" + (meses != 1 ? "es" : "");
        }
        
        long años = meses / 12;
        return "Hace " + años + " año" + (años != 1 ? "s" : "");
    }
}