# Casos de Uso

**Versión:** v0.2

**Estado:** Línea base aprobada

**Fase:** 03 — Comportamiento del sistema

**Producto:** Operational Close Validator

## Propósito

Este documento describe las interacciones funcionales aprobadas para el MVP y las relaciona con las reglas de validación, los estados del dominio y la máquina de estados.

Los casos de uso expresan comportamiento observable. No definen pantallas, endpoints, tablas, framework ni arquitectura.

## Alcance del MVP

Incluido:

- un único usuario responsable preconfigurado;
- autenticación básica;
- registro de Ingreso, Egreso, Descuento y Anulación;
- adjuntar Evidencia de Soporte;
- registrar Autorización formal;
- ejecutar VR-001, VR-002, VR-003, VR-006 y VR-008;
- generar y gestionar Alertas;
- corregir y revalidar Eventos Operativos;
- consolidar el Cierre Operativo;
- enviar internamente el cierre a contabilidad.

Fuera del MVP:

- múltiples usuarios o roles reales;
- aprobación remota de gerencia;
- recepción de contabilidad como actor externo;
- facturas de proveedores como proceso;
- conciliación bancaria o POS;
- inventario;
- cierre Provisional;
- reglas configurables por el usuario;
- integración contable real;
- detección autónoma de movimientos nunca registrados;
- reapertura o rectificación de un cierre ya Enviado a contabilidad.

## Actor

### Usuario responsable

Usuario preconfigurado que se autentica y ejecuta el flujo completo del MVP.

Responsabilidades:

- registrar y corregir Eventos Operativos;
- adjuntar evidencia;
- registrar autorizaciones;
- ejecutar validaciones;
- revisar y gestionar Alertas;
- consolidar el cierre;
- ejecutar el control final;
- enviar internamente el cierre a contabilidad.

Las funciones de gerencia y contabilidad se representan mediante datos y acciones simuladas dentro del mismo flujo. No constituyen actores independientes del MVP.

## Precondiciones globales

- El catálogo interno de reglas está disponible y versionado.
- El sistema conoce el Cierre Operativo sobre el cual se trabaja.
- El Cierre Operativo no está Enviado a contabilidad.
- Las acciones protegidas requieren una sesión autenticada.
- Cada Evento Operativo pertenece a un Cierre Operativo.

## Postcondiciones globales

- Toda acción relevante conserva fecha, hora y usuario responsable.
- Toda validación genera o actualiza Resultados de Validación trazables.
- Toda modificación de datos relevantes invalida los resultados dependientes.
- Toda falla relevante puede generar o mantener una Alerta.
- Ningún cierre avanza a Enviado a contabilidad sin superar VR-008 inmediatamente antes del envío.

## UC-001 — Autenticarse

### Objetivo

Permitir que el usuario responsable acceda al sistema mediante autenticación básica.

### Actor principal

Usuario responsable.

### Precondiciones

- Existe un usuario preconfigurado y habilitado.
- El usuario no mantiene una sesión autenticada válida.

### Flujo principal

1. El usuario ingresa sus credenciales.
2. El sistema verifica la identidad contra la configuración autorizada.
3. El sistema crea una sesión autenticada.
4. El sistema permite acceder a las funciones del MVP.

### Flujos alternativos

A1. Credenciales inválidas.

1. El sistema rechaza el acceso.
2. No crea una sesión.
3. Registra el intento conforme a las políticas técnicas que se definan posteriormente.

### Postcondiciones

- El usuario dispone de una sesión autenticada válida, o el acceso fue rechazado sin modificar datos del dominio.

## UC-002 — Registrar un Evento Operativo

### Objetivo

Crear un Evento Operativo de tipo Ingreso, Egreso, Descuento o Anulación dentro de un Cierre Operativo.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- El Cierre Operativo está en Preparación o Bloqueado.
- El cierre no está Enviado a contabilidad.

