# Impulsores de Arquitectura

**Versión:** v0.1

**Estado:** Línea base aprobada

**Fase:** 05 — Diseño técnico

**Producto:** Operational Close Validator

## 1. Propósito

Este documento identifica los requisitos, atributos de calidad, restricciones, riesgos y criterios de decisión que condicionan la arquitectura del MVP de Operational Close Validator.

Su propósito es proporcionar una base verificable para evaluar estilos arquitectónicos, tecnologías, mecanismos de persistencia, seguridad, despliegue y pruebas.

Este documento no selecciona todavía un stack tecnológico ni prescribe una implementación concreta.

## 2. Alcance

Este documento cubre únicamente los impulsores arquitectónicos del MVP aprobado en MVP Scope v0.3.

Quedan fuera de este análisis:

- funcionalidades excluidas del MVP;
- integraciones contables externas;
- conciliación bancaria o POS;
- inventario;
- configuración dinámica de reglas;
- reapertura de cierres enviados;
- decisiones tecnológicas todavía no aprobadas.

## 3. Fuentes aprobadas

Los impulsores de este documento se derivan de:

- Problem Statement v0.2.
- Product Thesis v0.2.
- Failure Mode Analysis v0.1.
- Validation Rules v0.2.
- Domain Model v0.3.
- State Machine v0.3.
- Use Cases v0.2.
- MVP Scope v0.3.
- ADR-0001.

## 4. Contexto técnico inicial

Operational Close Validator será una aplicación web que permitirá registrar, validar, corregir, consolidar y enviar internamente un Cierre Operativo.

El MVP deberá incluir:

- interfaz de usuario;
- backend o API;
- persistencia relacional;
- autenticación básica;
- reglas de validación;
- Alertas;
- máquinas de estados;
- pruebas automatizadas;
- configuración reproducible;
- despliegue público demostrable.

Esta descripción establece el contexto aprobado, pero no determina todavía la arquitectura ni el stack.

## 5. Requisitos funcionales arquitectónicamente significativos

### FR-ARCH-001 — Registro de Eventos Operativos

**Descripción:**
El sistema debe permitir registrar Eventos Operativos de tipo Ingreso, Egreso, Descuento y Anulación, asociados a un Cierre Operativo en estado Preparación o Bloqueado.

Los datos mínimos proporcionados para el registro son:

- tipo de evento;
- monto;
- fecha y hora de ocurrencia;
- responsable;
- motivo o descripción;
- Cierre Operativo relacionado.

El sistema debe registrar automáticamente:

- fecha y hora de registro;
- usuario autenticado que realizó la acción.

**Impacto arquitectónico:**
Obliga a definir una estructura de persistencia para eventos, una interfaz para su captura, y un mecanismo que garantice que el evento queda asociado correctamente al Cierre Operativo relacionado.

**Trazabilidad:**
- UC-002.
- Domain Model v0.3.
- MVP Scope v0.3.

### FR-ARCH-002 — Ejecución de reglas de validación

**Descripción:**
El sistema debe ejecutar las reglas VR-001, VR-002, VR-003 y VR-006 sobre cada Evento Operativo, determinando qué reglas aplican según el tipo de evento y sus datos asociados.

Cada validación debe registrar un Resultado de Validación con valores posibles: Satisfecha, Fallida.

**Impacto arquitectónico:**
Obliga a implementar un mecanismo de evaluación de reglas, persistencia de resultados, y un modelo que permita determinar qué reglas aplican a cada tipo de evento.

**Trazabilidad:**
- Validation Rules v0.2.
- UC-005.
- MVP Scope v0.3.

### FR-ARCH-003 — Gestión de Resultados de Validación

**Descripción:**
Cada Resultado de Validación debe registrar:
- regla aplicada;
- entidad evaluada;
- resultado;
- detalle;
- fecha y hora;
- vigencia.

Los resultados quedan no vigentes cuando cambian los datos relevantes que los originaron.

**Impacto arquitectónico:**
Obliga a diseñar una estructura de persistencia que relacione reglas, entidades evaluadas —Eventos Operativos o Cierres Operativos—, resultados y vigencia, y un mecanismo de invalidación por dependencia.

**Trazabilidad:**
- Domain Model v0.3.
- State Machine v0.3.
- Validation Rules v0.2: VR-008.
- UC-005.
- UC-007.
- UC-009.
- ADR-0001.

