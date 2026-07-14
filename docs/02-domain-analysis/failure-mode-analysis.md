# Análisis de Modos de Falla

**Versión:** v0.1

**Estado:** Línea base aprobada

**Fase:** 02 — Análisis de dominio

**Producto:** Operational Close Validator

## Propósito

Este documento identifica los principales modos de falla del cierre operativo, su causa probable, el punto actual de detección, el retrabajo generado y la oportunidad de validación temprana.

El análisis no define arquitectura ni implementación. Su función es conectar la evidencia de Product Discovery con las reglas de validación y el modelo de dominio.

## Alcance

El dominio investigado contempla caja, documentos, autorizaciones, inventario y dependencias externas. El MVP v0.3 implementa únicamente ingreso, egreso, descuento, anulación, documentos de soporte, autorizaciones, validaciones, alertas, consolidación y envío interno a contabilidad.

## Límite de cobertura

El producto puede validar eventos que hayan sido registrados y puede bloquear un cierre cuando esos eventos carecen de evidencia, autorización o consistencia.

No puede detectar directamente un movimiento real que nunca fue registrado. Ese riesgo permanece bajo controles externos como conciliación física, supervisión o auditoría manual.

## Modos de falla

### FM-001 — Movimiento autorizado sin registro operativo verificable

**Descripción:** Gerencia autoriza un egreso o descuento, pero el movimiento no queda representado por un evento operativo digital o no se comunica a administración antes del corte.

**Causa probable:** autorización verbal, mensaje informal, papel aislado o falta de responsabilidad explícita para registrar el movimiento.

**Detección actual:** diferencia de caja durante la conciliación final o comunicación tardía del responsable.

**Impacto:** cierre bloqueado, revisión individual de transacciones y aproximadamente 1–3 horas de retrabajo por incidente.

**Oportunidad de validación:** exigir un registro trazable con monto, motivo, responsable, autorización y evidencia antes de validar el cierre.

**Cobertura del MVP:** parcial. El MVP puede validar el evento cuando existe un registro, pero no puede descubrir de forma autónoma un movimiento nunca registrado.

**Regla relacionada:** VR-001.

### FM-002 — Monto de ingreso inconsistente con su comprobante

**Descripción:** El monto registrado para un ingreso no coincide con el monto de su comprobante físico o digital.

**Causa probable:** error de digitación, omisión, duplicidad o referencia incorrecta del POS.

**Detección actual:** comparación manual realizada por administración durante el cierre.

**Impacto:** revisión y ajuste manual del movimiento, con riesgo de saldo incorrecto.

**Oportunidad de validación:** comparar el monto registrado con el monto del comprobante en el momento del registro o modificación.

**Cobertura del MVP:** incluida.

**Regla relacionada:** VR-002.

### FM-003 — Egreso menor sin comprobante legible

**Descripción:** Un egreso menor se registra sin comprobante, con un archivo ilegible o con evidencia insuficiente.

**Causa probable:** el soporte no se exige al momento del gasto o se conserva fuera del flujo operativo.

**Detección actual:** revisión manual de egresos durante la consolidación.

**Impacto:** solicitud del documento, espera, corrección y posible reapertura del cierre.

**Oportunidad de validación:** exigir evidencia legible antes de considerar el evento válido.

**Cobertura del MVP:** incluida.

**Regla relacionada:** VR-003.

### FM-004 — Factura de proveedor recibida después del corte

**Descripción:** La factura asociada a una operación llega después de la hora límite del cierre.

**Causa probable:** entrega tardía del proveedor o ausencia de un canal y corte formal para documentos externos.

**Detección actual:** identificación de documentación faltante durante el cierre o después del envío.

**Impacto:** balance incompleto, reapertura del cierre y extensión de jornada. No implica necesariamente un descuadre de caja.

**Oportunidad de validación:** vincular la factura al evento registrado y marcarla como recibida o pendiente antes del corte.

**Cobertura del MVP:** fuera del MVP.

**Regla relacionada:** VR-004.

### FM-005 — Corte bancario o POS no disponible

**Descripción:** La información externa necesaria para conciliar tarjetas, transferencias o POS no está disponible al momento del cierre.

