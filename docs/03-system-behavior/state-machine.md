# Máquina de Estados

**Versión:** v0.3

**Estado:** Línea base aprobada

**Fase:** 03 — Comportamiento del sistema

**Producto:** Operational Close Validator

## Propósito

Este documento define los estados, transiciones, condiciones de entrada y restricciones de comportamiento del Cierre Operativo, el Evento Operativo y la Alerta.

La máquina de estados es conceptual. No prescribe framework, patrón de persistencia, API ni implementación técnica.

## Principios de comportamiento

1. Los estados representan condiciones de negocio verificables, no pantallas ni pasos de interfaz.
2. Toda transición debe conservar trazabilidad de quién la ejecutó, cuándo ocurrió y qué condición la autorizó.
3. Una corrección de datos relevantes invalida los resultados de validación que dependan de esos datos.
4. Una Alerta solo queda Resuelta después de una revalidación exitosa de la entidad afectada.
5. El Cierre Operativo solo puede enviarse a contabilidad desde Validado y después de ejecutar nuevamente el control final VR-008.
6. Una falla en el control final rechaza el envío y coloca el cierre en Bloqueado.
7. El estado Provisional y la conciliación externa pertenecen al dominio completo y quedan fuera del MVP v0.3.

## Máquina de estados del Cierre Operativo

### Estados

#### Preparación

Estado inicial. El cierre recibe Eventos Operativos, evidencia, autorizaciones y correcciones. Puede ejecutar validaciones parciales, pero todavía no cumple todas las condiciones para quedar Validado.

#### Validado

Todos los Eventos Operativos están en estado Validado, no existen Alertas bloqueantes activas, todos los Resultados de Validación aplicables están vigentes y satisfechos, y la consolidación está completa.

#### Provisional

Existen dependencias externas no bloqueantes y controladas que impiden considerar el cierre definitivo.

Este estado pertenece al dominio completo y queda fuera del MVP v0.3.

#### Bloqueado

Existe al menos una condición bloqueante: un Evento Operativo que no superó las validaciones requeridas, un Resultado de Validación obligatorio fallido o no vigente, una Alerta bloqueante activa o una consolidación incompleta o desactualizada.

#### Enviado a contabilidad

El cierre superó nuevamente el control final y fue puesto internamente a disposición de contabilidad. El MVP no realiza una integración con un sistema contable externo.

### Estados incluidos en el MVP

- Preparación.
- Bloqueado.
- Validado.
- Enviado a contabilidad.

### Transiciones del Cierre Operativo

#### Preparación → Validado

Condiciones obligatorias:

- todos los Eventos Operativos están Validados;
- no existen Alertas bloqueantes activas;
- todos los Resultados de Validación aplicables están vigentes y satisfechos;
- la consolidación está completa;
- el saldo esperado, el saldo real y las diferencias fueron calculados con eventos vigentes.

Acción asociada:

- registrar la consolidación y el resultado de validación del cierre.

#### Preparación → Bloqueado

Se produce cuando:

- una regla crítica falla;
- existe un Evento Operativo pendiente o con observaciones;
- aparece una Alerta bloqueante;
- un resultado obligatorio queda no vigente;
- la consolidación no puede completarse.

#### Preparación → Provisional

Transición del dominio completo, fuera del MVP.

Se produce cuando existen dependencias externas pendientes que son controladas y no se clasifican como bloqueantes críticas.

#### Provisional → Validado

Transición del dominio completo, fuera del MVP.

Condiciones:

- las dependencias externas fueron resueltas;
- las validaciones afectadas se ejecutaron nuevamente;
- no existen Alertas bloqueantes;
- la consolidación está completa.

#### Bloqueado → Validado

Condiciones obligatorias:

- se corrigió la causa de cada condición bloqueante;
- las Alertas afectadas fueron Resueltas mediante revalidación exitosa o Descartadas con autorización y justificación;
- se ejecutaron nuevamente las validaciones afectadas;
- todos los Eventos Operativos están Validados;
- todos los resultados aplicables están vigentes y satisfechos;
- la consolidación fue ejecutada nuevamente.

No existe una transición automática por el simple reconocimiento de una Alerta.

#### Validado → Enviado a contabilidad

Condiciones obligatorias:

- el cierre se encuentra en Validado;
- VR-008 se ejecuta inmediatamente antes del envío;
- todos los Eventos Operativos continúan Validados;
- no existen Alertas bloqueantes activas;
- todos los resultados aplicables continúan vigentes y satisfechos;
- la consolidación continúa completa y vigente.