### FR-ARCH-004 — Gestión de Alertas

**Descripción:**
El sistema debe generar alertas cuando una validación falla o se detecta una condición bloqueante. Las alertas deben poder ser reconocidas, revisadas, resueltas o descartadas, conservando siempre trazabilidad de la acción realizada.

Estados incluidos: Activa, Reconocida, En revisión, Resuelta, Descartada.

**Impacto arquitectónico:**
Obliga a implementar un ciclo de vida completo de alertas, con reglas claras para cada transición, especialmente la resolución (que requiere revalidación exitosa) y el descarte (que requiere una acción autorizada, justificación, fecha y responsable).

**Trazabilidad:**
- UC-006.
- State Machine v0.3.
- MVP Scope v0.3.

### FR-ARCH-005 — Invalidación y revalidación

**Descripción:**

Cuando un Evento Operativo es modificado en datos relevantes, Evidencia de
Soporte o Autorización, los Resultados de Validación dependientes deben quedar
no vigentes.

Si el evento estaba Validado, pasa a Registrado porque debe ejecutar nuevamente
sus validaciones aplicables.

Si el Cierre Operativo relacionado estaba Validado, pasa a Bloqueado.

La consolidación afectada queda no vigente. La revalidación determina el nuevo
estado real del Evento Operativo.

**Impacto arquitectónico:**

Obliga a gestionar dependencias entre entidades, controlar la vigencia de
resultados y consolidaciones, y garantizar consistencia ante modificaciones.

**Trazabilidad:**

- UC-003.
- UC-004.
- UC-007.
- State Machine v0.3.
- MVP Scope v0.3.

### FR-ARCH-006 — Consolidación del Cierre Operativo

**Descripción:**

El sistema debe consolidar un Cierre Operativo calculando y conservando, como mínimo:

- totales por tipo de Evento Operativo;
- saldo inicial;
- saldo esperado;
- saldo real;
- diferencia;
- fecha y hora de consolidación;
- usuario responsable.

La consolidación solo puede completarse cuando todos los Eventos Operativos están Validados, no existen Alertas bloqueantes activas y todos los Resultados de Validación aplicables están vigentes y satisfechos.

La consolidación debe quedar no vigente cuando cambios posteriores afecten sus datos de origen.

**Impacto arquitectónico:**
Obliga a definir un proceso de cálculo, verificación de condiciones, persistencia de consolidaciones y gestión de vigencia.

**Trazabilidad:**
- UC-008.
- Domain Model v0.3.
- MVP Scope v0.3.

### FR-ARCH-007 — Control final y envío

**Descripción:**
VR-008 debe ejecutarse inmediatamente antes del envío. La ejecución de VR-008, el rechazo o aceptación del envío y la transición del cierre constituyen una única operación de negocio.

Si VR-008 falla:
- el envío se rechaza;
- el cierre pasa de Validado a Bloqueado;
- se conserva la causa y las entidades afectadas;
- se exige corrección y revalidación;
- se exige ejecutar nuevamente la consolidación antes de que el cierre pueda
  volver a Validado.

Si VR-008 es exitosa:

- VR-008 queda Satisfecha;
- se registran fecha, hora y responsable del envío;
- el cierre pasa a Enviado a contabilidad;
- no se admiten modificaciones posteriores dentro del MVP.

**Impacto arquitectónico:**
Obliga a implementar un mecanismo que garantice la atomicidad de la operación: validación final + transición + registro de envío, sin posibilidad de que el cierre quede en estado inconsistente.

**Trazabilidad:**
- VR-008.
- UC-009.
- ADR-0001.

### FR-ARCH-008 — Autenticación del usuario responsable

**Descripción:**
El sistema debe permitir que el usuario responsable preconfigurado se autentique mediante credenciales básicas. Las acciones protegidas (registrar, corregir, validar, consolidar, enviar) requieren autenticación.

**Impacto arquitectónico:**
Obliga a implementar un mecanismo de autenticación, gestión de sesiones y protección de rutas o acciones.

**Trazabilidad:**
- UC-001.
- MVP Scope v0.3.

## 6. Atributos de calidad

### QA-001 — Consistencia

**Fuente del estímulo:**
Una segunda sesión, pestaña o solicitud concurrente modifica un Evento Operativo mientras se procesa el envío.

**Estímulo:**
Un Evento Operativo cambia de estado o se modifica durante la ejecución de VR-008.

