#!/bin/bash

echo "===== Testando endpoint /api/v1/media ====="
echo ""
echo "Aguardando aplicação iniciar..."
sleep 3

echo "Fazendo requisição..."
RESPONSE=$(curl -s http://localhost:8080/api/v1/media?page=0&size=1)

echo "Resposta completa:"
echo "$RESPONSE" | jq '.'

echo ""
echo "===== Verificando primeiro item ====="
FIRST_ITEM=$(echo "$RESPONSE" | jq '.content[0]')
echo "$FIRST_ITEM"

echo ""
echo "===== Verificando se tem dados ====="
ID=$(echo "$FIRST_ITEM" | jq -r '.id')
TITLE=$(echo "$FIRST_ITEM" | jq -r '.title')

if [ "$ID" != "null" ] && [ "$TITLE" != "null" ]; then
    echo "✅ SUCCESS! Dados estão sendo retornados corretamente!"
    echo "   ID: $ID"
    echo "   Title: $TITLE"
else
    echo "❌ FAIL! Ainda retornando null"
fi
