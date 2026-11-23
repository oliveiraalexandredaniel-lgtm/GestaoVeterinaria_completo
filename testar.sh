#!/bin/bash
# Testes do backend. ATENCAO: rode Script.sql antes (banco limpo).
cd "$(dirname "$0")"
./compilar.sh || exit 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "bin:lib/*" teste.TesteBackend
