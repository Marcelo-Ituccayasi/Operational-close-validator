# ADR-0001: La validación final puede devolver un cierre validado a bloqueado

**Estado:** Aceptada
**Estado documental:** Pendiente de incorporación y congelamiento en GitHub
**Fecha:** 2026-07-14
**Producto:** Operational Close Validator

## Contexto

Operational Close Validator valida Eventos Operativos antes de que un Cierre Operativo sea puesto a disposición de contabilidad.

Un cierre pasa a Validado después de que todos sus Eventos Operativos están Validados, no existen Alertas bloqueantes activas, todos los Resultados de Validación aplicables están vigentes y satisfechos, y la consolidación está completa.

Sin embargo, Validado no puede interpretarse como un estado irreversible antes del envío. Entre la consolidación y el intento de envío pueden ocurrir cambios relevantes:

- modificación de datos de un Evento Operativo;
- reemplazo o modificación de Evidencia de Soporte;
- modificación de una Autorización;
- aparición o reactivación lógica de una condición bloqueante;
- invalidación de Resultados de Validación;
- invalidación o desactualización de la consolidación.

Por esta razón, VR-008 debe ejecutarse inmediatamente antes del envío.

## Problema de decisión

Debe definirse qué ocurre cuando un cierre se encuentra en Validado, pero el control final detecta que ya no cumple las condiciones obligatorias para el envío.

Tratar Validado como irreversible produciría una de estas consecuencias incorrectas:

- permitir el envío usando resultados o consolidaciones desactualizados;
- ocultar la falla mediante una excepción manual;
- regresar el cierre a un estado ambiguo que no exprese la existencia de una condición bloqueante;
- perder trazabilidad sobre la causa del rechazo.

## Fuerzas de decisión

- El envío solo debe ocurrir con información vigente y verificable.
- El estado del cierre debe representar su condición real de negocio.
- El rechazo debe conservar causa, fecha, responsable y entidades afectadas.
- La recuperación debe utilizar el mismo flujo aprobado de corrección, revalidación y consolidación.
- La decisión no debe introducir reapertura de cierres ya enviados.
- La decisión debe ser coherente con VR-008, FM-008 y el alcance del MVP v0.3.

## Decisión

Se acepta la transición **Validado → Bloqueado** cuando VR-008 falla inmediatamente antes del envío.

### Condiciones que provocan la transición

La transición ocurre cuando el control final detecta al menos una de estas condiciones:

- un Evento Operativo cuyo estado no sea Validado;
- una Alerta bloqueante activa;
- un Resultado de Validación aplicable con valor Fallida;
- un Resultado de Validación aplicable no vigente;
- una consolidación incompleta;
- una consolidación desactualizada o no vigente;
- cualquier inconsistencia crítica que impida satisfacer VR-008.

### Efectos obligatorios

Cuando VR-008 falla:

1. el envío se rechaza;
2. VR-008 queda Fallida;
3. el Cierre Operativo pasa de Validado a Bloqueado;
4. se conserva la causa del rechazo y las entidades afectadas;
5. se conservan fecha, hora y usuario responsable del intento;
6. no se registra un envío exitoso;
7. el cierre requiere corrección, revalidación y nueva consolidación cuando corresponda.

### Envío exitoso

La transición **Validado → Enviado a contabilidad** solo se permite cuando VR-008 se ejecuta inmediatamente antes del envío y queda Satisfecha.

Enviado a contabilidad continúa siendo un estado terminal dentro del MVP. La reapertura, rectificación o anulación de un cierre enviado queda fuera del alcance.

## Restricción de consistencia

La ejecución de VR-008, el rechazo o aceptación del envío y la transición resultante del cierre constituyen una única operación de negocio.

Technical Design [diseño técnico] deberá seleccionar el mecanismo transaccional y de concurrencia que garantice que no exista un envío confirmado con resultados, Alertas o consolidación diferentes de los evaluados por VR-008.

Esta ADR no prescribe framework, base de datos, protocolo, patrón arquitectónico ni implementación concreta.

## Recuperación después del bloqueo

Un cierre que volvió a Bloqueado solo puede regresar a Validado cuando:

