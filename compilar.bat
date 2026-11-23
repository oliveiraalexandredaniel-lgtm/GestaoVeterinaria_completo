@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
dir /s /b src\*.java > fontes.tmp
javac -encoding UTF-8 -cp "lib\*" -d bin @fontes.tmp
del fontes.tmp
echo Compilacao finalizada.
