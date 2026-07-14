# Declaración del Problema

**Versión:** v0.2

**Estado:** Línea base aprobada

**Clúster seleccionado:** C-001 — Inconsistencias operativas durante procesos críticos de cierre

## Problema principal

Los procesos de cierre operativo relacionados con caja, documentos de soporte y stock generan retrabajo evitable porque la información crítica llega tarde, no se valida cuando se registra o no coincide entre los registros físicos y los sistemas digitales.

Estas inconsistencias suelen descubrirse durante la consolidación final o después de que la información ya fue enviada a otra área, en lugar de detectarse cuando ocurre el evento operativo.

## Usuarios afectados

### Usuarios principales

- Personal de administración responsable de registrar movimientos operativos.
- Personal de caja o tesorería responsable de conciliar saldos.
- Usuarios responsables de consolidar el cierre operativo.

### Usuarios secundarios

- Personal de finanzas que prepara balances o información contable.
- Personal de logística que valida stock antes del despacho.
- Personal de gerencia que autoriza movimientos extraordinarios.
- Personal de contabilidad que recibe el cierre consolidado.

## Contexto operativo

El problema aparece durante procesos como:

- conciliación diaria de caja;
- registro de ingresos y egresos;
- validación de descuentos y anulaciones;
- recepción de documentos de soporte;
- procesamiento de facturas de proveedores;
- conciliación de saldos físicos y digitales;
- preparación de información para contabilidad;
- validación de stock antes del despacho.

El proceso es especialmente vulnerable cuando la información se distribuye entre:

- facturas o recibos físicos;
- hojas de cálculo;
- registros de ERP o POS;
- mensajes y autorizaciones verbales;
- archivos personales;
- proveedores externos, como bancos o proveedores comerciales.

## Patrón de falla observado

Un caso representativo mostró la siguiente secuencia:

1. Administración recopiló y revisó las facturas del período operativo anterior.
2. La información fue consolidada manualmente para contabilidad.
3. Gerencia solicitó el balance actualizado.
4. Los valores físicos y digitales no coincidieron.
5. Un movimiento previamente autorizado no había sido registrado o comunicado correctamente.
6. Administración repitió el proceso de auditoría e ingreso de datos.
7. La información contable tuvo que corregirse y enviarse nuevamente.

El incidente generó aproximadamente tres horas de retrabajo.

## Hipótesis de causa raíz

> No existe un mecanismo temprano y coordinado que verifique si los eventos operativos cuentan con la evidencia, autorización e información consistente requeridas antes de incluirlos en el cierre final.

Por tanto, el problema no se limita a un ingreso de datos incorrecto.

También involucra:

- autorizaciones informales;
- documentos de soporte ausentes o tardíos;
- comunicación tardía entre áreas;
- transcripción manual;
- falta de un estado verificable para cada evento;
- validación ejecutada únicamente al final del proceso.

## Método actual de resolución

Las organizaciones suelen resolver estas inconsistencias mediante esfuerzo manual adicional:

- revisar transacciones una por una;
- buscar documentos físicos;
- llamar o enviar mensajes al responsable;
- solicitar autorizaciones faltantes;
- corregir hojas de cálculo o registros del ERP;
- reabrir información previamente consolidada;
- extender la jornada laboral;
- volver a enviar el cierre a contabilidad.

## Por qué el método actual es insuficiente

El enfoque actual es insuficiente porque es reactivo.

Intenta reparar las inconsistencias después de que ya afectaron el proceso de cierre, en lugar de prevenirlas cuando se registra el evento operativo.

Además, depende en gran medida de:

- memoria individual;
- comunicación informal;
- documentos físicos;
- comparación manual;
- experiencia del personal;
- disponibilidad de terceros.

Por ello, el proceso no escala de forma confiable y permanece expuesto a errores humanos repetidos.

## Consecuencias

### Consecuencias directas

- Revisión repetida de movimientos operativos.
- Corrección manual de saldos.
- Reapertura de cierres previamente consolidados.
- Envío tardío a contabilidad.
- Extensión de la jornada laboral.
- Retrasos en despachos u otras operaciones.

