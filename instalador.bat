@echo off
setlocal

REM === Pasta do JDK ===
set JDK=C:\Program Files\Java\jdk-21


set CONSOLE=

REM Se o WiX foi instalado mas o Windows ainda nao o enxerga, tenta achar pela variavel que ele cria
if defined WIX set "PATH=%PATH%;%WIX%bin"

if not exist "target\libs\cantinapdv-1.0-SNAPSHOT.jar" (
    echo ERRO: rode antes o Maven "clean" e "package" no IntelliJ.
    pause
    exit /b 1
)

if not exist "%JDK%\bin\jpackage.exe" (
    echo ERRO: nao achei o jpackage na pasta do JDK.
    echo Ajuste a linha "set JDK=" no inicio deste arquivo para a pasta do seu JDK 21.
    pause
    exit /b 1
)

if not exist "empacotamento\logo.ico" (
    echo ERRO: nao achei o arquivo empacotamento\logo.ico.
    pause
    exit /b 1
)

where candle.exe >nul 2>&1
if errorlevel 1 (
    echo ERRO: o WiX Toolset 3.14 nao foi encontrado.
    echo Instale o WiX 3.14, reinicie o computador e abra o IntelliJ de novo.
    pause
    exit /b 1
)

echo Montando o instalador, isso pode levar alguns minutos...
if exist "target\instalador" rmdir /s /q "target\instalador"

"%JDK%\bin\jpackage.exe" ^
  --type exe ^
  --name CantinaFast ^
  --app-version 1.0.0 ^
  --vendor "IFBA Campus Irece" ^
  --description "CantinaFast - PDV da cantina" ^
  --icon "empacotamento\logo.ico" ^
  --input "target\libs" ^
  --main-jar cantinapdv-1.0-SNAPSHOT.jar ^
  --main-class br.edu.ifba.cantinapdv.Main ^
  --add-modules java.se,jdk.unsupported,jdk.xml.dom ^
  %CONSOLE% ^
  --win-menu ^
  --win-menu-group "CantinaFast" ^
  --win-shortcut ^
  --win-per-user-install ^
  --win-dir-chooser ^
  --win-upgrade-uuid c3f1a8d2-5b7e-4c91-9a64-2e8d0f7b1a35 ^
  --dest "target\instalador"

if errorlevel 1 (
    echo.
    echo ERRO ao montar o instalador. Copie a mensagem acima e envie para ajuda.
    pause
    exit /b 1
)

echo.
echo PRONTO! O instalador esta em: target\instalador
pause