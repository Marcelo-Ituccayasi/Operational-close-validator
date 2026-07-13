# Tesis del Producto

**Versión:** v0.2
**Estado:** Baseline aprobado
**Producto:** Operational Close Validator

## Propósito

Este documento define la hipótesis central del producto derivada del proceso inicial de descubrimiento del problema.

La tesis describe al usuario, el problema operativo, el resultado esperado, los límites del producto, los riesgos principales y la evidencia necesaria para validar la oportunidad.

No define la arquitectura técnica ni la estrategia de implementación.

## Tesis principal

Las organizaciones pierden tiempo y confianza en su información operativa porque los sistemas existentes registran transacciones, pero no validan de manera consistente la falta de evidencia, las autorizaciones informales o las inconsistencias de datos antes de los procesos críticos de cierre.

La oportunidad de producto consiste en crear una capa de validación operativa temprana que detecte estas inconsistencias antes de que se conviertan en retrabajo de cierre.

## Usuario objetivo

### Usuario principal

El usuario principal es la persona responsable de administración o caja que:

- registra ingresos y egresos;
- recopila documentos de soporte;
- revisa movimientos operativos;
- concilia información física y digital;
- consolida el cierre operativo;
- prepara información para contabilidad.

### Usuarios secundarios

Los usuarios secundarios pueden incluir:

- personal de finanzas que prepara balances;
- personal de logística que concilia stock antes del despacho;
- personal de gerencia que autoriza movimientos extraordinarios;
- personal de contabilidad que recibe información de cierre validada.

El MVP se concentra únicamente en el usuario responsable de administración o caja.

## Trabajos por realizar

### Administración y caja

> Cuando preparo el cierre operativo, necesito que cada movimiento registrado tenga la evidencia y autorización requeridas para poder consolidar el período sin repetir manualmente la auditoría.

### Finanzas

> Cuando preparo balances, necesito que la información operativa esté completa y validada para no devolver el cierre por correcciones.

### Logística

> Cuando preparo un despacho, necesito que la información digital refleje la realidad operativa para evitar que la ruta se retrase por inconsistencias tardías.

## Dolor del usuario

El dolor principal es la falta de concordancia entre:

- movimientos operativos registrados;
- documentos físicos o digitales de soporte;
- autorizaciones formales;
- efectivo físico;
- saldos del sistema;
- información de stock;
- información externa recibida después del corte operativo.

La falla normalmente se detecta durante la consolidación final, no cuando se registra el evento.

## Solución actual

Las organizaciones suelen compensar estas fallas mediante:

- revisiones manuales;
- comparación transacción por transacción;
- hojas de cálculo;
- llamadas y mensajes;
- confirmaciones verbales;
- búsqueda de documentos físicos;
- horas extra;
- reapertura de información previamente consolidada;
- reenvío a contabilidad.

## Por qué la solución actual es insuficiente

El enfoque actual es reactivo y depende en gran medida del esfuerzo individual.

No previene de manera confiable:

- egresos sin soporte;
- descuentos o anulaciones informales;
- montos duplicados o incorrectos;
- documentos faltantes de proveedores;
- conciliaciones externas tardías;
- eventos operativos sin un estado verificable.

El proceso también se vuelve más frágil cuando los trabajadores experimentados dejan la organización o aumenta el volumen operativo.

## Resultado deseado

El resultado esperado es un proceso de cierre operativo donde:

- cada evento registrado tenga un estado explícito;
- la falta de evidencia se detecte temprano;
- las autorizaciones requeridas sean verificables;
- las inconsistencias generen alertas visibles;
- los problemas bloqueantes impidan que el cierre avance;
- las correcciones provoquen una nueva validación;
- la información enviada a contabilidad haya superado un control final de calidad.

## Hipótesis del producto

> Si los eventos operativos registrados se validan cuando se crean o modifican, y las inconsistencias no resueltas permanecen visibles mediante estados y alertas explícitos, las organizaciones pueden reducir el retrabajo de cierre y evitar que movimientos sin soporte o autorización sean ignorados durante la consolidación.

## Principio central del producto

> El problema principal no es registrar información. El problema principal es validar su consistencia operativa antes del cierre.

## Producto propuesto

> Una capa de validación operativa temprana que evalúa eventos registrados, aplica reglas de negocio, genera alertas y controla si un cierre operativo puede avanzar.

En el MVP, las reglas pertenecen a un catálogo interno fijo y no son configurables por el usuario.

