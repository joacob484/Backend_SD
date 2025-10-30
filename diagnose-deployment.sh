#!/bin/bash
# Script para diagnosticar problemas de deployment

echo "🔍 DIAGNÓSTICO DE DEPLOYMENT - FALTA UNO"
echo "========================================"
echo ""

# Check if we're in git repo
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ No estás en un repositorio git"
    exit 1
fi

echo "📌 Commit actual:"
git log -1 --oneline
echo ""

echo "📝 Verificando archivos clave..."
echo ""

# Check critical files
FILES=(
    "Dockerfile.cloudrun"
    "cloudbuild-cloudrun.yaml"
    ".github/workflows/deploy.yml"
    "scripts/cloudrun-deploy.sh"
    "src/main/resources/application.yaml"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file FALTA"
    fi
done

echo ""
echo "🔍 Verificando sintaxis de cloudbuild-cloudrun.yaml..."

# Check if yq or python is available for YAML validation
if command -v python3 &> /dev/null; then
    python3 << 'PYTHON'
import yaml
import sys

try:
    with open('cloudbuild-cloudrun.yaml', 'r') as f:
        config = yaml.safe_load(f)
    
    # Check required sections
    if 'steps' not in config:
        print("❌ Falta sección 'steps'")
        sys.exit(1)
    
    if 'availableSecrets' in config:
        print("✅ availableSecrets está al nivel correcto")
    else:
        print("⚠️  No se encontró availableSecrets")
    
    if 'substitutions' in config:
        print("✅ substitutions configurado")
        subs = config['substitutions']
        print(f"   Variables: {len(subs)} configuradas")
    
    print("✅ cloudbuild-cloudrun.yaml es YAML válido")
    
except yaml.YAMLError as e:
    print(f"❌ ERROR en YAML: {e}")
    sys.exit(1)
except FileNotFoundError:
    print("❌ No se encontró cloudbuild-cloudrun.yaml")
    sys.exit(1)
except Exception as e:
    print(f"❌ Error: {e}")
    sys.exit(1)
PYTHON
else
    echo "⚠️  Python no disponible, no se puede validar YAML"
fi

echo ""
echo "🔍 Verificando application.yaml..."

if grep -q "JWT_SECRET:.*changeme" src/main/resources/application.yaml; then
    echo "✅ JWT_SECRET tiene valor por defecto"
else
    echo "⚠️  JWT_SECRET podría no tener default"
fi

if grep -q "GOOGLE_CLIENT_ID:.*not-configured" src/main/resources/application.yaml; then
    echo "✅ GOOGLE_CLIENT_ID tiene valor por defecto"
else
    echo "⚠️  GOOGLE_CLIENT_ID podría no tener default"
fi

echo ""
echo "📊 Estado del repositorio:"
if git diff --quiet; then
    echo "✅ No hay cambios sin commit"
else
    echo "⚠️  Hay cambios sin commit:"
    git status -s
fi

echo ""
echo "🌐 Último push:"
git log origin/main -1 --oneline 2>/dev/null || echo "⚠️  No se puede verificar origin/main"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💡 PRÓXIMOS PASOS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "1. Ve a GitHub Actions:"
echo "   https://github.com/joacob484/Backend_SD/actions"
echo ""
echo "2. Busca el workflow que falló"
echo ""
echo "3. Copia el mensaje de error COMPLETO"
echo ""
echo "4. Pégalo aquí para diagnosticar el problema exacto"
echo ""
