@echo off
cd /d "%~dp0"
java -Xms2G -Xmx6G -jar dxdcraft-server-launcher.jar %*
pause