### Datos mínimos

- tipo de evento;
- monto;
- fecha y hora de ocurrencia;
- responsable;
- motivo o descripción;
- Cierre Operativo relacionado.

### Flujo principal

1. El usuario selecciona el Cierre Operativo.
2. El usuario registra los datos del evento.
3. El sistema verifica que el tipo pertenezca al catálogo del MVP.
4. El sistema crea el Evento Operativo en estado Registrado.
5. El sistema determina si el evento requiere Evidencia de Soporte o Autorización.
6. El sistema deja el evento disponible para validación.

### Flujos alternativos

A1. Faltan datos obligatorios.

1. El sistema rechaza el registro.
2. Informa los datos faltantes.
3. No crea un evento incompleto.

A2. El cierre está Enviado a contabilidad.

1. El sistema rechaza el registro.
2. No modifica el cierre enviado.

### Postcondiciones

- Existe un Evento Operativo en estado Registrado y vinculado al cierre, o la operación fue rechazada sin crear datos parciales.

## UC-003 — Adjuntar Evidencia de Soporte

### Objetivo

Vincular una evidencia verificable a un Evento Operativo.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- El Evento Operativo existe.
- El cierre relacionado no está Enviado a contabilidad.

### Datos mínimos

- archivo o referencia verificable;
- tipo de evidencia;
- evento relacionado;
- monto respaldado cuando corresponda;
- fecha;
- condición de legibilidad.

### Flujo principal

1. El usuario selecciona el Evento Operativo.
2. Adjunta o registra la referencia de la evidencia.
3. El sistema vincula la evidencia al evento.
4. El sistema invalida los Resultados de Validación dependientes de evidencia.
5. Si el evento estaba Validado, pasa a Registrado.
6. Si el cierre relacionado estaba Validado, pasa a Bloqueado.
7. El evento queda pendiente de revalidación.

### Flujos alternativos

A1. Evidencia ilegible o inválida.

1. El sistema conserva la referencia cuando sea necesario para trazabilidad.
2. La evidencia no satisface la regla aplicable.
3. El evento puede quedar Pendiente de soporte.
4. Se genera o mantiene una Alerta bloqueante.

A2. El archivo o referencia no puede almacenarse.

1. El sistema informa el fallo.
2. No declara la evidencia como válida.
3. Mantiene el evento pendiente de soporte cuando corresponda.

### Postcondiciones

- La evidencia queda vinculada y los resultados dependientes quedan no vigentes hasta ejecutar una nueva validación.

## UC-004 — Registrar una Autorización formal

### Objetivo

Vincular una Autorización formal y verificable a un Evento Operativo que la requiere.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- El Evento Operativo existe.
- El cierre relacionado no está Enviado a contabilidad.

### Datos mínimos

- Evento Operativo autorizado;
- responsable de autorizar;
- motivo;
- fecha y hora;
- referencia formal verificable.

### Flujo principal

1. El usuario selecciona el Evento Operativo.
2. Registra los datos de la autorización.
3. El sistema vincula la Autorización al evento.
4. El sistema invalida los Resultados de Validación dependientes de autorización.
5. Si el evento estaba Validado, pasa a Registrado.
6. Si el cierre relacionado estaba Validado, pasa a Bloqueado.
7. El evento queda pendiente de revalidación.

### Flujos alternativos

A1. La autorización carece de referencia verificable.

1. El sistema no la considera formalmente válida.
2. El evento puede permanecer Pendiente de autorización.
3. Se genera o mantiene una Alerta bloqueante.

### Postcondiciones

- La Autorización queda vinculada y los resultados dependientes quedan no vigentes hasta ejecutar una nueva validación.

## UC-005 — Validar un Evento Operativo

### Objetivo

Evaluar las reglas aplicables y determinar el estado verificable del Evento Operativo.

### Actor principal

Usuario responsable.

### Reglas del MVP

