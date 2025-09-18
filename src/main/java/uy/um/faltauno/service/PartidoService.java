package uy.um.faltauno.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uy.um.faltauno.repository.PartidoRepository;
import uy.um.faltauno.entity.Partido;
import java.util.List;
import java.util.Map;

@Service
public class PartidoService {

    private final PartidoRepository repo;
    private final RabbitTemplate rabbitTemplate;

    public PartidoService(PartidoRepository repo, RabbitTemplate rabbitTemplate) {
        this.repo = repo;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Cacheable(value = "partidosPorZona", key = "#zona + '_' + #nivel", unless="#result==null || #result.isEmpty()")
    public List<Partido> buscarPartidos(String zona, String nivel) {
        return repo.findByZonaAndNivel(zona, nivel);
    }

    @Transactional
    @CacheEvict(value = "partidosPorZona", allEntries = true) // puedes hacer evict selectivo
    public Partido crearPartido(Partido p) {
        Partido saved = repo.save(p);

        // publicar evento DESPUÉS de commit para evitar inconsistencias
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                Map<String, Object> payload = Map.of(
                    "event", "PARTIDO_CREATED",
                    "partidoId", saved.getId(),
                    "fechaHora", saved.getFechaHora().toString(),
                    "zona", saved.getZona(),
                    "nivel", saved.getNivel()
                );
                rabbitTemplate.convertAndSend("exchange.partidos", "partidos.created", payload);
            }
        });

        return saved;
    }

    @Transactional
    @CacheEvict(value = "partidosPorZona", allEntries = true)
    public void joinPartido(Long partidoId) {
        Partido p = repo.findById(partidoId).orElseThrow(() -> new RuntimeException("Partido no encontrado"));
        if (p.getConfirmados() >= p.getMaxJugadores()) throw new RuntimeException("Partido lleno");
        p.setConfirmados(p.getConfirmados() + 1);
        repo.save(p);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                Map<String, Object> payload = Map.of(
                    "event","PLAYER_JOINED",
                    "partidoId", p.getId()
                );
                rabbitTemplate.convertAndSend("exchange.partidos", "partidos.created", payload);
            }
        });
    }
}
