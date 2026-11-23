@echo off
cd /d "%~dp0"
call compilar.bat
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "bin;lib\*" teste.TesteBackend
