#!/bin/bash
# Compila todo o projeto para a pasta bin/ (precisa do JDK 17+ e do driver em lib/)
cd "$(dirname "$0")"
mkdir -p bin
javac -encoding UTF-8 -cp "lib/*" -d bin $(find src -name "*.java") && echo "Compilado com sucesso em bin/"
