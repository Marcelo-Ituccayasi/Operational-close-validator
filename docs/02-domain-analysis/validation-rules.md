# Reglas de Validación

**Versión:** v0.2
**Estado:** Línea base aprobada
**Fase:** 02 — Análisis de dominio
**Producto:** Operational Close Validator

## Propósito

Este documento define las reglas de negocio que convierten los modos de falla identificados en condiciones verificables.

Las reglas describen comportamiento de dominio. No definen interfaz, persistencia ni tecnología.

## Convenciones

### Resultados de validación

- **Satisfecha:** la condición se cumple.
- **Fallida:** la condición no se cumple.
- **Pendiente:** la evaluación depende de información externa todavía no disponible. Este resultado pertenece al dominio general y no se utiliza en el MVP v0.3.

### Severidad

- **Crítica:** bloquea el cierre.
- **Alta:** bloquea el cierre o el evento.
- **Media:** puede permitir un cierre provisional en el dominio completo.

### Estados de evento relacionados

- Registrado.
- Pendiente de soporte.
- Pendiente de autorización.
- Pendiente de conciliación externa.
- Con observaciones.
- Validado.

“Observado” se reserva para la clasificación de evidencia de Product Discovery. “Con observaciones” identifica el estado de un evento con una inconsistencia.

## Catálogo de reglas

### VR-001 — Registro trazable de movimientos autorizados

**Regla:** Todo egreso o descuento autorizado por gerencia debe estar representado por un Evento Operativo registrado y trazable antes de validar el cierre.

**Datos requeridos:**
- monto;
- motivo;
- responsable;
- referencia de autorización;
- evidencia de soporte cuando corresponda.

**Severidad:** Crítica.

**Efecto de falla:** bloquea el cierre.

**Estado resultante:**
- Pendiente de autorización, cuando falta la autorización formal;
- Con observaciones, cuando existe una inconsistencia de registro o trazabilidad.

**Modo de falla relacionado:** FM-001.

**Cobertura del MVP:** incluida.

**Límite:** La regla es normativa, pero la detección automatizada solo puede evaluar movimientos conocidos por el sistema. Un movimiento real nunca registrado permanece fuera de la cobertura directa del producto.

### VR-002 — Coincidencia de monto de ingreso

**Regla:** Todo ingreso registrado debe coincidir con el monto de su comprobante físico o digital asociado.

**Datos requeridos:**
- monto registrado;
- monto del comprobante;
- identificador o referencia de transacción.

**Severidad:** Crítica.

**Efecto de falla:** bloquea el evento y el cierre.

**Estado resultante:** Con observaciones.

**Modo de falla relacionado:** FM-002.

**Cobertura del MVP:** incluida.

### VR-003 — Evidencia legible para egresos menores

**Regla:** Todo egreso menor que requiera soporte debe tener un comprobante presente y legible antes de considerarse válido.

**Datos requeridos:**
- archivo o referencia del comprobante;
- tipo de evidencia;
- verificación de legibilidad;
- relación con el evento.

**Severidad:** Alta.

**Efecto de falla:** bloquea el evento y el cierre.

**Estado resultante:** Pendiente de soporte.

**Modo de falla relacionado:** FM-003.

**Cobertura del MVP:** incluida.

### VR-004 — Estado documental de factura de proveedor

**Regla:** Toda factura de proveedor vinculada a un evento operativo registrado debe estar recibida o marcada formalmente como pendiente antes del corte.

**Datos requeridos:**
- evento relacionado;
- referencia de proveedor;
- estado de recepción;
- fecha esperada;
- hora de corte.

**Severidad:** Media.

**Efecto de falla:** impide el cierre final, pero puede permitir un cierre provisional en el dominio completo.

**Estado resultante:** Pendiente de soporte.

**Modo de falla relacionado:** FM-004.

**Cobertura del MVP:** fuera del MVP.

### VR-005 — Dependencia de conciliación externa

**Regla:** El cierre no puede considerarse final cuando el corte bancario, de transferencias o POS requerido no está disponible.

**Datos requeridos:**
- tipo de fuente externa;
- período esperado;
- estado de disponibilidad;
- referencia del reporte cuando exista.

