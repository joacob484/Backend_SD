package uy.um.faltauno.util;

import uy.um.faltauno.entity.Partido;
import uy.um.faltauno.dto.PartidoDto;

public class PartidoMapper {
    public static PartidoDto toDto(Partido p) {
        PartidoDto d = new PartidoDto();
        d.id = p.getId();
        d.cancha = p.getCancha();
        d.fechaHora = p.getFechaHora();
        d.maxJugadores = p.getMaxJugadores();
        d.confirmados = p.getConfirmados();
        d.zona = p.getZona();
        d.nivel = p.getNivel();
        return d;
    }
}