- VR-001 — Registro trazable de movimientos autorizados.
- VR-002 — Coincidencia de monto de ingreso.
- VR-003 — Evidencia legible para egresos menores.
- VR-006 — Autorización formal de descuento o anulación.

VR-008 se aplica al cierre y se describe en UC-009.

### Precondiciones

- El usuario está autenticado.
- El Evento Operativo existe.
- El cierre relacionado no está Enviado a contabilidad.

### Flujo principal

1. El usuario solicita la validación del evento o el sistema la ejecuta como parte de una acción aprobada.
2. El sistema determina las reglas aplicables según tipo y datos del evento.
3. El sistema evalúa cada regla aplicable.
4. Registra un Resultado de Validación por regla.
5. Si todas las reglas aplicables quedan Satisfechas y no existe una Alerta bloqueante activa, cambia el evento a Validado.
6. Si el cierre estaba Validado y el evento deja de estarlo, el sistema reevalúa el cierre y lo coloca en Bloqueado.

### Flujos alternativos

A1. Falta Evidencia de Soporte válida o legible.

1. La regla aplicable queda Fallida.
2. El evento pasa a Pendiente de soporte.
3. El sistema genera o mantiene una Alerta bloqueante.

A2. Falta Autorización formal.

1. La regla aplicable queda Fallida.
2. El evento pasa a Pendiente de autorización.
3. El sistema genera o mantiene una Alerta bloqueante.

A3. Existe una inconsistencia de monto, registro o trazabilidad.

1. La regla aplicable queda Fallida.
2. El evento pasa a Con observaciones.
3. El sistema genera o mantiene una Alerta bloqueante.

A4. Una validación depende de conciliación externa.

Este flujo pertenece al dominio completo y queda fuera del MVP.

### Postcondiciones

- El evento queda Validado, Pendiente de soporte, Pendiente de autorización o Con observaciones.
- Los Resultados de Validación y las Alertas relacionadas conservan trazabilidad.

## UC-006 — Gestionar una Alerta

### Objetivo

Permitir que el usuario reconozca, revise, corrija o descarte una Alerta sin romper las invariantes del dominio.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- La Alerta existe y se encuentra Activa, Reconocida o En revisión.

### Flujo principal de reconocimiento y revisión

1. El usuario abre la Alerta.
2. El sistema muestra causa, severidad, entidad afectada y regla relacionada.
3. El usuario puede reconocerla.
4. La Alerta pasa a Reconocida sin perder su efecto bloqueante.
5. El usuario inicia la revisión.
6. La Alerta pasa a En revisión.
7. El usuario corrige la causa mediante el caso de uso correspondiente.
8. El sistema exige revalidación de la entidad afectada.
9. Solo después de una revalidación exitosa, la Alerta pasa a Resuelta.

### Flujo alternativo de descarte

A1. Descartar una Alerta.

1. El usuario solicita el descarte.
2. El sistema verifica que la acción esté autorizada.
3. El usuario registra una justificación obligatoria.
4. El sistema conserva fecha, responsable y motivo.
5. La Alerta pasa a Descartada.
6. El sistema vuelve a evaluar las demás reglas e invariantes.

### Restricciones

- Reconocer una Alerta no corrige la causa.
- Una Alerta no puede marcarse manualmente como Resuelta.
- Resolver requiere una revalidación exitosa y un Resultado de Validación vigente asociado.
- Descartar no cambia automáticamente el Evento Operativo a Validado.
- Descartar no garantiza que el Cierre Operativo pueda avanzar.

### Postcondiciones

- La Alerta queda Reconocida, En revisión, Resuelta o Descartada con trazabilidad completa.

## UC-007 — Corregir y revalidar un Evento Operativo

### Objetivo

Corregir la causa de una inconsistencia y volver a evaluar el Evento Operativo.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- El evento está Pendiente de soporte, Pendiente de autorización o Con observaciones, o se modificará un evento previamente Validado.
- El cierre relacionado no está Enviado a contabilidad.

