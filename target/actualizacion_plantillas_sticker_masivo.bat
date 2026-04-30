@echo off
setlocal

:: Paso 1: Descargar los archivos desde las URLs
echo "Descargando archivos..."

:: Usamos curl para descargar los archivos
curl -k -o imprimirStickerMasivo.vm https://olva.pe/OlvaCorp/imprimirStickerMasivo.vm

echo "Archivo descargado."

:: Paso 2: Copiar los archivos especificados a ~/.Oryx
echo "Copiando archivos a ~/.Oryx..."

:: Nota: En sistemas Windows, ~ no es compatible con la variable de entorno del usuario
:: por lo que usamos la ruta completa del usuario
set USERPROFILE=%USERPROFILE%

:: Copiar los archivos forzosamente
xcopy /Y imprimirStickerMasivo.vm "%USERPROFILE%\.Oryx\"

echo "Archivos copiados correctamente."

:: Finalizar
endlocal