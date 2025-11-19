# Grafana Configuration

Este directorio contiene archivos de configuración para Grafana Agent y Grafana Alloy.

## ⚠️ Configuración de Secretos

Los archivos de configuración usan **variables de entorno** para proteger credenciales sensibles.

### Paso 1: Crear archivo de variables de entorno

```bash
cp .env.grafana.example .env.grafana
```

### Paso 2: Editar `.env.grafana` con tus credenciales

```bash
# .env.grafana
GRAFANA_USERNAME=tu_username_aqui
GRAFANA_API_TOKEN=tu_api_token_aqui
GRAFANA_SERVICE_ACCOUNT_TOKEN=tu_service_account_token_aqui
```

### Paso 3: Cargar las variables antes de ejecutar

#### Para Grafana Agent (grafana-agent-config.yaml)
```bash
# Exportar variables
export $(cat .env.grafana | xargs)

# Ejecutar con envsubst para reemplazar variables
envsubst < grafana-agent-config.yaml | grafana-agent --config.file=/dev/stdin
```

#### Para Grafana Alloy (alloy-config.alloy)
```bash
# Exportar variables
export $(cat .env.grafana | xargs)

# Ejecutar Alloy (lee variables automáticamente con env())
alloy run alloy-config.alloy
```

## 📁 Archivos

- `grafana-agent-config.yaml` - Configuración para Grafana Agent (usa ${VAR} syntax)
- `alloy-config.alloy` - Configuración para Grafana Alloy (usa env("VAR") syntax)
- `.env.grafana.example` - Plantilla de variables de entorno
- `.env.grafana` - Tus credenciales reales (ignorado por git)

## 🔒 Seguridad

**NUNCA** hagas commit de archivos con credenciales hardcodeadas. 

El archivo `.env.grafana` está en `.gitignore` y no se subirá a git.