### Flujo principal

1. El usuario identifica la causa mediante la Alerta o los Resultados de Validación.
2. Corrige los datos, adjunta evidencia o registra la autorización faltante.
3. El sistema invalida los resultados dependientes anteriores.
4. El sistema marca como no vigente la consolidación afectada.
5. El usuario ejecuta nuevamente la validación del evento.
6. El sistema registra nuevos Resultados de Validación.
7. Si todas las reglas aplicables quedan Satisfechas, el evento pasa a Validado.
8. Las Alertas relacionadas solo pasan a Resueltas cuando existe una revalidación exitosa asociada.
9. El cierre permanece Bloqueado o en Preparación hasta volver a consolidarse y cumplir todas sus condiciones.

### Flujos alternativos

A1. La corrección no resuelve todas las fallas.

1. El evento conserva o cambia a un estado no validado apropiado.
2. Las Alertas correspondientes permanecen activas, reconocidas o en revisión.
3. El cierre no puede avanzar a Validado.

### Postcondiciones

- El evento tiene Resultados de Validación vigentes.
- Su estado refleja el resultado real de la revalidación.
- Las Alertas Resueltas referencian la revalidación exitosa.

## UC-008 — Consolidar el Cierre Operativo

### Objetivo

Calcular y verificar los totales y saldos del cierre a partir de Eventos Operativos vigentes.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- El cierre está en Preparación o Bloqueado.
- Todos los Eventos Operativos están en estado Validado.
- No existen Alertas bloqueantes activas.
- Todos los Resultados de Validación aplicables están vigentes y satisfechos.

### Flujo principal

1. El usuario solicita la consolidación.
2. El sistema obtiene los Eventos Operativos vigentes del cierre.
3. Calcula totales por tipo de evento.
4. Calcula saldo esperado, saldo real y diferencia.
5. Registra fecha, hora y responsable de la consolidación.
6. Verifica nuevamente que todos los eventos continúen Validados.
7. Verifica que no existan Alertas bloqueantes activas.
8. Verifica que los resultados aplicables continúen vigentes y satisfechos.
9. Si todas las condiciones se cumplen, el cierre pasa a Validado.

### Flujos alternativos

A1. Existe al menos un Evento Operativo no Validado.

1. El sistema rechaza la consolidación final.
2. El cierre queda o pasa a Bloqueado.
3. Informa las entidades que requieren corrección.

A2. Existe una Alerta bloqueante activa.

1. El sistema rechaza la consolidación final.
2. El cierre queda o pasa a Bloqueado.

A3. Un resultado quedó no vigente durante el proceso.

1. El sistema rechaza la consolidación.
2. El cierre queda Bloqueado.
3. Exige revalidar las entidades afectadas.

### Postcondiciones

- El cierre queda Validado con consolidación vigente, o queda Bloqueado sin declararse apto para envío.

## UC-009 — Enviar el Cierre Operativo a contabilidad

### Objetivo

Ejecutar el control final y registrar el envío interno de un cierre válido a contabilidad.

### Actor principal

Usuario responsable.

### Precondiciones

- El usuario está autenticado.
- El cierre se encuentra en estado Validado.
- Existe una consolidación completa y vigente.

### Flujo principal

1. El usuario solicita el envío a contabilidad.
2. El sistema ejecuta nuevamente VR-008 inmediatamente antes del envío.
3. Verifica que todos los Eventos Operativos continúen en estado Validado.
4. Verifica que no existan Alertas bloqueantes activas.
5. Verifica que todos los Resultados de Validación aplicables estén vigentes y satisfechos.
6. Verifica que la consolidación esté completa y vigente.
7. VR-008 queda Satisfecha.
8. El sistema registra fecha, hora y responsable del envío.
9. El cierre pasa a Enviado a contabilidad.

### Flujos alternativos