Acción asociada:

- registrar fecha, hora y responsable del envío interno.

#### Validado → Bloqueado

Se produce cuando el control final previo al envío detecta:

- un Evento Operativo cuyo estado ya no es Validado;
- un Resultado de Validación fallido o no vigente;
- una Alerta bloqueante activa;
- una consolidación incompleta o desactualizada;
- cualquier inconsistencia crítica que impida VR-008.

Efecto:

- el envío se rechaza;
- el cierre queda Bloqueado;
- se conserva trazabilidad de la causa;
- se requiere corrección, revalidación y nueva consolidación.

Esta transición evita que el estado Validado se interprete como irreversible antes del envío.

### Transiciones no permitidas del Cierre Operativo

- Preparación → Enviado a contabilidad.
- Bloqueado → Enviado a contabilidad.
- Provisional → Enviado a contabilidad.
- Enviado a contabilidad → Validado dentro del MVP.
- Enviado a contabilidad → Preparación dentro del MVP.

Una corrección posterior al envío requeriría un flujo de reapertura o rectificación fuera del alcance del MVP.

## Máquina de estados del Evento Operativo

### Estados

#### Registrado

El evento existe y pertenece a un Cierre Operativo, pero todavía no ha completado todas sus validaciones aplicables.

#### Pendiente de soporte

Falta una Evidencia de Soporte requerida o la evidencia no es válida o legible.

#### Pendiente de autorización

Falta una Autorización formal requerida.

#### Pendiente de conciliación externa

La validación depende de información externa todavía no disponible.

Este estado pertenece al dominio completo y queda fuera del MVP v0.3.

#### Con observaciones

Existe una inconsistencia de monto, registro o trazabilidad que no se clasifica únicamente como falta de soporte o autorización.

El término “Observado” se reserva para la clasificación de evidencia de Product Discovery [descubrimiento de producto].

#### Validado

Todas las reglas aplicables están satisfechas, los resultados están vigentes y no existe una Alerta bloqueante activa sobre el evento.

### Transiciones del Evento Operativo

#### Registrado → Validado

Se produce cuando todas las reglas aplicables quedan Satisfechas y no se genera una Alerta bloqueante.

#### Registrado → Pendiente de soporte

Se produce cuando VR-003 u otra regla aplicable detecta evidencia requerida ausente, inválida o ilegible.

#### Registrado → Pendiente de autorización

Se produce cuando VR-001 o VR-006 detecta una autorización formal requerida ausente.

#### Registrado → Con observaciones

Se produce cuando VR-001 o VR-002 detecta una inconsistencia de registro, monto o trazabilidad.

#### Registrado → Pendiente de conciliación externa

Transición del dominio completo, fuera del MVP. Se produce cuando una validación depende de información externa todavía no disponible.

#### Pendiente de soporte → Validado

Condiciones:

- se adjuntó o corrigió la evidencia requerida;
- la evidencia es válida y legible;
- las reglas afectadas se ejecutaron nuevamente;
- todas las reglas aplicables quedaron satisfechas;
- no existe una Alerta bloqueante activa.

#### Pendiente de autorización → Validado

Condiciones:

- se registró una Autorización formal y verificable;
- las reglas afectadas se ejecutaron nuevamente;
- todas las reglas aplicables quedaron satisfechas;
- no existe una Alerta bloqueante activa.

#### Con observaciones → Validado

Condiciones:

- se corrigió la inconsistencia;
- se invalidaron los resultados anteriores dependientes;
- se ejecutaron nuevamente las reglas afectadas;
- todas las reglas aplicables quedaron satisfechas;
- no existe una Alerta bloqueante activa.

#### Revalidación que mantiene un estado no validado

Cuando una corrección no resuelve todas las fallas, la revalidación puede cambiar el Evento Operativo entre Registrado, Pendiente de soporte, Pendiente de autorización y Con observaciones, según las reglas que continúen fallando.

Una revalidación no fuerza la transición a Validado.

#### Validado → Registrado

Cuando cambian datos relevantes, evidencia o autorizaciones:

1. los Resultados de Validación dependientes quedan no vigentes;
2. el Evento Operativo pasa a Registrado porque debe ejecutar nuevamente sus validaciones aplicables;
3. si el Cierre Operativo relacionado estaba Validado, vuelve a evaluarse y pasa a Bloqueado;
4. una nueva validación determina si el evento pasa a Validado, Pendiente de soporte, Pendiente de autorización, Con observaciones o, fuera del MVP, Pendiente de conciliación externa.

