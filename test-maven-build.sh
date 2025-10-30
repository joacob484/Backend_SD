#!/bin/bash

echo "════════════════════════════════════════════════════════"
echo "🔨 TEST BUILD LOCAL - Verificar que Maven funciona"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Este script va a:"
echo "1. Limpiar target/ completamente"
echo "2. Ejecutar mvn clean package (SIN Docker)"
echo "3. Verificar que se crea el JAR"
echo ""
echo "Presiona ENTER para continuar o Ctrl+C para cancelar"
read

cd "$(dirname "$0")"

echo ""
echo "════════════════════════════════════════════════════════"
echo "PASO 1: Limpiar target/"
echo "════════════════════════════════════════════════════════"
rm -rf target/
echo "✅ target/ eliminado"

echo ""
echo "════════════════════════════════════════════════════════"
echo "PASO 2: Maven clean package"
echo "════════════════════════════════════════════════════════"
echo ""

if command -v mvn &> /dev/null; then
    echo "✅ Maven encontrado: $(mvn --version | head -1)"
    echo ""
    echo "🔨 Ejecutando: mvn clean package -DskipTests"
    echo ""
    
    mvn clean package -DskipTests -B
    
    BUILD_STATUS=$?
    
    if [ $BUILD_STATUS -eq 0 ]; then
        echo ""
        echo "════════════════════════════════════════════════════════"
        echo "✅ BUILD EXITOSO"
        echo "════════════════════════════════════════════════════════"
        echo ""
        echo "📁 Contenido de target/:"
        ls -lah target/
        echo ""
        echo "📦 JARs creados:"
        find target/ -name "*.jar" -type f -exec ls -lah {} \;
        echo ""
        echo "📊 Tamaño del JAR:"
        du -h target/*.jar 2>/dev/null || echo "No JAR found"
        echo ""
        echo "════════════════════════════════════════════════════════"
        echo "✅ TODO BIEN - Maven funciona correctamente"
        echo "════════════════════════════════════════════════════════"
        echo ""
        echo "Ahora el problema DEBE estar en Cloud Build"
        echo "Verifica que .dockerignore y .gcloudignore excluyan target/"
    else
        echo ""
        echo "════════════════════════════════════════════════════════"
        echo "❌ BUILD FALLÓ"
        echo "════════════════════════════════════════════════════════"
        echo ""
        echo "Maven no pudo compilar el proyecto"
        echo "Revisa los errores arriba"
    fi
else
    echo "❌ Maven NO encontrado"
    echo ""
    echo "Opciones:"
    echo "1. Instalar Maven: brew install maven"
    echo "2. Usar Maven wrapper: ./mvnw clean package -DskipTests"
fi
