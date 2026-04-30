# Informe tecnico - Producto Academico N. 3

## 1. Resumen del proyecto

El proyecto `olva-integration-hub` integra servicios web de Ripley para consultar informacion de pedidos, registrar informacion en un sistema local y publicar eventos de trazabilidad hacia servicios externos de Ripley y Serhafen. La aplicacion esta construida con Spring Boot, tareas programadas Quartz, clientes HTTP con `RestTemplate`, repositorios para Oracle y pruebas unitarias con JUnit 5, AssertJ y Mockito.

Para cumplir el Producto Academico N. 3 se adecuo el proyecto con evidencias concretas de pruebas unitarias, enfoque TDD, Katas TDD y ORM. La adecuacion se realizo de forma acotada para no alterar el flujo productivo actual de integracion, pero dejando clases ejecutables y probadas dentro del repositorio.

## 2. Objetivo

Aplicar pruebas unitarias y TDD sobre reglas relevantes del dominio de integracion: validacion de eventos, normalizacion de datos enviados a Ripley, decision de reintentos y persistencia local de eventos creados.

## 3. Pruebas unitarias implementadas

El proyecto ya contaba con pruebas sobre el despacho de notificaciones, job Quartz y gateway Serhafen. Se agregaron pruebas adicionales en:

- `src/test/java/com/olva/academic/kata/RipleyEventKataTest.java`
- `src/test/java/com/olva/academic/kata/RipleyRetryKataTest.java`
- `src/test/java/com/olva/academic/orm/MockRipleyEventOrmServiceTest.java`

Estas pruebas validan multiples escenarios: datos validos, datos obligatorios faltantes, normalizacion de CUD, eliminacion de duplicados, reintentos por error temporal, fallo al alcanzar el limite de intentos y persistencia mediante repositorio ORM mockeado.

## 4. Aplicacion de TDD

Se uso el ciclo Red-Green-Refactor en las clases academicas agregadas:

1. Red: se escribieron pruebas que describen el comportamiento esperado antes de implementar la logica.
2. Green: se implemento el codigo minimo en `RipleyEventKata`, `RipleyRetryKata` y `MockRipleyEventOrmService`.
3. Refactor: se separaron responsabilidades en records de salida (`RipleyEventSummary`, `RetryDecision`) y entidades ORM (`MockRipleyOutboxEntity`, `MockRipleyEventAuditEntity`).

El enfoque permite que las reglas del negocio sean verificables sin depender de los servicios reales de Ripley ni de una base de datos Oracle disponible.

## 5. Katas TDD

Se aplicaron dos Katas TDD vinculadas al dominio:

- Kata 1: validacion y resumen de eventos Ripley. Problema: normalizar el codigo de evento y los CUD recibidos, evitando duplicados y rechazando datos incompletos.
- Kata 2: decision de reintento. Problema: decidir si un evento queda enviado, reintenta o falla segun el resultado HTTP y el numero de intentos.

Cada kata tiene pruebas unitarias independientes, entradas controladas y resultados esperados. Esto permite mejorar la solucion por iteraciones sin impactar directamente las integraciones externas.

## 6. Uso de ORM

Se incorporo `spring-boot-starter-data-jpa` y se agregaron dos clases mock persistibles:

- `MockRipleyOutboxEntity`: representa un evento local pendiente de publicacion.
- `MockRipleyEventAuditEntity`: representa la auditoria del evento y su relacion con el outbox.

Tambien se agrego `MockRipleyOutboxJpaRepository`, basado en Spring Data JPA, y `MockRipleyEventOrmService`, que registra eventos con una relacion uno-a-muchos entre outbox y auditorias. La prueba `MockRipleyEventOrmServiceTest` usa Mockito para validar la interaccion con el repositorio sin requerir una base de datos real.

## 7. Integracion en flujo agil

Las pruebas se integran al flujo Maven del proyecto mediante:

```bash
./mvnw test
```

En un flujo de equipo, este comando debe ejecutarse antes de cada commit y en GitHub Actions o cualquier pipeline CI. La evidencia esperada para la entrega es el historial de commits, el resultado de pruebas y la documentacion de las iteraciones TDD.

## 8. Relacion con la rubrica

- Diseno e implementacion de pruebas unitarias: se cubren escenarios exitosos y alternos con JUnit, AssertJ y Mockito.
- Aplicacion TDD: las clases nuevas fueron disenadas desde pruebas y documentadas bajo Red-Green-Refactor.
- Katas TDD: se agregaron katas de validacion de eventos y decision de reintentos.
- ORM: se agregaron entidades JPA, repositorio Spring Data y servicio transaccional.
- Flujo agil: las pruebas quedan ejecutables con Maven y listas para CI.

## 9. Conclusiones

La adecuacion permite demostrar practicas de calidad sin reescribir toda la integracion existente. Las pruebas unitarias reducen el riesgo en reglas criticas, las katas evidencian aprendizaje iterativo y el modulo ORM mock muestra como gestionar datos relacionales mediante entidades y repositorios JPA.
