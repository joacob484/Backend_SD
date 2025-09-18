package uy.um.faltauno.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "partidos")
public class Partido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cancha;
    private LocalDateTime fechaHora;
    private int maxJugadores;
    private int confirmados;

    // metadata: zona, nivel
    private String zona;
    private String nivel;

    // getters & setters
    // constructor vacío
}