- se corrigieron las causas bloqueantes;
- los Eventos Operativos afectados fueron revalidados;
- todos los Resultados de Validación aplicables están vigentes y satisfechos;
- las Alertas afectadas fueron Resueltas mediante revalidación exitosa o Descartadas mediante una acción autorizada y justificada;
- no existen Alertas bloqueantes activas;
- la consolidación fue ejecutada nuevamente cuando quedó invalidada;
- todas las condiciones del estado Validado vuelven a cumplirse.

Reconocer una Alerta no permite la transición a Validado.

## Consecuencias positivas

- Evita envíos basados en información desactualizada.
- Mantiene alineados el estado del cierre y su condición verificable.
- Hace explícita la diferencia entre un cierre validado previamente y un cierre apto para envío en el momento actual.
- Conserva trazabilidad del rechazo final.
- Reutiliza el flujo normal de corrección y revalidación.
- Permite definir pruebas deterministas para la compuerta final.

## Consecuencias y costos

- Validado deja de ser un estado irreversible antes del envío.
- La implementación debe volver a evaluar las condiciones del cierre inmediatamente antes de enviarlo.
- Los Resultados de Validación y la consolidación deben conservar información de vigencia.
- El sistema debe manejar concurrencia entre modificaciones y solicitudes de envío.
- Las pruebas deben cubrir el cambio Validado → Bloqueado y no solo el envío exitoso.

## Alternativas descartadas

### Alternativa 1 — Considerar Validado como irreversible

**Descartada** porque permitiría confiar en una validación histórica aunque los datos o resultados hayan cambiado.

### Alternativa 2 — Permitir el envío con una advertencia

**Descartada** porque contradice el carácter bloqueante de VR-008 y el objetivo central del producto.

### Alternativa 3 — Regresar el cierre a Preparación

**Descartada** como respuesta principal porque Preparación no expresa de forma suficiente que existe una condición crítica que impide avanzar. Bloqueado comunica explícitamente la restricción y conserva el flujo de recuperación aprobado.

### Alternativa 4 — Crear un estado adicional de error de envío

**Descartada** para el MVP porque Bloqueado ya representa la condición de negocio necesaria. Un estado técnico adicional ampliaría el alcance sin aportar una distinción funcional aprobada.

## Criterios de cumplimiento

La decisión se considera correctamente implementada cuando:

1. ningún cierre se envía sin ejecutar VR-008 inmediatamente antes;
2. una falla de VR-008 impide registrar el envío;
3. la falla cambia el cierre de Validado a Bloqueado;
4. se conserva la causa del rechazo;
5. el cierre no vuelve a Validado sin corrección, revalidación y consolidación vigentes;
6. las pruebas automatizadas cubren cada condición bloqueante de VR-008;
7. un cierre Enviado a contabilidad no se modifica dentro del MVP.

## Documentos relacionados

- Failure Mode Analysis v0.1 [análisis de modos de falla]: FM-008.
- Validation Rules v0.2 [reglas de validación]: VR-008.
- Domain Model v0.3 [modelo de dominio]: estados e invariantes del Cierre Operativo.
- State Machine v0.3 [máquina de estados]: transiciones Validado → Bloqueado y Validado → Enviado a contabilidad.
- Use Cases v0.2 [casos de uso]: UC-008 y UC-009.
- MVP Scope v0.3 [alcance del MVP]: control final, escenarios E y F y criterios transversales.

## Control de cambios

Modificar esta decisión requiere:

1. registrar una nueva necesidad;
2. evaluar el impacto sobre VR-008, FM-008, estados, casos de uso y alcance;
3. crear una ADR que reemplace o modifique expresamente esta decisión;
4. actualizar las versiones de los documentos afectados;
5. incorporar los cambios mediante revisión independiente.

## Conclusión

Validado representa que el cierre cumplió las condiciones aprobadas en un momento determinado; no garantiza por sí solo que siga siendo apto para envío. La compuerta definitiva es VR-008 ejecutada inmediatamente antes del envío. Cuando esa compuerta falla, el comportamiento correcto es rechazar el envío y devolver el cierre a Bloqueado con trazabilidad completa.