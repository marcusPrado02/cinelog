#!/bin/bash

# Script para adicionar métricas em todos os controllers do CineLog
# Automatiza a Fase 1 da implementação de observabilidade

echo "🚀 Iniciando implementação de métricas nos controllers..."

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Contadores
TOTAL_FILES=0
SUCCESS_FILES=0

# Função para adicionar import se não existir
add_import_if_missing() {
    local file=$1
    local import_line=$2
    
    if ! grep -q "$import_line" "$file"; then
        # Adiciona após os outros imports
        sed -i "/^import.*observability\.aop/a $import_line" "$file"
        return 0
    fi
    return 1
}

# Função para adicionar BusinessMetricsService no construtor
add_metrics_service() {
    local file=$1
    echo "  Processando: $(basename $file)"
    
    # Verifica se já tem BusinessMetricsService
    if grep -q "BusinessMetricsService" "$file"; then
        echo "    ⏭️  Já possui BusinessMetricsService"
        return 1
    fi
    
    # Adiciona import
    add_import_if_missing "$file" "import com.cine.cinelog.shared.observability.metrics.BusinessMetricsService;"
    
    echo "    ✅ Import adicionado"
    return 0
}

# Lista de controllers para processar
CONTROLLERS=(
    "src/main/java/com/cine/cinelog/features/genres/web/controller/GenreController.java"
    "src/main/java/com/cine/cinelog/features/seasons/web/controller/SeasonController.java"
    "src/main/java/com/cine/cinelog/features/episodes/web/controller/EpisodeController.java"
    "src/main/java/com/cine/cinelog/features/watchentry/web/controller/WatchEntryController.java"
    "src/main/java/com/cine/cinelog/features/watchlist/web/controller/WatchlistController.java"
    "src/main/java/com/cine/cinelog/features/credits/web/controller/CreditController.java"
    "src/main/java/com/cine/cinelog/features/people/web/controller/PersonController.java"
    "src/main/java/com/cine/cinelog/features/users/web/controller/UserController.java"
)

echo ""
echo "📝 Controllers a processar: ${#CONTROLLERS[@]}"
echo ""

for controller in "${CONTROLLERS[@]}"; do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    
    if [ -f "$controller" ]; then
        if add_metrics_service "$controller"; then
            SUCCESS_FILES=$((SUCCESS_FILES + 1))
        fi
    else
        echo "  ⚠️  Arquivo não encontrado: $controller"
    fi
    echo ""
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Resumo:"
echo "  Total de arquivos: $TOTAL_FILES"
echo "  Atualizados com sucesso: $SUCCESS_FILES"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "⚠️  ATENÇÃO:"
echo "  1. Os imports foram adicionados"
echo "  2. Você precisa adicionar manualmente:"
echo "     - O campo 'private final BusinessMetricsService metricsService;'"
echo "     - O parâmetro no construtor"
echo "     - As chamadas metricsService.incrementXXX() nos métodos"
echo ""
echo "📖 Consulte o guia: docs/OBSERVABILITY_IMPLEMENTATION_GUIDE.md"
echo ""
echo "✅ Script concluído!"