**Entorno:**
Operación normal del MVP, sin condiciones excepcionales de carga.

**Artefacto afectado:**
Cierre Operativo, Evento Operativo, Resultados de Validación, Consolidación.

**Respuesta esperada:**
La ejecución de VR-008, la transición del Cierre Operativo y el registro del envío deben completarse como una única operación consistente. Si una modificación concurrente invalida las condiciones evaluadas, la operación debe rechazarse sin registrar un envío exitoso.

**Medida de respuesta:**
Una prueba automatizada concurrente demuestra que ninguna modificación confirmada durante la operación permite registrar el cierre como Enviado a contabilidad utilizando resultados o una consolidación anteriores.

### QA-002 — Trazabilidad

**Escenario:**
Un usuario necesita investigar por qué un Cierre Operativo quedó Bloqueado o por qué una Alerta fue Resuelta o Descartada.

**Medida verificable:**
El sistema debe conservar fecha, hora, responsable, causa y detalle de:

- las transiciones relevantes de estado de los Eventos Operativos;
- las transiciones relevantes de estado de los Cierres Operativos;
- las transiciones relevantes de estado de las Alertas;
- cada Resultado de Validación y su vigencia;
- cada consolidación y su vigencia;
- cada intento de envío y su resultado.

Este requisito define la información auditable que debe poder reconstruirse; no prescribe event sourcing ni un mecanismo específico de almacenamiento.

### QA-003 — Seguridad

**Escenario:**
Un usuario no autenticado intenta registrar un evento, consolidar un cierre o enviarlo a contabilidad.

**Medida verificable:**
El sistema debe rechazar cualquier acción protegida sin autenticación válida. Las credenciales incorrectas deben ser rechazadas sin exponer datos del sistema.

### QA-004 — Mantenibilidad

**Escenario:**
Se requiere agregar una nueva regla de validación o modificar una regla existente en una versión futura, sin afectar el resto del sistema.

**Medida verificable:**
Modificar la implementación de una regla no debe requerir cambios en la interfaz de usuario ni en el mecanismo concreto de persistencia, salvo cuando cambien los datos requeridos por esa regla.

### QA-005 — Testabilidad

**Escenario:**
Un desarrollador necesita verificar que VR-008 rechaza correctamente un envío cuando existe una Alerta bloqueante activa.

**Medida verificable:**
Las reglas de validación y las transiciones de estado deben poder probarse automatizadamente sin depender de la interfaz de usuario. Debe ser posible ejecutar validaciones y transiciones en un entorno aislado con datos controlados.

### QA-006 — Usabilidad

**Escenario:**
Un usuario necesita identificar qué Eventos Operativos están pendientes, qué Alertas bloquean el cierre y qué acciones debe tomar para resolverlas.

**Medida verificable:**
Desde la vista del Cierre Operativo, el usuario puede consultar sin navegar a documentación externa:

- el estado actual del cierre;
- los Eventos Operativos que no están Validados;
- las Alertas bloqueantes activas;
- la causa de cada bloqueo;
- la acción necesaria para continuar.

### QA-007 — Desplegabilidad

**Escenario:**
Una persona externa al equipo de desarrollo necesita ejecutar o acceder a una versión demostrable del MVP.

**Medida verificable:**
El sistema debe contar con configuración documentada, variables de entorno definidas y un proceso de despliegue reproducible. Debe existir una versión desplegada públicamente.

## 7. Restricciones

### CON-001 — Aplicación web

El MVP debe implementarse como una aplicación web con interfaz de usuario y backend.

**Origen:**
MVP Scope v0.3.

### CON-002 — Persistencia relacional

El MVP debe utilizar persistencia relacional.

**Origen:**
MVP Scope v0.3.

### CON-003 — Catálogo fijo de reglas

Las reglas de validación del MVP pertenecen a un catálogo interno, fijo y versionado. El usuario no puede crear, editar, habilitar ni deshabilitar reglas.

**Origen:**
Validation Rules v0.2 y MVP Scope v0.3.

### CON-004 — Usuario único preconfigurado

El MVP utiliza un único usuario responsable preconfigurado con autenticación básica.

El término autenticación básica describe el alcance funcional reducido del MVP y no prescribe el uso del protocolo HTTP Basic Authentication.

**Origen:**
MVP Scope v0.3.

### CON-005 — Sin integración contable externa

El envío a contabilidad es interno dentro del sistema.

