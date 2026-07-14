# Mapa de Problemas

**Versión:** v0.2.1

**Estado:** Línea base aprobada

**Alcance:** Procesos de cierre operativo en pequeñas y medianas empresas

## Propósito

Este documento consolida los problemas operativos identificados durante la fase inicial de descubrimiento de producto.

El mapa distingue entre problemas observados directamente, hallazgos obtenidos mediante entrevistas o análisis documental e inferencias iniciales que todavía requieren validación adicional.

Su propósito es identificar patrones operativos recurrentes antes de proponer una solución de software.

## Clasificación de evidencia

| Clasificación | Significado |
|---|---|
| Observado | El problema fue identificado mediante observación directa de un proceso operativo real. |
| Investigado | El problema fue identificado mediante entrevistas, documentos o análisis de casos operativos reportados. |
| Inferido | El problema es una hipótesis de trabajo derivada de la evidencia disponible y requiere validación adicional. |

## Inventario de problemas

| ID | Problema observado | Usuario o área afectada | Frecuencia | Impacto | Evidencia | Clasificación | Causa probable | Tiempo perdido estimado | Confianza |
|---|---|---|---|---|---|---|---|---|---|
| P-001 | Retrabajo causado por errores detectados después de iniciado el proceso de cierre de caja. | Administración | Proceso diario; frecuencia de incidentes variable | Alto | OBS-0001 | Observado | Falta de un corte operativo coordinado y de validación entre gerencia y administración. | Aproximadamente 1–3 horas por incidente | Alta |
| P-002 | Trabajo duplicado causado por la transcripción manual de información entre plataformas operativas. | Jefes de área | Semanal | Medio | DOC-0001 | Investigado | Incompatibilidad operativa o falta de integración entre hojas de cálculo y ERP. | Aproximadamente 4 horas por semana | Media |
| P-003 | Retrasos potenciales en gastos operativos causados por dependencia de autorización jerárquica. | Operaciones | Desconocida | Medio–Alto | INF-0001 | Inferido | Proceso recurrente de aprobación realizado manualmente o mediante canales informales. | No estimado | Baja |
| P-004 | Retrabajo causado por la reconstrucción manual de facturas después de una pérdida local de datos durante mantenimiento de equipos. | Administración | Aproximadamente cada seis meses | Medio | OBS-0002 | Observado | Ausencia de copias de seguridad automáticas o externas antes de actividades de soporte técnico o mantenimiento. | Aproximadamente 15–20 horas por incidente | Alta |
| P-005 | Errores y reducción de productividad causados por la curva de aprendizaje asociada a rotación o recontratación de personal. | Administración | Aproximadamente cada seis meses | Alto | OBS-0003 | Observado | Inducción insuficiente, documentación limitada de procesos e inestabilidad en el puesto. | Aproximadamente una semana por nuevo trabajador | Alta |
| P-006 | Vulnerabilidad documental causada por dependencia exclusiva de registros fiscales físicos. | Contabilidad | Exposición semanal | Alto | OBS-0004 | Observado | Falta de digitalización y ausencia de una réplica documental verificable. | No estimado directamente; genera riesgo de pérdida y auditoría | Alta |
| P-007 | Tiempo excesivo empleado en solicitar cotizaciones debido a que los contactos y tarifas históricas de proveedores están dispersos. | Administración | Mensual | Medio | INF-0002 | Inferido | Información comercial distribuida entre contactos personales, mensajes y archivos individuales. | Aproximadamente 6 horas por mes | Baja |
| P-008 | Interrupciones operativas y gastos imprevistos causados por la omisión del mantenimiento preventivo. | Operaciones y mantenimiento | Exposición mensual | Alto | INT-0001 | Investigado | Se prioriza el ahorro inmediato frente al mantenimiento preventivo planificado. | Variable | Media |
| P-009 | Retrasos o fallas técnicas causados por asignar tareas críticas a perfiles en formación sin supervisión suficiente. | Ingeniería y operaciones | Mensual | Alto | INT-0002 | Investigado | Reducción de costos sin un mecanismo adecuado de supervisión técnica. | Aproximadamente 10–15 horas por proyecto afectado | Media |
| P-010 | Descuadre financiero diario causado por la recepción de facturas de proveedores después del corte operativo. | Finanzas y administración | Semanal | Alto | INT-0003 | Investigado | Ausencia de un canal formal de entrega y de una hora límite para documentos de soporte externos. | Aproximadamente 3 horas por cierre afectado | Media |
| P-011 | Retrasos en el despacho a ruta causados por diferencias entre inventario digital e inventario físico. | Logística | Semanal | Alto | INT-0004 | Investigado | Actualizaciones tardías de inventario o falta de conciliación antes del despacho. | Aproximadamente 2 horas por ruta afectada | Media |

