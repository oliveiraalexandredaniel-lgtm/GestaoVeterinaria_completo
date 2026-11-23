#!/bin/bash
cd "$(dirname "$0")"
[ -d bin ] || ./compilar.sh || exit 1
java -Dfile.encoding=UTF-8 -cp "bin:lib/*" principal.Main