A1. Algún Evento Operativo ya no está Validado.

1. VR-008 queda Fallida.
2. El sistema rechaza el envío.
3. El cierre pasa de Validado a Bloqueado.
4. Identifica los eventos que requieren corrección y revalidación.

A2. Existe una Alerta bloqueante activa.

1. VR-008 queda Fallida.
2. El sistema rechaza el envío.
3. El cierre pasa a Bloqueado.

A3. Existe un resultado fallido o no vigente.

1. VR-008 queda Fallida.
2. El sistema rechaza el envío.
3. El cierre pasa a Bloqueado.
4. Exige revalidación y nueva consolidación.

A4. La consolidación está incompleta o desactualizada.

1. VR-008 queda Fallida.
2. El sistema rechaza el envío.
3. El cierre pasa a Bloqueado.
4. Exige ejecutar nuevamente la consolidación.

### Postcondiciones

Éxito:

- el cierre queda Enviado a contabilidad;
- se conserva trazabilidad del envío interno;
- el cierre no admite modificaciones dentro del MVP.

Falla:

- el envío no ocurre;
- el cierre queda Bloqueado;
- se conserva la causa de rechazo;
- se requiere corrección, revalidación y nueva consolidación.

## Relaciones entre casos de uso

- UC-002 crea el Evento Operativo que será validado por UC-005.
- UC-003 y UC-004 pueden completar datos requeridos antes de UC-005.
- UC-005 puede generar Alertas gestionadas mediante UC-006.
- UC-006 puede conducir a UC-003, UC-004 o UC-007 según la causa.
- UC-007 incluye una nueva ejecución de UC-005.
- UC-008 requiere que todos los Eventos Operativos hayan completado UC-005 exitosamente.
- UC-009 requiere un cierre Validado por UC-008 y ejecuta nuevamente VR-008.

## Matriz de trazabilidad

- UC-001 → Usuario y autenticación básica del MVP.
- UC-002 → Evento Operativo y Cierre Operativo.
- UC-003 → Evidencia de Soporte; VR-001, VR-002 y VR-003.
- UC-004 → Autorización; VR-001 y VR-006.
- UC-005 → Resultado de Validación, Alerta y estados del Evento Operativo.
- UC-006 → estados Activa, Reconocida, En revisión, Resuelta y Descartada.
- UC-007 → invalidación de resultados, revalidación y resolución de Alertas.
- UC-008 → Consolidación y transición Preparación o Bloqueado → Validado.
- UC-009 → VR-008, FM-008 y transiciones Validado → Enviado a contabilidad o Validado → Bloqueado.

## Reglas de aceptación transversal

1. Ninguna acción protegida se ejecuta sin autenticación.
2. Un Evento Operativo no puede quedar Validado si algún Resultado de Validación aplicable tiene valor Fallida o no está vigente.
3. Ninguna Alerta queda Resuelta sin revalidación exitosa.
4. Ninguna Alerta queda Descartada sin autorización y justificación.
5. Ningún Cierre Operativo queda Validado con un evento cuyo estado no sea Validado.
6. Ningún cierre se envía con una Alerta bloqueante activa.
7. Ningún cierre se envía con resultados fallidos o no vigentes.
8. Ningún cierre se envía con consolidación incompleta o desactualizada.
9. VR-008 se ejecuta inmediatamente antes del envío.
10. Una falla de VR-008 rechaza el envío y coloca el cierre en Bloqueado.
11. Enviado a contabilidad es terminal dentro del MVP.
12. El producto solo valida movimientos conocidos por el sistema y no garantiza detectar movimientos nunca registrados.

## Conclusión

Los casos de uso convierten el modelo de dominio y la máquina de estados en interacciones verificables. El flujo central es registrar, completar evidencia y autorización, validar, gestionar Alertas, corregir, revalidar, consolidar y ejecutar un control final obligatorio antes del envío interno a contabilidad.