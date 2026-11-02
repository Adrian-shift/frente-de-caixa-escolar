@echo off
setlocal

REM === Ajuste aqui se o seu JDK 21 estiver em outra pasta ===
set JDK=C:\Users\User\.jdks\ms-21.0.12.1

if not exist "target\libs\cantinapdv-1.0-SNAPSHOT.jar" (
    echo ERRO: rode antes o Maven "clean" e "package" no IntelliJ.
    pause
    exit /b 1
)

echo Montando o pacote...
if exist "target\pacote" rmdir /s /q "target\pacote"

"%JDK%\bin\jpackage.exe" ^
  --type app-image ^
  --name CantinaFast ^
  --app-version 1.0.0 ^
  --input "target\libs" ^
  --main-jar cantinapdv-1.0-SNAPSHOT.jar ^
  --main-class br.edu.ifba.cantinapdv.Main ^
  --add-modules java.se,jdk.unsupported,jdk.xml.dom ^
  --dest "target\pacote" ^
  --win-console

if errorlevel 1 (
    echo.
    echo ERRO ao montar o pacote. Copie a mensagem acima e envie para ajuda.
    pause
    exit /b 1
)

echo.
echo PRONTO! O programa esta em: target\pacote\CantinaFast
pause