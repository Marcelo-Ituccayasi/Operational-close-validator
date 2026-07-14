# Modelo de Dominio

**Versión:** v0.3

**Estado:** Línea base aprobada

**Fase:** 02 — Análisis de dominio

**Producto:** Operational Close Validator

## Propósito

Este documento define los conceptos, relaciones, estados e invariantes centrales del dominio.

El modelo es conceptual. No fija tablas, clases, API, framework ni arquitectura. La frontera transaccional y la persistencia se decidirán durante el diseño técnico.

## Límite del dominio

Operational Close Validator es una capa de validación para eventos operativos registrados. No reemplaza un ERP, POS, sistema contable, facturación electrónica, plataforma bancaria ni gestión completa de inventario.

El producto valida el cierre operativo antes del envío a contabilidad. No realiza el cierre contable.

## Lenguaje del dominio

### Cierre Operativo

Conjunto de eventos registrados para un período que deben validarse, consolidarse y quedar aptos para envío a contabilidad.

### Evento Operativo

Movimiento o acción registrada que puede afectar el cierre.

Tipos incluidos en el MVP:
- Ingreso.
- Egreso.
- Descuento.
- Anulación.

### Evidencia de Soporte

Documento o referencia que respalda un evento, como recibo, boleta o comprobante de pago.

Adjuntar una factura como evidencia no implica implementar gestión de facturas de proveedores.

### Autorización

Aprobación formal y verificable vinculada a un evento que la requiere.

### Regla de Validación

Condición de negocio perteneciente a un catálogo interno fijo.

### Resultado de Validación

Registro de la evaluación de una regla sobre un evento o cierre en un momento determinado.

### Alerta

Representación visible de una inconsistencia o condición pendiente que requiere atención.

### Consolidación

Cálculo y verificación de los totales y saldos del cierre a partir de eventos vigentes.

### Envío a Contabilidad

Transición interna que indica que el cierre operativo validado fue puesto a disposición de contabilidad. El MVP no integra un sistema contable externo.

## Entidades principales

### Cierre Operativo

Responsabilidades:
- identificar el período;
- agrupar eventos operativos;
- mantener el estado del cierre;
- calcular totales y saldos;
- conocer sus alertas bloqueantes;
- controlar la validación final;
- registrar el envío a contabilidad.

Atributos conceptuales:
- identificador;
- período o fecha operativa;
- saldo inicial;
- saldo esperado;
- saldo real;
- totales por tipo de evento;
- estado;
- fecha de consolidación;
- fecha de envío.

### Evento Operativo

Responsabilidades:
- representar un movimiento registrado;
- mantener tipo, monto, fecha, responsable y descripción;
- relacionar evidencia y autorizaciones;
- mantener su estado;
- conservar resultados de validación vigentes.

Atributos conceptuales:
- identificador;
- tipo;
- monto;
- fecha y hora de ocurrencia;
- fecha y hora de registro;
- responsable;
- motivo o descripción;
- estado;
- cierre relacionado;
- indicador de evidencia requerida;
- indicador de autorización requerida.

### Evidencia de Soporte

Responsabilidades:
- respaldar un evento;
- conservar referencia, tipo, monto y fecha;
- permitir verificar presencia y legibilidad.

Atributos conceptuales:
- identificador;
- evento relacionado;
- tipo;
- archivo o referencia;
- monto respaldado;
- fecha;
- estado de legibilidad.

### Autorización

Responsabilidades:
- demostrar que una decisión fue aprobada;
- identificar a la persona responsable;
- conservar motivo, fecha y referencia verificable.

Atributos conceptuales:
- identificador;
- evento relacionado;
- responsable de autorizar;
- motivo;
- fecha y hora;
- referencia formal.

### Regla de Validación

Responsabilidades:
- identificar una condición de negocio;
- declarar alcance, severidad y efecto;
- permanecer versionada dentro del catálogo.

Atributos conceptuales:
- identificador VR;
- nombre;
- descripción;
- alcance;
- severidad;
- efecto de falla;
- vigencia.

En el MVP, las reglas no pueden ser creadas ni configuradas por el usuario.

### Resultado de Validación

Responsabilidades:
- registrar qué regla se evaluó;
- identificar la entidad evaluada;
- conservar resultado, mensaje y momento;
- quedar invalidado cuando cambian datos relevantes.

Atributos conceptuales:
- identificador;
- regla;
- entidad evaluada;
- resultado;
- detalle;
- fecha y hora;
- vigencia.

Resultados posibles:
- Satisfecha.
- Fallida.
- Pendiente, únicamente para el dominio general.

### Alerta

Responsabilidades:
- hacer visible una falla;
- indicar severidad y entidad afectada;
- registrar reconocimiento, revisión, resolución o descarte;
- mantener trazabilidad de la acción tomada.

Atributos conceptuales:
- identificador;
- regla o causa;
- evento o cierre afectado;
- severidad;
- estado;
- detalle;
- fecha de creación;
- responsable de gestión;
- justificación de descarte;
- resultado de revalidación asociado.

### Usuario

Responsabilidades:
- autenticarse;
- registrar y corregir eventos;
- gestionar evidencia, autorizaciones y alertas;
- consolidar y enviar el cierre según permisos.

En el MVP existe un único usuario responsable preconfigurado con autenticación básica. Las funciones de gerencia y contabilidad se simulan dentro del mismo flujo y no representan usuarios independientes.

## Objetos de valor

### Monto

Representa una cantidad monetaria con moneda y precisión definidas.

### Período Operativo

Representa el intervalo al que pertenece un cierre.

### Referencia de Evidencia

Representa la ubicación o identificador verificable de un documento.

### Justificación

Texto obligatorio utilizado cuando una alerta se descarta.

## Relaciones

