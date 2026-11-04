@echo off
setlocal EnableDelayedExpansion

echo =======================================================
echo        GERANDO INSTALADOR CANTINAFAST (WINDOWS)
echo =======================================================

REM 1. Configura Java e inclui a pasta BIN do WiX v3.14
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
)

set "WIX_BIN=C:\Program Files (x86)\WiX Toolset v3.14\bin"
set "PATH=%JAVA_HOME%\bin;%WIX_BIN%;C:\Program Files (x86)\WiX Toolset v3.11\bin;%PATH%"

REM 2. Testa se o candle.exe foi encontrado
where candle.exe >nul 2>&1
if ERRORLEVEL 1 (
    echo.
    echo [ERRO] candle.exe nao foi encontrado em "%WIX_BIN%".
    echo Verifique se os arquivos existem dentro da pasta bin.
    echo.
    pause
    exit /b 1
)

echo [OK] WiX 3.14 detectado em:
where candle.exe

REM 3. Variaveis da aplicacao
set "APP_NAME=CantinaFast"
set "APP_VERSION=1.0.0"
set "MAIN_CLASS=br.edu.ifba.cantinapdv.Main"
set "INPUT_DIR=target\libs"
set "OUTPUT_DIR=target\dist"
set "ICON_PATH=src\main\resources\br\edu\ifba\cantinapdv\icones\logo.ico"

if exist "%OUTPUT_DIR%" rd /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

echo.
echo [1/2] Compilando com Maven...
call mvnw.cmd clean package -DskipTests
if ERRORLEVEL 1 (
    echo Erro ao compilar com o Maven.
    pause
    exit /b 1
)

if exist "target\*.jar" (
    copy /Y "target\*.jar" "%INPUT_DIR%\" >nul 2>&1
)

echo.
echo [2/2] Gerando executavel (.exe) via jpackage...

set "MAIN_JAR="
for %%F in (%INPUT_DIR%\cantinapdv*.jar %INPUT_DIR%\CantinaFast*.jar) do (
    set "MAIN_JAR=%%~nxF"
)

if "%MAIN_JAR%"=="" (
    echo [ERRO] JAR principal nao encontrado em %INPUT_DIR%.
    pause
    exit /b 1
)

echo Jar principal: %MAIN_JAR%

set "ICON_PARAM="
if exist "%ICON_PATH%" (
    set ICON_PARAM=--icon "%ICON_PATH%"
)

jpackage ^
  --type exe ^
  --input "%INPUT_DIR%" ^
  --dest "%OUTPUT_DIR%" ^
  --name "%APP_NAME%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class %MAIN_CLASS% ^
  --app-version %APP_VERSION% ^
  --vendor "IFBA Campus Irece" ^
  --description "Frente de Caixa (PDV) da Cantina IFBA" ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "CantinaFast" ^
  --win-dir-chooser ^
  %ICON_PARAM% ^
  --java-options "-Dfile.encoding=UTF-8"

if ERRORLEVEL 1 (
    echo.
    echo [ERRO] Ocorreu uma falha no jpackage.
    pause
    exit /b 1
)

echo.
echo =======================================================
echo  Instalador gerado com sucesso!
echo  Arquivo: %CD%\%OUTPUT_DIR%\%APP_NAME%-%APP_VERSION%.exe
echo =======================================================
pause