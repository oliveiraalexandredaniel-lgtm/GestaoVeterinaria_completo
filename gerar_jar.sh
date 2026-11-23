#!/bin/bash
# Gera GestaoVeterinaria.jar (o driver continua em lib/, ao lado do jar)
cd "$(dirname "$0")"
./compilar.sh || exit 1
JARS=$(ls lib/*.jar 2>/dev/null | tr '\n' ' ')
printf "Main-Class: principal.Main\nClass-Path: %s\n" "$JARS" > manifest.tmp
jar cfm GestaoVeterinaria.jar manifest.tmp -C bin . && rm manifest.tmp && echo "Gerado GestaoVeterinaria.jar"
