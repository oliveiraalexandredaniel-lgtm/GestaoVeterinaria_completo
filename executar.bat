@echo off
cd /d "%~dp0"
if not exist bin call compilar.bat
java -Dfile.encoding=UTF-8 -cp "bin;lib\*" principal.Main
