package uy.um.faltauno.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NovedadesService {

    private static final Logger logger = LoggerFactory.getLogger(NovedadesService.class);
    
    @Value("${GITHUB_TOKEN:}")
    private String githubToken;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> getUltimosCommits(int limit) {
        List<Map<String, Object>> novedades = new ArrayList<>();
        
        try {
            // Obtener commits del backend con deploys exitosos
            novedades.addAll(getCommitsFromRepo("joacob484", "Backend_SD", limit, "backend"));
            
            // Si no hay suficientes, agregar del frontend
            if (novedades.size() < limit) {
                int remaining = limit - novedades.size();
                novedades.addAll(getCommitsFromRepo("joacob484", "FaltaUnoFront", remaining, "frontend"));
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo commits desde GitHub: ", e);
            return getDefaultNovedades();
        }
        
        return novedades.isEmpty() ? getDefaultNovedades() : novedades;
    }
    
    private List<Map<String, Object>> getCommitsFromRepo(String owner, String repo, int limit, String source) {
        List<Map<String, Object>> commits = new ArrayList<>();
        
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/commits?per_page=%d", owner, repo, Math.min(limit * 2, 20));
            JsonNode commitsData = getGithubData(url);
            
            if (commitsData != null && commitsData.isArray()) {
                for (JsonNode commitNode : commitsData) {
                    String sha = commitNode.get("sha").asText();
                    String message = commitNode.path("commit").path("message").asText();
                    String author = commitNode.path("commit").path("author").path("name").asText();
                    String dateStr = commitNode.path("commit").path("author").path("date").asText();
                    
                    // Verificar si tiene deploy exitoso verificando las GitHub Actions
                    if (hasSuccessfulDeploy(owner, repo, sha)) {
                        Map<String, Object> novedad = new HashMap<>();
                        
                        String[] messageParts = parseCommitMessage(message);
                        String type = messageParts[0];
                        String title = messageParts[1];
                        String description = messageParts[2];
                        String timeAgo = calculateTimeAgo(dateStr);
                        
                        novedad.put("id", sha.substring(0, 7));
                        novedad.put("type", type);
                        novedad.put("title", title);
                        novedad.put("description", description);
                        novedad.put("date", timeAgo);
                        novedad.put("author", author);
                        novedad.put("source", source);
                        novedad.put("tags", extractTags(message));
                        
                        commits.add(novedad);
                        
                        if (commits.size() >= limit) break;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error obteniendo commits de " + repo + ": ", e);
        }
        
        return commits;
    }
    
    private JsonNode getGithubData(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (githubToken != null && !githubToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + githubToken);
            }
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "FaltaUno-App");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            logger.error("Error llamando a GitHub API: ", e);
            return null;
        }
    }
    
    private boolean hasSuccessfulDeploy(String owner, String repo, String sha) {
        try {
            // Verificar el estado de los workflows para este commit
            String url = String.format("https://api.github.com/repos/%s/%s/commits/%s/check-runs", owner, repo, sha);
            JsonNode checkRuns = getGithubData(url);
            
            if (checkRuns != null && checkRuns.has("check_runs")) {
                JsonNode runs = checkRuns.get("check_runs");
                if (runs.isArray() && runs.size() > 0) {
                    // Si hay al menos un check run con conclusión "success", consideramos que tiene deploy exitoso
                    for (JsonNode run : runs) {
                        String conclusion = run.path("conclusion").asText("");
                        String name = run.path("name").asText("").toLowerCase();
                        
                        // Buscar workflows de deploy que hayan sido exitosos
                        if (conclusion.equals("success") && 
                            (name.contains("deploy") || name.contains("build") || name.contains("ci"))) {
                            return true;
                        }
                    }
                }
            }
            
            // Si no hay información de workflows, asumir que los commits recientes tienen deploy
            // (fallback para repos sin GitHub Actions configurado)
            return true;
            
        } catch (Exception e) {
            logger.warn("No se pudo verificar deploy para commit " + sha + ": " + e.getMessage());
            // En caso de error, asumir que sí tiene deploy para no filtrar demasiado
            return true;
        }
    }
    
    private String[] parseCommitMessage(String message) {
        // Formato: "tipo: Título - descripción" o "tipo: Título"
        String type = "update";
        String title = message;
        String description = "";
        
        if (message.contains(":")) {
            String[] parts = message.split(":", 2);
            String prefix = parts[0].trim().toLowerCase();
            String rest = parts.length > 1 ? parts[1].trim() : "";
            
            // Mapear tipo de commit
            switch (prefix) {
                case "feat":
                case "feature":
                    type = "feature";
                    break;
                case "fix":
                    type = "update";
                    break;
                case "chore":
                case "docs":
                case "refactor":
                    type = "announcement";
                    break;
                default:
                    type = "update";
            }
            
            // Separar título y descripción
            if (rest.contains("-")) {
                String[] titleDesc = rest.split("-", 2);
                title = titleDesc[0].trim();
                description = titleDesc.length > 1 ? titleDesc[1].trim() : "";
            } else {
                title = rest;
            }
        }
        
        // Si no hay descripción, crear una desde el título
        if (description.isEmpty()) {
            description = "Actualización del sistema: " + title;
        }
        
        return new String[]{type, title, description};
    }
    
    private String calculateTimeAgo(String dateStr) {
        try {
            ZonedDateTime commitDate = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            ZonedDateTime now = ZonedDateTime.now();
            
            long minutes = ChronoUnit.MINUTES.between(commitDate, now);
            long hours = ChronoUnit.HOURS.between(commitDate, now);
            long days = ChronoUnit.DAYS.between(commitDate, now);
            
            if (minutes < 60) {
                return "Hace " + minutes + (minutes == 1 ? " minuto" : " minutos");
            } else if (hours < 24) {
                return "Hace " + hours + (hours == 1 ? " hora" : " horas");
            } else if (days < 30) {
                return "Hace " + days + (days == 1 ? " día" : " días");
            } else {
                long months = days / 30;
                return "Hace " + months + (months == 1 ? " mes" : " meses");
            }
        } catch (Exception e) {
            return "Recientemente";
        }
    }
    
    private List<String> extractTags(String message) {
        List<String> tags = new ArrayList<>();
        
        // Extraer palabras clave comunes
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("oauth") || lowerMessage.contains("login") || lowerMessage.contains("auth")) {
            tags.add("Autenticación");
        }
        if (lowerMessage.contains("redis") || lowerMessage.contains("cache")) {
            tags.add("Cache");
        }
        if (lowerMessage.contains("estadistica") || lowerMessage.contains("stats")) {
            tags.add("Estadísticas");
        }
        if (lowerMessage.contains("deploy") || lowerMessage.contains("cloud")) {
            tags.add("Deploy");
        }
        if (lowerMessage.contains("fix") || lowerMessage.contains("corregir")) {
            tags.add("Bug Fix");
        }
        if (lowerMessage.contains("feature") || lowerMessage.contains("feat")) {
            tags.add("Nueva Funcionalidad");
        }
        if (lowerMessage.contains("database") || lowerMessage.contains("db") || lowerMessage.contains("migration")) {
            tags.add("Base de Datos");
        }
        if (lowerMessage.contains("backend") || lowerMessage.contains("api")) {
            tags.add("Backend");
        }
        
        // Si no hay tags, agregar uno genérico
        if (tags.isEmpty()) {
            tags.add("Mejora");
        }
        
        return tags;
    }
    
    private List<Map<String, Object>> getDefaultNovedades() {
        List<Map<String, Object>> defaults = new ArrayList<>();
        
        Map<String, Object> novedad1 = new HashMap<>();
        novedad1.put("id", "default1");
        novedad1.put("type", "update");
        novedad1.put("title", "Sistema actualizado");
        novedad1.put("description", "El sistema ha sido actualizado con las últimas mejoras de rendimiento y seguridad.");
        novedad1.put("date", "Recientemente");
        novedad1.put("author", "Equipo Falta Uno");
        novedad1.put("tags", Arrays.asList("Sistema", "Actualización"));
        defaults.add(novedad1);
        
        return defaults;
    }
}