**Origen:**
UC-009 y MVP Scope v0.3.

### CON-006 — Estado Enviado terminal

Un cierre Enviado a contabilidad no puede modificarse dentro del MVP.

**Origen:**
State Machine v0.3 y ADR-0001.

### CON-007 — Despliegue demostrable

El sistema debe contar con configuración reproducible y despliegue público.

**Origen:**
MVP Scope v0.3.

## 8. Riesgos arquitectónicos

### RISK-001 — Condición de carrera durante el envío

**Descripción:**
Un Evento Operativo, Resultado de Validación, Alerta o consolidación podría cambiar mientras se ejecuta VR-008 y se registra el envío. Si el sistema no maneja este caso correctamente, podría enviar un cierre inconsistente.

**Impacto:**
Crítico.

**Probabilidad inicial:**
Media.

**Decisiones afectadas:**
- arquitectura de aplicación;
- persistencia;
- transacciones;
- control de concurrencia.

**Mitigación pendiente:**
El sistema debe garantizar que la evaluación de VR-008, la decisión de envío y el registro del resultado utilicen un estado coherente. Si se confirma una modificación conflictiva durante la operación, el envío debe rechazarse o reintentarse, pero nunca registrarse utilizando resultados o una consolidación no vigentes.

### RISK-002 — Inconsistencia entre estados y resultados

**Descripción:**
Un Evento Operativo podría quedar Validado mientras un Resultado de Validación asociado está fallido o no vigente, o un Cierre Operativo podría quedar Validado mientras contiene eventos no validados.

**Impacto:**
Crítico.

**Probabilidad inicial:**
Media.

**Mitigación pendiente:**
El sistema debe mantener invariantes del dominio, garantizando que el estado de las entidades sea consistente con sus resultados y validaciones.

### RISK-003 — Acoplamiento de reglas con interfaz o persistencia

**Descripción:**
Si las reglas de validación están acopladas a la interfaz de usuario o a la persistencia, cualquier cambio en estas áreas podría afectar la lógica de negocio, incrementando la complejidad del sistema.

**Impacto:**
Medio.

**Probabilidad inicial:**
Media.

**Mitigación pendiente:**
El diseño debe permitir evaluar y probar las reglas sin depender de la interfaz de usuario ni de una tecnología concreta de almacenamiento.

### RISK-004 — Pérdida de trazabilidad

**Descripción:**
Si el sistema no conserva información suficiente sobre transiciones, validaciones y acciones, no será posible investigar por qué un cierre quedó bloqueado o por qué un envío fue rechazado.

**Impacto:**
Alto.

**Probabilidad inicial:**
Baja.

**Mitigación pendiente:**
El sistema debe persistir los registros mínimos necesarios para reconstruir las decisiones de validación, las transiciones relevantes y los intentos de envío.

### RISK-005 — Complejidad excesiva para el MVP

**Descripción:**
Sobrediseñar la arquitectura, infraestructura o seguridad para casos de uso futuros podría retrasar la entrega del MVP sin aportar valor a la validación de la hipótesis de producto.

**Impacto:**
Alto.

**Probabilidad inicial:**
Media.

**Mitigación pendiente:**
Las decisiones técnicas deben estar justificadas por requisitos del MVP, no por proyecciones de crecimiento o funcionalidades futuras.

## 9. Criterios para evaluar alternativas técnicas

Las alternativas de arquitectura y stack se evaluarán con los siguientes criterios.

| ID | Criterio | Descripción | Prioridad |
|---|---|---|---|
| DEC-001 | Consistencia transaccional | Capacidad de mantener coherentes VR-008, envío y estado del cierre | Crítica |
| DEC-002 | Claridad del dominio | Capacidad de representar entidades, reglas e invariantes sin acoplamiento innecesario | Alta |
| DEC-003 | Testabilidad | Facilidad para probar reglas y transiciones de forma automatizada | Alta |
| DEC-004 | Simplicidad operativa | Esfuerzo requerido para ejecutar, desplegar y mantener el MVP | Alta |
| DEC-005 | Persistencia relacional | Adecuación para relaciones, restricciones y trazabilidad | Crítica |
| DEC-006 | Seguridad suficiente | Soporte para autenticación y protección de operaciones | Alta |
| DEC-007 | Despliegue público | Facilidad para publicar una versión demostrable | Alta |
| DEC-008 | Tiempo de implementación | Viabilidad dentro de la capacidad y calendario del proyecto | Alta |
| DEC-009 | Evolución | Capacidad de ampliar roles, reglas e integraciones después del MVP | Media |
| DEC-010 | Adecuación al aprendizaje | Valor técnico y profesional del stack para el proyecto | Media |

