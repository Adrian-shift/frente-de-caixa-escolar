@echo off
setlocal

REM === Ajuste aqui se o seu JDK 21 estiver em outra pasta ===
set JDK=C:\Program Files\Java\jdk-21

REM === Janela preta com as mensagens do programa (util para TESTES) ===
REM Para a versao FINAL, deixe a linha abaixo assim: set CONSOLE=
set CONSOLE=--win-console

if not exist "target\libs\cantinapdv-1.0-SNAPSHOT.jar" (
    echo ERRO: rode antes o Maven "clean" e "package" no IntelliJ.
    pause
    exit /b 1
)

if not exist "%JDK%\bin\jpackage.exe" (
    echo ERRO: nao achei o jpackage em "%JDK%\bin".
    echo Ajuste a linha "set JDK=" no inicio deste arquivo para a pasta do seu JDK 21.
    pause
    exit /b 1
)

echo Montando o pacote...
if exist "target\pacote" rmdir /s /q "target\pacote"

"%JDK%\bin\jpackage.exe" ^
  --type app-image ^
  --name CantinaFast ^
  --app-version 1.0.0 ^
  --vendor "IFBA Campus Irece" ^
  --description "CantinaFast - PDV da cantina" ^
  --input "target\libs" ^
  --main-jar cantinapdv-1.0-SNAPSHOT.jar ^
  --main-class br.edu.ifba.cantinapdv.Main ^
  --add-modules java.se,jdk.unsupported,jdk.xml.dom ^
  %CONSOLE% ^
  --dest "target\pacote"

if errorlevel 1 (
    echo.
    echo ERRO ao montar o pacote. Copie a mensagem acima e envie para ajuda.
    pause
    exit /b 1
)

echo.
echo PRONTO! O programa esta em: target\pacote\CantinaFast
pause