### Consecuencias organizacionales potenciales

Las siguientes consecuencias se consideran plausibles, pero requieren evidencia adicional antes de tratarse como confirmadas:

- reducción de la confianza en la información operativa;
- deterioro del ambiente laboral;
- retraso en decisiones de gerencia;
- mayor exposición ante auditorías;
- pérdidas financieras causadas por discrepancias no resueltas.

## Impacto estimado

Con base en las observaciones y los casos investigados disponibles:

- P-001 puede generar aproximadamente 1–3 horas de retrabajo por incidente.
- P-010 puede generar aproximadamente 3 horas de retrabajo por cierre afectado.
- P-011 puede generar aproximadamente 2 horas de retraso por ruta afectada.

P-010 y P-011 representan aproximadamente cinco horas de impacto cuando
cada caso ocurre una vez durante la semana.

La estimación total de **10–15 horas de trabajo evitable por semana** es una
hipótesis provisional que combina los casos investigados con una frecuencia
todavía no medida de incidentes P-001.

Por tanto, esta cifra no representa todavía un resultado estadístico
confirmado y debe validarse mediante mediciones operativas adicionales.

Bajo el supuesto de que ese impacto se mantuviera durante 52 semanas, la
proyección anual equivalente sería de aproximadamente **520–780 horas**.
Esta proyección depende directamente de que la estimación semanal sea
confirmada.

## Oportunidad de producto

> **Una capa de validación operativa temprana que detecte inconsistencias antes de que se conviertan en retrabajo de cierre.**

El producto propuesto debe verificar si los eventos operativos registrados:

- contienen los datos requeridos;
- tienen documentación de soporte cuando corresponde;
- cuentan con autorización formal cuando corresponde;
- mantienen un estado verificable;
- cumplen las reglas de negocio aplicables;
- pueden incluirse en un cierre validado.

## Límite del producto

El producto propuesto no pretende reemplazar:

- el ERP;
- el POS;
- el sistema contable;
- la facturación electrónica;
- la gestión completa de inventario;
- las plataformas bancarias;
- los procedimientos de control físico.

Su propósito es operar como una capa de validación y control sobre eventos operativos ya registrados.

## Declaración del problema

> Los equipos de administración, caja, finanzas y logística experimentan retrabajo y cierres operativos tardíos porque los movimientos registrados, documentos de soporte, autorizaciones y saldos no se validan de forma consistente antes de la consolidación final. Los procesos actuales detectan estas fallas demasiado tarde y las resuelven mediante revisión, comunicación y corrección manual.

## Hipótesis inicial de producto

> Si los eventos operativos se validan cuando se registran y sus inconsistencias no resueltas permanecen visibles mediante estados y alertas explícitos, la organización puede reducir el retrabajo de cierre, evitar que se ignoren movimientos sin soporte y mejorar la confiabilidad de la información enviada a contabilidad.

## Principio central del producto

> El problema principal no es la capacidad de registrar información. El problema principal es la incapacidad de verificar su consistencia operativa antes del cierre.

## Relación con el alcance del MVP

El dominio investigado contempla caja, documentos, stock y dependencias externas. Sin embargo, el MVP v0.3 se limita al cierre operativo de caja y a eventos de ingreso, egreso, descuento y anulación.

Las facturas de proveedores, la conciliación bancaria o POS, el stock y el cierre provisional permanecen fuera del MVP.

## Nivel de confianza

**Confianza general:** Media–alta

- Existe evidencia directa de inconsistencias de caja detectadas tardíamente.
- Los casos investigados respaldan patrones similares relacionados con documentos de proveedores y stock.
- El tiempo estimado y el impacto económico requieren mediciones adicionales.
- La eficacia de la validación temprana continúa siendo una hipótesis de producto que debe comprobarse mediante el MVP.

## Límite de cobertura

El producto puede bloquear eventos registrados que carecen de evidencia,
autorización o consistencia.

No puede detectar directamente un movimiento real que nunca fue registrado.
Ese riesgo permanece bajo controles operativos externos, como conciliación
física, supervisión o auditoría manual.