**Causa probable:** demora de un tercero o diferencia entre horarios de corte.

**Detección actual:** ausencia del reporte durante la conciliación.

**Impacto:** cierre incompleto, ajuste al día siguiente o espera operativa.

**Oportunidad de validación:** representar la dependencia como pendiente de conciliación externa y evitar tratarla como un dato controlable por el producto.

**Cobertura del MVP:** fuera del MVP.

**Regla relacionada:** VR-005.

### FM-006 — Descuento o anulación sin autorización formal

**Descripción:** Un descuento o una anulación existe en el sistema, pero carece de autorización formal y verificable.

**Causa probable:** aprobación mediante conversación, llamada o mensaje sin registro estructurado.

**Detección actual:** diferencia de caja, revisión de transacciones o consulta a gerencia.

**Impacto:** llamadas, formalización tardía, corrección y posible bloqueo del cierre.

**Oportunidad de validación:** exigir una autorización vinculada al evento antes de validarlo.

**Cobertura del MVP:** incluida.

**Regla relacionada:** VR-006.

### FM-007 — Dato manual de consolidación sin fuente trazable

**Descripción:** Un valor se ingresa manualmente en la consolidación sin referencia a su fuente original o no coincide con ella.

**Causa probable:** transcripción manual y ausencia de trazabilidad entre dato y origen.

**Detección actual:** diferencia encontrada en la revisión final.

**Impacto:** ajuste, reapertura del cierre y repetición de cálculos.

**Oportunidad de validación:** conservar la referencia de origen y comprobar la coincidencia antes de consolidar.

**Cobertura del MVP:** fuera del MVP.

**Regla relacionada:** VR-007.

### FM-008 — Envío a contabilidad con eventos no validados

**Descripción:** El cierre se envía a contabilidad cuando al menos un evento no se encuentra en estado Validado, existe una alerta bloqueante activa, hay resultados de validación aplicables fallidos o no vigentes, o la consolidación no está completa.

**Causa probable:** una falla previa no fue detectada, una corrección invalidó resultados anteriores, la consolidación quedó desactualizada o no existió un control final obligatorio.

**Detección actual:** contabilidad identifica el problema después del envío.

**Impacto:** devolución, corrección, reenvío y pérdida de trazabilidad.

**Oportunidad de validación:** ejecutar un control final que exija que todos los eventos estén Validados, que no existan alertas bloqueantes activas, que los resultados aplicables estén vigentes y satisfechos, y que la consolidación esté completa.

**Cobertura del MVP:** incluida.

**Regla relacionada:** VR-008.

**Nota:** FM-008 representa una falla del control final y una consecuencia de otros modos de falla; no debe interpretarse como causa raíz independiente.

## Familias de falla

### FAM-001 — Autorización formal

Incluye FM-001 y FM-006.

Patrón: la decisión operativa existe, pero no tiene una representación formal y trazable suficiente.

### FAM-002 — Evidencia documental

Incluye FM-003 y FM-004.

Patrón: el evento existe, pero la documentación requerida está ausente, ilegible o llega tarde.

### FAM-003 — Consistencia de monto y registro

Incluye FM-002 y FM-007.

Patrón: el dato registrado no coincide con su fuente o no conserva trazabilidad.

### FAM-004 — Dependencias externas

Incluye FM-005.

Patrón: la información requerida depende de un tercero y no puede tratarse como disponible bajo control interno.

### FAM-005 — Control final del cierre

Incluye FM-008.

Patrón: el cierre avanza sin verificar que todas las condiciones obligatorias se mantengan satisfechas.

## Prioridad para el MVP

Prioridad crítica:
- FM-001, en su componente detectable;
- FM-002;
- FM-003;
- FM-006;
- FM-008.

Fuera del MVP:
- FM-004;
- FM-005;
- FM-007.

## Conclusión

El patrón central no es la falta de herramientas para registrar información. El problema es que los movimientos, documentos y autorizaciones no mantienen un estado verificable antes de la consolidación.

La solución debe validar temprano, conservar trazabilidad, generar alertas y bloquear el cierre cuando exista una inconsistencia crítica.