## Máquina de estados de la Alerta

### Estados

#### Activa

La inconsistencia fue detectada y todavía no fue atendida.

#### Reconocida

El usuario confirmó que conoce la Alerta. Reconocerla no corrige la causa ni elimina su efecto bloqueante.

#### En revisión

La causa está siendo investigada o corregida.

#### Resuelta

La causa fue corregida y la entidad afectada superó una revalidación exitosa.

Una Alerta no puede marcarse manualmente como Resuelta sin un Resultado de Validación exitoso asociado.

#### Descartada

La Alerta se cerró como no aplicable o aceptable mediante una acción autorizada y una justificación obligatoria.

### Transiciones de la Alerta

#### Activa → Reconocida

El usuario confirma que conoce la Alerta. La Alerta conserva su severidad y efecto bloqueante.

#### Activa o Reconocida → En revisión

El usuario inicia el análisis o la corrección de la causa.

#### Activa, Reconocida o En revisión → Resuelta

Solo se permite cuando:

- la causa fue corregida;
- la entidad afectada fue revalidada;
- la regla que originó la Alerta quedó Satisfecha;
- existe un Resultado de Validación vigente asociado.

#### Activa, Reconocida o En revisión → Descartada

Solo se permite cuando:

- el usuario está autorizado para descartar;
- registra una justificación obligatoria;
- queda trazabilidad de fecha, responsable y motivo.

Descartar una Alerta no modifica por sí solo el estado del Evento Operativo ni garantiza que el Cierre Operativo pueda avanzar. Las demás reglas e invariantes continúan aplicándose.

### Estados terminales de la Alerta

- Resuelta.
- Descartada.

Una nueva inconsistencia posterior genera una nueva Alerta o una nueva ocurrencia trazable; el MVP no define reapertura automática de una Alerta terminal.

## Coordinación entre máquinas de estados

1. Una falla de validación de un Evento Operativo puede cambiar su estado y generar una Alerta.
2. Un Evento Operativo no validado o una Alerta bloqueante activa impiden que el Cierre Operativo quede Validado.
3. Resolver una Alerta requiere revalidar la entidad afectada; la revalidación puede cambiar el estado del Evento Operativo.
4. Cuando todos los Eventos Operativos quedan Validados y no existen Alertas bloqueantes, el cierre puede consolidarse y pasar a Validado.
5. VR-008 vuelve a evaluar el cierre inmediatamente antes del envío.
6. Si VR-008 falla, el cierre pasa de Validado a Bloqueado y el envío se rechaza.

## Invariantes de comportamiento

1. Todo Evento Operativo pertenece a un Cierre Operativo.
2. Un Evento Operativo no puede quedar Validado si algún Resultado de Validación aplicable tiene valor Fallida o no está vigente.
3. Un Cierre Operativo no puede quedar Validado si contiene al menos un Evento Operativo cuyo estado no sea Validado.
4. Un Cierre Operativo no puede quedar Validado si existe una Alerta bloqueante activa.
5. Una corrección invalida los resultados dependientes antes de ejecutar una nueva validación.
6. Una Alerta solo queda Resuelta después de una revalidación exitosa.
7. Una Alerta Descartada requiere autorización y justificación.
8. Un cierre solo puede enviarse a contabilidad desde Validado.
9. VR-008 debe ejecutarse nuevamente inmediatamente antes del envío.
10. Una falla de VR-008 rechaza el envío y coloca el cierre en Bloqueado.
11. Enviado a contabilidad es terminal dentro del MVP.
12. El producto no garantiza la detección de movimientos reales nunca registrados.

## Trazabilidad

- Estados de Evento Operativo: Domain Model v0.3 [modelo de dominio].
- Estados de Alerta: Domain Model v0.3 [modelo de dominio].
- Estados del Cierre Operativo: Domain Model v0.3 [modelo de dominio].
- Condiciones de validación: Validation Rules v0.2 [reglas de validación].
- Control final: VR-008 y FM-008.
- Transición Validado → Bloqueado: decisión asociada ADR-0001 — La validación final puede devolver un cierre validado a bloqueado.

## Conclusión

El comportamiento aprobado exige validación explícita, revalidación después de cada corrección y un control final obligatorio antes del envío. El cierre no avanza por decisión manual: avanza únicamente cuando los estados, resultados, alertas y consolidación cumplen simultáneamente las condiciones del dominio.