**Severidad:** Media.

**Efecto de falla:** permite únicamente un cierre provisional en el dominio completo.

**Estado resultante:** Pendiente de conciliación externa.

**Modo de falla relacionado:** FM-005.

**Cobertura del MVP:** fuera del MVP.

### VR-006 — Autorización formal de descuento o anulación

**Regla:** Todo descuento o anulación debe tener una autorización formal, registrada y vinculada antes de considerarse válido.

**Datos requeridos:**
- evento autorizado;
- responsable de autorizar;
- motivo;
- fecha y hora;
- referencia verificable.

**Severidad:** Crítica.

**Efecto de falla:** bloquea el evento y el cierre.

**Estado resultante:** Pendiente de autorización.

**Modo de falla relacionado:** FM-006.

**Cobertura del MVP:** incluida.

**Relación con VR-001:** VR-001 exige que el movimiento autorizado exista y sea trazable. VR-006 exige específicamente que el descuento o la anulación tenga una autorización formal vinculada.

### VR-007 — Trazabilidad de datos manuales

**Regla:** Todo dato ingresado manualmente en la consolidación debe conservar una referencia a su fuente original y coincidir con ella.

**Datos requeridos:**
- valor ingresado;
- fuente original;
- referencia de origen;
- responsable del ingreso.

**Severidad:** Alta.

**Efecto de falla:** bloquea la consolidación.

**Estado resultante:** Con observaciones.

**Modo de falla relacionado:** FM-007.

**Cobertura del MVP:** fuera del MVP.

### VR-008 — Control final antes del envío

**Regla:** Un cierre solo puede enviarse a contabilidad cuando todos sus
eventos se encuentran en estado Validado, no existen alertas bloqueantes
activas, todos los resultados de validación aplicables están vigentes y
satisfechos, y la consolidación está completa.

**Datos requeridos:**
- estado de todos los eventos;
- estado de todas las alertas;
- resultados vigentes de validación;
- resultado de consolidación.

**Severidad:** Crítica.

**Efecto de falla:** rechaza el envío y coloca el cierre en estado Bloqueado.

**Estado resultante del cierre:** Bloqueado.

**Modo de falla relacionado:** FM-008.

**Cobertura del MVP:** incluida.

## Familias de reglas

### FAM-001 — Autorización formal

- VR-001.
- VR-006.

### FAM-002 — Evidencia documental

- VR-003.
- VR-004.

### FAM-003 — Consistencia de monto y registro

- VR-002.
- VR-007.

### FAM-004 — Dependencias externas

- VR-005.

### FAM-005 — Control final del cierre

- VR-008.

## Orden de evaluación

1. Determinar las reglas aplicables al evento.
2. Validar datos, evidencia y autorización.
3. Registrar un resultado por regla.
4. Generar o mantener una alerta por cada falla relevante.
5. Revalidar el evento después de una corrección.
6. Consolidar únicamente cuando las condiciones del cierre se cumplan.
7. Ejecutar VR-008 inmediatamente antes del envío a contabilidad.

## Invariantes

- Las reglas del MVP pertenecen a un catálogo interno fijo y no son configurables por el usuario.
- Un evento no puede quedar Validado si alguna regla aplicable está fallida.
- Una modificación del evento invalida los resultados anteriores que dependan de los datos modificados.
- Una alerta solo puede quedar Resuelta después de una revalidación exitosa de la entidad afectada.
- Descartar una alerta requiere justificación y autorización.
- El cierre no puede quedar Validado si existe una alerta bloqueante activa.
- El cierre no puede enviarse a contabilidad sin ejecutar nuevamente el control final VR-008.

## Reglas incluidas en el MVP v0.3

- VR-001.
- VR-002.
- VR-003.
- VR-006.
- VR-008.

## Reglas fuera del MVP v0.3

- VR-004.
- VR-005.
- VR-007.

## Conclusión

El catálogo convierte fallas operativas conocidas en condiciones verificables. Su objetivo es detectar inconsistencias temprano, mantenerlas visibles y evitar que el cierre avance hasta que las condiciones críticas vuelvan a cumplirse.