- Un Cierre Operativo contiene uno o más Eventos Operativos.
- Un Evento Operativo pertenece a un Cierre Operativo.
- Un Evento Operativo puede tener cero o más Evidencias de Soporte.
- Un Evento Operativo puede tener cero o más Autorizaciones.
- Una Regla de Validación genera cero o más Resultados de Validación.
- Un Resultado de Validación evalúa un Evento Operativo o un Cierre Operativo.
- Una validación fallida puede generar una Alerta.
- Una Alerta afecta un Evento Operativo o el Cierre Operativo.
- Una Alerta resuelta debe referenciar una revalidación exitosa.

La implementación definitiva de agregados y fronteras transaccionales se decidirá durante el diseño técnico.

## Estados del Evento Operativo

### Registrado

El evento existe, pero todavía no ha completado todas sus validaciones.

### Pendiente de soporte

Falta evidencia requerida o la evidencia no es legible.

### Pendiente de autorización

Falta una autorización formal requerida.

### Pendiente de conciliación externa

La validación depende de información externa todavía no disponible.

Este estado pertenece al dominio general y queda fuera del MVP v0.3.

### Con observaciones

El evento presenta una inconsistencia de monto, registro o trazabilidad que no se clasifica únicamente como falta de soporte o autorización.

### Validado

Todas las reglas aplicables están satisfechas y no existe una alerta bloqueante activa sobre el evento.

## Estados de la Alerta

### Activa

La inconsistencia fue detectada y todavía no fue atendida.

### Reconocida

El usuario confirmó que conoce la alerta.

### En revisión

La causa está siendo investigada o corregida.

### Resuelta

La causa fue corregida y la entidad afectada superó una revalidación exitosa.

Una alerta no puede marcarse como Resuelta solamente por decisión manual.

### Descartada

La alerta se cerró como no aplicable o aceptable mediante una acción autorizada y una justificación obligatoria.

## Estados del Cierre Operativo

### Preparación

El cierre está recibiendo eventos, evidencia, autorizaciones y correcciones.

### Validado

Todos los Eventos Operativos están Validados, no existen Alertas bloqueantes activas, todos los Resultados de Validación aplicables están vigentes y satisfechos, y la consolidación está completa.

### Provisional

Existen dependencias no bloqueantes controladas, como una conciliación externa pendiente.

Este estado pertenece al dominio completo y queda fuera del MVP v0.3.

### Bloqueado

Existe al menos una condición bloqueante: un Evento Operativo que no superó las validaciones requeridas, un Resultado de Validación obligatorio fallido o no vigente, una Alerta bloqueante activa, o una consolidación incompleta o desactualizada.

### Enviado a contabilidad

El cierre validado superó el control final y fue enviado internamente a contabilidad.

Estados del MVP:
- Preparación.
- Bloqueado.
- Validado.
- Enviado a contabilidad.

## Invariantes del dominio

1. Un Evento Operativo debe pertenecer a un Cierre Operativo.
2. Un Evento Operativo no puede quedar Validado si algún Resultado de Validación aplicable tiene valor Fallida o no está vigente.
3. Un evento que requiere evidencia no puede quedar Validado sin evidencia válida y legible.
4. Un evento que requiere autorización no puede quedar Validado sin una autorización formal vinculada.
5. Una modificación de datos relevantes invalida los resultados de validación anteriores.
6. Un Cierre Operativo no puede quedar Validado si contiene al menos un Evento Operativo cuyo estado no sea Validado.
7. Un Cierre Operativo no puede quedar Validado si existe una alerta bloqueante activa.
8. Un cierre solo puede enviarse a contabilidad desde Validado y después de ejecutar nuevamente la validación final.
9. Si la validación final detecta una inconsistencia crítica, el envío se rechaza y el cierre queda Bloqueado.
10. Una alerta solo puede quedar Resuelta después de una revalidación exitosa.
11. Una alerta Descartada requiere justificación y autorización.
12. Las reglas del MVP pertenecen a un catálogo fijo.
13. El producto no puede garantizar la detección de movimientos reales nunca registrados.

## Servicios conceptuales del dominio

### Validador de Evento

Determina reglas aplicables, genera resultados y produce alertas cuando una condición falla.

### Validador de Cierre

Comprueba estados de eventos, alertas bloqueantes, vigencia de resultados y condiciones de envío.

### Consolidador de Cierre

Calcula totales, saldo esperado, saldo real y diferencias a partir de eventos vigentes.

Estos servicios son conceptos de dominio y no prescriben componentes técnicos.

## Alcance del MVP v0.3

Incluido:
- un usuario responsable preconfigurado;
- autenticación básica;
- ingreso, egreso, descuento y anulación;
- adjuntar evidencia;
- registrar autorización;
- aplicar VR-001, VR-002, VR-003, VR-006 y VR-008;
- generar y gestionar alertas;
- corregir y revalidar;
- consolidar;
- enviar internamente a contabilidad.

Fuera del MVP:
- facturas de proveedores como proceso;
- conciliación bancaria o POS;
- inventario;
- cierre provisional;
- múltiples sucursales;
- configuración dinámica de reglas;
- integración contable real;
- detección autónoma de movimientos nunca registrados.

## Trazabilidad con reglas

- Evento Operativo y Autorización → VR-001 y VR-006.
- Evento Operativo y Evidencia de Soporte → VR-002 y VR-003.
- Dependencia externa → VR-004 y VR-005.
- Consolidación trazable → VR-007.
- Cierre Operativo, Alertas y Resultados → VR-008.

## Conclusión

El núcleo del dominio está formado por Eventos Operativos verificables dentro de un Cierre Operativo. Las reglas producen resultados y alertas; las correcciones requieren revalidación; y el cierre solo puede avanzar cuando las condiciones críticas permanecen satisfechas.