## 10. Priorización de impulsores

Las restricciones CON-001 a CON-007 son obligatorias para el MVP y no se clasifican por prioridad. Toda alternativa técnica debe cumplirlas.

### Prioridad crítica

- FR-ARCH-007 — Control final y envío.
- QA-001 — Consistencia.
- QA-002 — Trazabilidad.
- RISK-001 — Condición de carrera durante el envío.
- RISK-002 — Inconsistencia entre estados y resultados.

### Prioridad alta

- FR-ARCH-001 — Registro de Eventos Operativos.
- FR-ARCH-002 — Ejecución de reglas de validación.
- FR-ARCH-003 — Gestión de Resultados de Validación.
- FR-ARCH-004 — Gestión de Alertas.
- FR-ARCH-005 — Invalidación y revalidación.
- FR-ARCH-006 — Consolidación del Cierre Operativo.
- FR-ARCH-008 — Autenticación del usuario responsable.
- QA-003 — Seguridad.
- QA-005 — Testabilidad.
- QA-006 — Usabilidad.
- RISK-004 — Pérdida de trazabilidad.

### Prioridad media

- QA-004 — Mantenibilidad.
- QA-007 — Desplegabilidad.
- RISK-003 — Acoplamiento de reglas con interfaz o persistencia.
- RISK-005 — Complejidad excesiva para el MVP.

## 11. Preguntas abiertas

- ¿Cuántas sesiones, pestañas o solicitudes simultáneas del usuario preconfigurado debe soportar el MVP?
- ¿Qué volumen estimado de cierres y eventos debe considerarse?
- ¿Qué formatos, tamaño máximo y período de conservación tendrá la Evidencia de Soporte?
- ¿Qué información mínima de auditoría debe conservar cada acción?
- ¿Qué mecanismo protegerá la operación VR-008 más envío?
- ¿Qué requisitos impone el proveedor de despliegue?
- ¿Qué estrategia de respaldo requiere la demostración pública?

Las preguntas abiertas no deben resolverse mediante suposiciones ocultas. Cada respuesta que afecte la arquitectura deberá documentarse o convertirse en una decisión explícita.

## 12. Matriz de trazabilidad

| Impulsor | Fuente o derivación |
|---|---|
| QA-001 | VR-008, UC-009 y ADR-0001 |
| QA-002 | Domain Model v0.3, State Machine v0.3, UC-006, UC-007 y UC-009 |
| QA-003 | UC-001 y MVP Scope v0.3 |
| QA-004 | Validation Rules v0.2 y CON-003 |
| QA-005 | FR-ARCH-002, FR-ARCH-005, FR-ARCH-007 y MVP Scope v0.3 |
| QA-006 | Product Thesis v0.2, UC-006 y UC-008 |
| QA-007 | MVP Scope v0.3 y CON-007 |
| RISK-001 | QA-001, VR-008 y ADR-0001 |
| RISK-002 | Domain Model v0.3, invariantes 2 y 6 |
| RISK-003 | QA-004, QA-005 y FR-ARCH-002 |
| RISK-004 | QA-002 y UC-006 a UC-009 |
| RISK-005 | MVP Scope v0.3 y exclusiones aprobadas del MVP |

## 13. Decisiones que este documento habilita

Después de aprobar estos impulsores podrán evaluarse:

- ADR-0002: estilo arquitectónico de la aplicación;
- ADR-0003: stack tecnológico;
- ADR-0004: estrategia de persistencia;
- ADR-0005: estrategia de autenticación;
- diseño de API;
- diseño de datos;
- estrategia de pruebas;
- estrategia de despliegue.

## 14. Criterios de aprobación

Este documento podrá aprobarse cuando:

1. todos los impulsores estén trazados a una fuente aprobada;
2. no contenga decisiones tecnológicas disfrazadas de requisitos;
3. los atributos de calidad sean verificables;
4. las restricciones estén diferenciadas de las preferencias;
5. los riesgos críticos estén identificados;
6. los criterios de evaluación permitan comparar alternativas;
7. no amplíe el alcance aprobado del MVP.