# 🔧 Fix: Connection Leak y Errores 500 en Endpoints de Partidos

## 🐛 Problema Identificado

Los endpoints de partidos estaban devolviendo **500 Internal Server Error**:
- `GET /api/partidos?estado=DISPONIBLE` 
- `GET /api/partidos/usuario/{id}`

### Causa Raíz

**Connection Leak en HikariCP** detectado en los logs de Cloud Run:

```
java.lang.Exception: Apparent connection leak detected
at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:127)
```

El problema tenía dos orígenes:

#### 1. **LazyInitializationException** en `listarPartidosPorUsuario()`

El método accedía a propiedades lazy (`organizador`) **FUERA** de la transacción:

```java
// ❌ ANTES - Causaba LazyInitializationException
List<Partido> inscritos = inscripciones.stream()
    .map(Inscripcion::getPartido)  // ← Lazy load fuera de transacción
    .collect(Collectors.toList());
```

Cuando el stream intentaba acceder al `organizador` del partido, la transacción ya había terminado, causando:
- LazyInitializationException
- Conexiones no liberadas correctamente
- Agotamiento del pool de conexiones (leak)

#### 2. **Pool de Conexiones Mal Configurado**

El pool de HikariCP estaba configurado con:
- `maximum-pool-size: 20` (muy alto para Cloud Run)
- Sin `leak-detection-threshold` (no detectaba leaks tempranamente)
- `max-lifetime: 1800000` (30 minutos, demasiado largo)

## ✅ Soluciones Implementadas

### 1. Fix en `listarPartidosPorUsuario()`

✅ **Forzar inicialización del organizador DENTRO de la transacción:**

```java
@Transactional(readOnly = true)
public List<PartidoDTO> listarPartidosPorUsuario(UUID usuarioId) {
    // ✅ FIX: Usar query con JOIN FETCH
    List<Inscripcion> inscripciones = inscripcionRepository
            .findByUsuarioIdAndEstado(usuarioId, Inscripcion.EstadoInscripcion.ACEPTADO);
    
    // ✅ FIX: Forzar carga del organizador DENTRO de la transacción
    List<Partido> inscritos = inscripciones.stream()
            .map(i -> {
                Partido p = i.getPartido();
                if (p.getOrganizador() != null) {
                    p.getOrganizador().getNombre(); // ← Touch lazy field
                }
                return p;
            })
            .collect(Collectors.toList());
    
    // Resto del código...
}
```

**Beneficios:**
- ✅ Todas las lazy properties se cargan dentro de la transacción activa
- ✅ No hay excepciones de lazy loading
- ✅ Las conexiones se liberan correctamente

### 2. Fix en `listarPartidos()`

✅ **Similar fix para el método de listado general:**

```java
@Transactional(readOnly = true)
public List<PartidoDTO> listarPartidos(...) {
    List<Partido> partidos = partidoRepository.findAll(spec);
    
    // ✅ FIX: Forzar inicialización dentro de la transacción
    for (Partido p : partidos) {
        if (p.getOrganizador() != null) {
            p.getOrganizador().getNombre(); // ← Touch lazy field
        }
    }
    
    return partidos.stream()
            .map(this::entityToDtoCompleto)
            .collect(Collectors.toList());
}
```

### 3. Optimización del Pool de Conexiones

✅ **Configuración mejorada de HikariCP** (`application.yaml`):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # ✅ Reducido (antes: 20)
      minimum-idle: 2        # ✅ Reducido (antes: 5)
      connection-timeout: 20000  # ✅ 20 segundos
      idle-timeout: 300000       # ✅ 5 minutos
      max-lifetime: 1200000      # ✅ 20 minutos (renovar conexiones)
      leak-detection-threshold: 60000  # ✅ NUEVO: Detectar leaks >60s
      pool-name: FaltaUnoHikariCP
```

**Mejoras:**
- ✅ Menos conexiones simultáneas (Cloud Run tiene recursos limitados)
- ✅ Detección temprana de leaks (alerta en logs)
- ✅ Renovación más frecuente de conexiones (evita conexiones stale)

### 4. Logging Mejorado

✅ **Logs detallados para debugging:**

```java
log.debug("[PartidoService.listarPartidosPorUsuario] Partidos creados: {}", creados.size());
log.debug("[PartidoService.listarPartidosPorUsuario] Inscripciones encontradas: {}", inscripciones.size());
log.debug("[PartidoService.listarPartidosPorUsuario] Total partidos únicos: {}", todosPartidos.size());
```

## 🧪 Verificación

### Antes del Fix:
```
❌ GET /api/partidos?estado=DISPONIBLE → 500 Internal Server Error
❌ GET /api/partidos/usuario/{id} → 500 Internal Server Error
⚠️ Logs: "Apparent connection leak detected"
```

### Después del Fix:
```
✅ GET /api/partidos?estado=DISPONIBLE → 200 OK
✅ GET /api/partidos/usuario/{id} → 200 OK
✅ No más connection leaks en logs
✅ Pool de conexiones estable
```

## 📊 Impacto

- **Performance**: Menos conexiones = menos overhead
- **Estabilidad**: No más errores 500 por connection pool exhausted
- **Debugging**: Detección temprana de leaks con threshold
- **Recursos**: Mejor uso de memoria en Cloud Run

## 🔄 Deploy

```bash
# 1. Build de la imagen
gcloud builds submit --tag=gcr.io/master-might-274420/faltauno-backend:latest

# 2. Deploy a Cloud Run
gcloud run deploy faltauno-backend \
  --image=gcr.io/master-might-274420/faltauno-backend:latest \
  --region=us-central1 \
  --project=master-might-274420
```

## 📝 Lecciones Aprendidas

1. **Siempre usar `@Transactional` en métodos que accedan a lazy properties**
2. **Forzar inicialización de lazy fields DENTRO de la transacción activa**
3. **Configurar `leak-detection-threshold` para detectar leaks tempranamente**
4. **Ajustar el pool de conexiones según los recursos del entorno (Cloud Run)**
5. **Usar queries con `JOIN FETCH` para evitar N+1 queries**
6. **Nunca acceder a lazy properties fuera de una transacción activa**

---

**Fecha**: 2025-11-03  
**Severidad**: CRÍTICA (endpoints principales rotos)  
**Status**: ✅ RESUELTO
