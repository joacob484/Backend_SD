package uy.um.faltauno.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partidos")
public class Partido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String cancha;

  @Column(nullable = false)
  private LocalDateTime fechaHora;

  @Column(nullable = false)
  private Integer maxJugadores;

  @Column(nullable = false)
  private Integer confirmados = 0;

  @Column(nullable = false)
  private String zona;   // si después querés enum/tabla aparte, lo cambiamos

  @Column(nullable = false)
  private String nivel;  // idem (BEGINNER/INTERMEDIATE/ADVANCED)

  // ==== Getters & Setters ====
  public Long getId() { return id; }
  public String getCancha() { return cancha; }
  public void setCancha(String cancha) { this.cancha = cancha; }

  public LocalDateTime getFechaHora() { return fechaHora; }
  public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

  public Integer getMaxJugadores() { return maxJugadores; }
  public void setMaxJugadores(Integer maxJugadores) { this.maxJugadores = maxJugadores; }

  public Integer getConfirmados() { return confirmados; }
  public void setConfirmados(Integer confirmados) { this.confirmados = confirmados; }

  public String getZona() { return zona; }
  public void setZona(String zona) { this.zona = zona; }

  public String getNivel() { return nivel; }
  public void setNivel(String nivel) { this.nivel = nivel; }
}
