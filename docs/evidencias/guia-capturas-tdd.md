# Guia de capturas para evidenciar TDD

## Captura 1 - Red: prueba fallando

Abrir el archivo:

`docs/evidencias/01-red-prueba-fallando.txt`

Capturar la seccion donde se observe:

- `Failures: 1`
- La prueba `shouldNormalizeEventCodeAndRemoveDuplicatedCuds`
- El error de asercion donde se esperaba `rp-created-red` pero se obtuvo `rp-created`

Esta captura evidencia la fase Red del ciclo TDD.

## Captura 2 - Green: prueba pasando

Abrir el archivo:

`docs/evidencias/02-green-prueba-pasando.txt`

Capturar la seccion donde se observe:

- `Tests run: 2`
- `Failures: 0`
- `BUILD SUCCESS`

Esta captura evidencia la fase Green despues de corregir la prueba/implementacion esperada.

## Captura 3 - Suite completa del proyecto

Abrir el archivo:

`docs/evidencias/03-suite-completa-pasando.txt`

Capturar la seccion final donde se observe:

- `Tests run: 15`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

Esta captura evidencia la integracion de pruebas unitarias al flujo del proyecto.

## Comandos ejecutados

```bash
./mvnw.cmd -Dtest=RipleyEventKataTest test
./mvnw.cmd test
```