Inicialmente, el producto se concentra en:

- ingresos;
- egresos;
- descuentos;
- anulaciones;
- documentos de soporte;
- autorizaciones;
- validaciones;
- alertas;
- estados del cierre operativo.

## Límite del producto

El producto no pretende reemplazar:

- un ERP;
- un POS;
- una plataforma contable;
- la facturación electrónica;
- la gestión completa de inventario;
- los sistemas bancarios;
- la gestión completa de proveedores;
- los procedimientos de control físico.

Opera como una capa de validación y control sobre información operativa ya registrada.

## Objetivos explícitamente excluidos

El producto no proporcionará inicialmente:

- contabilidad completa;
- cálculo tributario;
- emisión de comprobantes electrónicos;
- gestión completa de inventario;
- planillas;
- automatización de conciliación bancaria;
- portales de proveedores;
- administración multisucursal;
- reportes financieros avanzados.

## Propuesta de valor inicial

> Detectar eventos operativos sin soporte, sin autorización o inconsistentes antes de que se conviertan en retrabajo de cierre.

## Riesgo principal

> El producto solo puede validar eventos que hayan sido registrados.

Si un movimiento real ocurre, pero nunca se ingresa al sistema, la capa de validación no puede detectarlo por sí sola.

Esto permanece como un riesgo operativo residual fuera del control del producto inicial.

## Riesgos adicionales

- Resistencia al cambio de hábitos operativos.
- Registro incompleto o tardío de eventos.
- Exceso de alertas que los usuarios comiencen a ignorar.
- Reglas de validación definidas incorrectamente.
- Reglas bloqueantes que interrumpan operaciones legítimas.
- Evidencia insuficiente sobre la frecuencia real del problema.
- Dificultad futura de integración con sistemas existentes.

## Evidencia requerida

La tesis del producto debe validarse con evidencia adicional como:

- tiempo entre un evento operativo y su registro;
- tiempo entre recepción de un documento y su conciliación;
- frecuencia de comprobantes faltantes;
- frecuencia de autorizaciones informales;
- número de cierres reabiertos;
- número de correcciones solicitadas por contabilidad;
- horas extra asociadas al cierre;
- tiempo promedio para resolver una discrepancia;
- porcentaje de inconsistencias detectadas antes de la consolidación final.

## Indicadores de éxito

Los posibles indicadores de éxito incluyen:

- reducción del tiempo dedicado a auditar el cierre;
- porcentaje de inconsistencias bloqueantes detectadas antes de consolidar;
- porcentaje de eventos registrados con evidencia completa;
- reducción de cierres reabiertos;
- reducción de devoluciones por parte de contabilidad;
- reducción de llamadas o mensajes necesarios para resolver discrepancias;
- tiempo necesario para llevar un evento desde pendiente o con observaciones hasta validado.

## Hipótesis del MVP

> Un egreso registrado sin el comprobante o autorización requeridos puede detectarse automáticamente, generar una alerta bloqueante, impedir que el cierre operativo sea validado y permitir que el cierre avance únicamente después de corregir la causa y revalidarla exitosamente.

## Condición de éxito del MVP

La hipótesis se considerará demostrada técnicamente cuando el MVP pueda:

1. Registrar un evento operativo.
2. Detectar evidencia o autorización faltante.
3. Generar una alerta bloqueante.
4. Bloquear el cierre operativo.
5. Adjuntar la evidencia o registrar la autorización faltante.
6. Revalidar el evento exitosamente.
7. Resolver la alerta mediante la revalidación.
8. Validar el cierre operativo.
9. Enviar el cierre a contabilidad como una transición interna de estado.

## Relación con el alcance del MVP

El dominio investigado contempla caja, documentos, stock y dependencias externas. Sin embargo, el MVP v0.3 se limita al cierre operativo de caja y a eventos de ingreso, egreso, descuento y anulación.

Las facturas de proveedores, la conciliación bancaria o POS, el stock y el cierre provisional permanecen fuera del MVP.

## Confianza actual

**Confianza general:** Media–alta

El problema operativo está respaldado por observación directa y casos investigados.

Sin embargo:

- El impacto económico estimado requiere mediciones adicionales.
- El comportamiento de los usuarios todavía no fue validado mediante un prototipo funcional.
- La eficacia del enfoque de validación continúa siendo una hipótesis de producto.
- La escalabilidad del enfoque más allá del cierre de caja todavía no ha sido demostrada.