## Agrupación de problemas

| Clúster | Problemas relacionados | Patrón operativo compartido | Prioridad |
|---|---|---|---|
| C-001 | P-001, P-010, P-011 | Inconsistencias críticas entre dinero, documentos y stock durante procesos de cierre operativo o despacho. | Alta |
| C-002 | P-004, P-006 | Información crítica almacenada en un único punto de falla físico o local, sin respaldo verificable. | Alta |
| C-003 | P-002, P-005, P-007 | Conocimiento e información operativa dispersos y poco sistematizados. | Media |
| C-004 | P-003, P-008, P-009 | Decisiones operativas débiles causadas por control, supervisión o validación previa insuficientes. | Media–Baja |

## Clúster seleccionado

El alcance inicial del producto se concentra en:

> **C-001 — Inconsistencias críticas entre dinero, documentos y stock durante procesos de cierre operativo o despacho.**

Este clúster fue seleccionado porque:

- contiene fallas operativas de alto impacto;
- las fallas producen retrabajo medible;
- las inconsistencias se detectan con frecuencia demasiado tarde;
- el problema puede abordarse mediante validación temprana sin reemplazar el ERP, sistema contable o plataforma de inventario existente.

## Patrón central

> Las empresas pierden tiempo cuando la información crítica relacionada con dinero, documentos de soporte y stock no está sincronizada entre registros físicos, sistemas digitales y las personas responsables del proceso.

Las fallas normalmente no se originan por ausencia de herramientas de registro. Ocurren porque la información está incompleta, llega tarde, carece de soporte o autorización, o resulta inconsistente cuando debe consolidarse un proceso crítico.

## Oportunidad de producto

La oportunidad no consiste en construir otro ERP completo.

La oportunidad consiste en crear una capa de validación operativa temprana capaz de:

- identificar eventos registrados incompletos o inconsistentes;
- mantener un estado verificable para cada evento operativo;
- generar alertas antes de la consolidación final;
- impedir que un cierre avance mientras existan inconsistencias bloqueantes;
- preservar trazabilidad de validaciones, correcciones y cambios de estado.

## Relación con el alcance del MVP

El dominio investigado contempla caja, documentos, stock y dependencias externas. Sin embargo, el MVP v0.3 se limita al cierre operativo de caja y a eventos de ingreso, egreso, descuento y anulación.

Las facturas de proveedores, la conciliación bancaria o POS, el stock y el cierre provisional permanecen fuera del MVP.

## Confianza actual

El nivel general de confianza del clúster seleccionado es **medio–alto**.

P-001 está respaldado por observación directa. P-010 y P-011 están respaldados por casos investigados y requieren validación adicional en campo antes de considerar definitivas su frecuencia estimada y su impacto económico.

## Límite de cobertura

El producto puede bloquear eventos registrados que carecen de evidencia,
autorización o consistencia.

No puede detectar directamente un movimiento real que nunca fue registrado.
Ese riesgo permanece bajo controles operativos externos, como conciliación
física, supervisión o auditoría manual.