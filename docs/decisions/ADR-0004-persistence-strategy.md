# ADR-0004: Estrategia de persistencia y control de concurrencia

**Estado:** Aceptada

**Estado documental:** Línea base aprobada

**Fecha:** 2026-07-16

**Producto:** Operational Close Validator

---

## 1. Contexto

Operational Close Validator debe persistir Eventos Operativos, Evidencias de Soporte, Autorizaciones, Resultados de Validación, Alertas, consolidaciones y el estado del Cierre Operativo.

Architecture Drivers v0.1 establece la persistencia relacional como restricción y considera crítica la consistencia de la operación final. ADR-0001 establece que una validación final fallida puede devolver un Cierre Operativo validado a bloqueado. ADR-0002 selecciona un monolito modular por capacidades con puertos y adaptadores, mantiene VR-008 dentro de Gestión del Cierre Operativo y exige que la validación final, la transición del cierre y el registro del envío compartan un límite local de consistencia.

ADR-0003 selecciona Java, Spring Boot, Spring MVC, Thymeleaf y Maven como stack del MVP, y mantiene fuera de su alcance la persistencia concreta, las migraciones, el aislamiento transaccional y el control de concurrencia.

Esta ADR completa esas decisiones mediante PostgreSQL, Spring Data JPA con Hibernate, Flyway y un protocolo de bloqueo pesimista por Cierre Operativo. La selección tecnológica se limita a Infraestructura y no autoriza dependencias del Dominio hacia Spring, JPA, Hibernate, SQL ni el driver de base de datos.

---

## 2. Problema de decisión

¿Qué estrategia de persistencia y control de concurrencia permite:

- conservar el Dominio independiente de la tecnología;
- mantener trazabilidad relacional;
- ejecutar los casos de uso bajo límites transaccionales explícitos;
- impedir que VR-008 envíe un cierre basado en datos no vigentes;
- evitar complejidad distribuida innecesaria;
- mantener una implementación viable para el MVP?

---

## 3. Alcance de la decisión

### Esta ADR decide

- motor relacional;
- adaptador de persistencia y ORM;
- herramienta de migraciones;
- topología lógica de persistencia del MVP;
- propiedad de los datos por módulo;
- dirección de dependencias hacia persistencia;
- ubicación de los límites transaccionales;
- API concreta que materializa las transacciones;
- protocolo general de bloqueo y orden de adquisición;
- mecanismo de bloqueo pesimista del Cierre Operativo;
- nivel de aislamiento inicial;
- comportamiento transaccional de VR-008;
- política del MVP ante bloqueos mutuos, timeouts y conflictos;
- uso de restricciones relacionales;
- estrategia general de trazabilidad persistente;
- estrategia de pruebas contra el motor real;
- política general de versiones soportadas.

### Esta ADR no decide

- versión mayor exacta de PostgreSQL;
- versión exacta de Spring Data JPA, Hibernate o Flyway;
- nombres físicos de tablas y columnas;
- tipos físicos definitivos de identificadores;
- índices concretos;
- esquema detallado de datos;
- almacenamiento físico de archivos de Evidencia de Soporte;
- proveedor de base de datos;
- política de copias de seguridad;
- credenciales y secretos;
- mecanismo de autenticación y sesión;
- contratos HTTP.

Las versiones exactas se fijarán al iniciar la implementación mediante Maven y configuración reproducible. El esquema detallado se documentará en `data-model.md`.

---

## 4. Restricciones y decisiones heredadas

### 4.1. Restricciones obligatorias

- La persistencia del MVP es relacional.
- La aplicación es una sola unidad desplegable.
- Gestión del Cierre Operativo e Identidad y Acceso son módulos internos, no servicios independientes.
- VR-008 permanece dentro de Gestión del Cierre Operativo.
- El estado Enviado a contabilidad es terminal dentro del MVP.
- El catálogo de reglas es interno, fijo y versionado.
- La solución debe conservar trazabilidad de estados, validaciones y responsables.
- La solución debe ser reproducible y públicamente desplegable.

### 4.2. Reglas heredadas de ADR-0002

- El Dominio no depende de ORM, drivers ni modelos persistentes.
- Aplicación define los puertos de salida.
- Infraestructura implementa los adaptadores.
- Los casos de uso definen sus límites de consistencia.
- Infraestructura materializa las transacciones.
- Los módulos no acceden directamente a las estructuras persistentes internas de otro módulo.
- VR-008, la transición del cierre y el registro del envío comparten un mismo límite de consistencia.
- La base de datos no se implementa como un servicio de aplicación independiente.

---

## 5. Impulsores de la decisión

| ID | Criterio | Descripción | Prioridad | Peso |
|---|---|---|---|---:|
| PS-001 | Consistencia de VR-008 | Impedir envíos basados en información modificada durante la validación final | Crítica | 5 |
| PS-002 | Integridad relacional | Aplicar relaciones y restricciones estructurales en la base de datos | Crítica | 5 |
| PS-003 | Independencia del Dominio | Evitar dependencias del Dominio hacia ORM, SQL o drivers | Alta | 3 |
| PS-004 | Límite transaccional explícito | Permitir que Aplicación defina la atomicidad de cada caso de uso | Alta | 3 |
| PS-005 | Trazabilidad | Conservar estados, responsables, fechas y causas relevantes | Alta | 3 |
| PS-006 | Testabilidad | Sustituir puertos en pruebas y probar adaptadores por separado | Alta | 3 |
| PS-007 | Simplicidad operativa | Evitar bases distribuidas y coordinación innecesaria | Alta | 3 |
| PS-008 | Prevención de bloqueos mutuos | Establecer un orden de adquisición de bloqueos verificable | Alta | 3 |
| PS-009 | Evolución controlada | Aplicar cambios de esquema mediante migraciones versionadas | Alta | 3 |
| PS-010 | Rendimiento para el MVP | Mantener operaciones suficientemente rápidas bajo baja concurrencia | Media | 1 |

### 5.1. Escala

- 1 — Muy deficiente.
- 2 — Deficiente.
- 3 — Aceptable.
- 4 — Buena.
- 5 — Muy buena.

---

## 6. Alternativas consideradas

### Alternativa A — Acceso directo mediante ORM y transacciones implícitas

#### Descripción

Los controladores o servicios utilizan directamente modelos del ORM. Las transacciones se abren implícitamente por solicitud o mediante llamadas locales cuando el framework lo requiere.

No existe un protocolo común para impedir modificaciones concurrentes durante VR-008.

#### Ventajas

- Menor cantidad inicial de abstracciones.
- Implementación rápida para operaciones simples.
- Integración directa con las herramientas del framework.

#### Desventajas

- El ORM puede filtrarse hacia Aplicación o Dominio.
- Los límites transaccionales quedan ligados al framework.
- VR-008 puede leer información y enviarla después de una modificación concurrente.
- Es difícil verificar que todas las operaciones relacionadas participen en la misma transacción.

#### Riesgos

- Incumplimiento de RISK-001.
- Dependencia tecnológica en el núcleo.
- Comportamientos diferentes entre casos de uso.
- Pruebas de negocio condicionadas por persistencia real.

---

### Alternativa B — Puertos de persistencia con concurrencia optimista global

#### Descripción

Aplicación utiliza repositorios y un puerto transaccional. Cada Cierre Operativo mantiene una versión.

Toda modificación relevante debe incrementar esa versión. Los comandos incluyen la versión esperada y fallan cuando la versión persistida cambió.

VR-008 valida los datos y realiza una actualización condicional sobre la versión del cierre antes de registrar el envío.

#### Ventajas

- No mantiene bloqueos durante la evaluación.
- Detecta comandos basados en una versión desactualizada.
- Conserva puertos y adaptadores.
- Funciona bien cuando los conflictos son poco frecuentes.

#### Desventajas

- Toda modificación de entidades dependientes debe incrementar correctamente la versión del cierre.
- Un olvido en una ruta de escritura debilita la protección.
- La coordinación de versiones entre tablas dependientes aumenta la complejidad.
- La lógica de reintentos debe quedar claramente definida.

#### Riesgos

- Una mutación que no actualice la versión puede permitir un envío no vigente.
- Mayor dificultad para demostrar que todas las escrituras respetan el protocolo.
- Conflictos tardíos después de ejecutar validaciones costosas.

---

### Alternativa C — Base relacional única con puertos, transacciones explícitas y bloqueo por cierre

#### Descripción

La aplicación utiliza una sola base de datos relacional.

Aplicación define repositorios y un puerto de ejecución transaccional. Infraestructura implementa esos contratos.

Toda operación que pueda modificar información relevante para un Cierre Operativo adquiere primero un bloqueo exclusivo sobre la fila del cierre correspondiente. Después puede leer o modificar sus entidades dependientes.

VR-008 adquiere el mismo bloqueo, recarga la información necesaria, ejecuta la validación y persiste el resultado dentro de una sola transacción.

#### Ventajas

- Proporciona un único punto de serialización por Cierre Operativo.
- Evita que una modificación relevante se intercale con VR-008.
- El protocolo es verificable mediante pruebas de integración.
- Mantiene el Dominio independiente de persistencia.
- Evita coordinación distribuida.
- Se ajusta al bajo nivel de concurrencia esperado para el MVP.

#### Desventajas

- Las operaciones sobre un mismo cierre se ejecutan secuencialmente.
- Un caso de uso largo puede mantener el bloqueo más tiempo del necesario.
- Todas las rutas de escritura deben respetar el protocolo.
- El motor y adaptador seleccionados deben soportar bloqueo de filas con semántica verificable.

#### Riesgos

- Bloqueos mutuos cuando no se respeta el orden aprobado.
- Esperas excesivas si se ejecuta trabajo externo dentro de la transacción.
- Dependencia de diferencias semánticas entre motores relacionales.

---

### Alternativa D — Persistencia separada por módulo

#### Descripción

Gestión del Cierre Operativo e Identidad y Acceso utilizan almacenes independientes. Las operaciones que atraviesan módulos se coordinan mediante mensajes o llamadas entre componentes.

#### Ventajas

- Aislamiento físico de datos.
- Posibilidad futura de despliegue o escalamiento independiente.

#### Desventajas

- Introduce coordinación distribuida.
- Complica transacciones, pruebas y trazabilidad.
- No aporta valor proporcional al alcance del MVP.
- Contradice la simplicidad operativa aprobada.

#### Riesgos

- Estados inconsistentes entre almacenes.
- Dificultad para reconstruir una operación completa.
- Complejidad equivalente a una arquitectura distribuida sin necesidad aprobada.

---

## 7. Evaluación comparativa

### 7.1. Matriz de puntuación

| Criterio | Peso | A. ORM directo | B. Optimista | C. Bloqueo por cierre | D. Persistencia separada |
|---|---:|---:|---:|---:|---:|
| PS-001 — Consistencia de VR-008 | 5 | 2 | 4 | 5 | 1 |
| PS-002 — Integridad relacional | 5 | 5 | 5 | 5 | 3 |
| PS-003 — Independencia del Dominio | 3 | 2 | 5 | 5 | 4 |
| PS-004 — Límite transaccional explícito | 3 | 2 | 5 | 5 | 2 |
| PS-005 — Trazabilidad | 3 | 3 | 4 | 5 | 3 |
| PS-006 — Testabilidad | 3 | 2 | 5 | 5 | 2 |
| PS-007 — Simplicidad operativa | 3 | 5 | 4 | 4 | 1 |
| PS-008 — Prevención de bloqueos mutuos | 3 | 3 | 4 | 4 | 2 |
| PS-009 — Evolución controlada | 3 | 4 | 5 | 5 | 3 |
| PS-010 — Rendimiento para el MVP | 1 | 5 | 5 | 4 | 3 |

### 7.2. Puntuación ponderada

| Criterio | Peso | A. ORM directo | B. Optimista | C. Bloqueo por cierre | D. Persistencia separada |
|---|---:|---:|---:|---:|---:|
| PS-001 | 5 | 10 | 20 | 25 | 5 |
| PS-002 | 5 | 25 | 25 | 25 | 15 |
| PS-003 | 3 | 6 | 15 | 15 | 12 |
| PS-004 | 3 | 6 | 15 | 15 | 6 |
| PS-005 | 3 | 9 | 12 | 15 | 9 |
| PS-006 | 3 | 6 | 15 | 15 | 6 |
| PS-007 | 3 | 15 | 12 | 12 | 3 |
| PS-008 | 3 | 9 | 12 | 12 | 6 |
| PS-009 | 3 | 12 | 15 | 15 | 9 |
| PS-010 | 1 | 5 | 5 | 4 | 3 |
| **Total** |  | **103** | **146** | **153** | **74** |

### 7.3. Fundamento de la evaluación

La alternativa A satisface la persistencia relacional, pero no proporciona un protocolo suficiente para RISK-001 y debilita la dirección de dependencias aprobada.

La alternativa B es válida, pero depende de que todas las mutaciones actualicen correctamente una versión compartida del cierre. La cantidad de entidades dependientes aumenta el riesgo de omitir una actualización de versión.

La alternativa C ofrece un protocolo directo: todas las escrituras relevantes se serializan mediante el Cierre Operativo. La baja concurrencia esperada hace aceptable el costo de serialización por cierre.

La alternativa D introduce complejidad distribuida sin un impulsor que la justifique.

---

## 8. Decisión

Se adopta:

**Alternativa C — Base relacional única con puertos, transacciones explícitas y bloqueo por Cierre Operativo.**

### 8.1. Topología de persistencia

El MVP utilizará:

- una sola base de datos relacional;
- una sola fuente de datos lógica, administrada mediante un pool de conexiones;
- propiedad lógica de datos por módulo;
- transacciones locales;
- ausencia de bases de datos independientes por módulo;
- ausencia de coordinación distribuida.

La base de datos es una dependencia de Infraestructura y no constituye un módulo de negocio.

### 8.2. Propiedad por módulo

Gestión del Cierre Operativo es propietario de:

- Cierres Operativos;
- Eventos Operativos;
- Evidencias de Soporte y sus referencias;
- Autorizaciones;
- Resultados de Validación;
- Alertas;
- consolidaciones;
- registros internos de envío;
- trazabilidad de estados y acciones operativas.

Identidad y Acceso es propietario de:

- usuario preconfigurado;
- credenciales o referencia segura a credenciales;
- información mínima necesaria para autenticación y sesión.

Un módulo no modifica directamente datos propiedad del otro módulo.

Cuando un caso de uso necesita identidad, recibe el principal autenticado mediante el contrato aprobado en ADR-0002.

### 8.3. Puertos y adaptadores

Aplicación define únicamente los contratos requeridos por sus casos de uso, incluyendo cuando corresponda:

- repositorios;
- `TransactionRunner` como puerto de ejecución transaccional;
- reloj;
- almacenamiento de referencia de Evidencias de Soporte;
- contexto del principal autenticado.

Infraestructura implementa estos contratos.

`TransactionRunner` se materializa mediante `TransactionTemplate` y el administrador de transacciones configurado por Spring. El caso de uso determina qué operación debe ser atómica; Infraestructura abre, confirma o revierte la transacción.

Los controladores, el Dominio y las vistas Thymeleaf no abren ni confirman transacciones.

El Dominio no recibe:

- sesiones de ORM;
- entidades persistentes;
- consultas SQL;
- conexiones;
- transacciones;
- anotaciones de persistencia;
- tipos específicos del driver.

### 8.4. Límite transaccional

Cada caso de uso que modifica estado se ejecuta dentro de una transacción explícita controlada por Aplicación y materializada por Infraestructura.

La transacción debe:

1. comenzar antes de adquirir el bloqueo del cierre;
2. adquirir el bloqueo requerido;
3. recargar los datos necesarios;
4. ejecutar las reglas del Dominio;
5. persistir todos los cambios;
6. registrar la trazabilidad requerida;
7. confirmar una sola vez;
8. revertir completamente ante cualquier error.

No se permiten confirmaciones parciales dentro de un mismo caso de uso.

### 8.5. Puerta de concurrencia por Cierre Operativo

Toda operación que pueda modificar información relevante para VR-008 debe adquirir primero un bloqueo exclusivo sobre el Cierre Operativo correspondiente.

Esto incluye modificaciones sobre:

- Eventos Operativos;
- Evidencias de Soporte;
- Autorizaciones;
- Resultados de Validación;
- Alertas bloqueantes;
- consolidación;
- estado del cierre;
- registro interno del envío.

El Cierre Operativo actúa como puerta de concurrencia, aunque la información esté distribuida en varias tablas.

### 8.6. Orden de bloqueo

El orden obligatorio es:

```text
1. Cierre Operativo
2. Entidades dependientes del cierre
3. Registros de trazabilidad
```

Cuando una operación excepcional requiera más de un Cierre Operativo, los cierres se bloquean en orden ascendente por identificador persistente.

El MVP debe evitar casos de uso que modifiquen más de un cierre dentro de la misma transacción.

### 8.7. Protocolo de VR-008

El caso de uso de envío ejecuta este protocolo:

```text
1. Iniciar transacción.
2. Bloquear el Cierre Operativo.
3. Rechazar la operación si el cierre ya está Enviado a contabilidad.
4. Recargar Eventos Operativos, Resultados de Validación,
   Alertas bloqueantes y consolidación.
5. Ejecutar VR-008 con la información recargada.
6. Si VR-008 falla:
   a. rechazar el envío;
   b. registrar el Resultado de Validación fallido;
   c. registrar causa y entidades afectadas;
   d. cambiar el cierre de Validado a Bloqueado;
   e. persistir la trazabilidad;
   f. confirmar la transacción sin registrar un envío exitoso.
7. Si VR-008 se satisface:
   a. registrar el resultado vigente y satisfecho;
   b. registrar fecha, hora y responsable;
   c. crear el registro interno del envío;
   d. cambiar el cierre a Enviado a contabilidad;
   e. persistir la trazabilidad.
8. Confirmar la transacción.
```

Mientras la transacción conserva el bloqueo del cierre, ninguna operación que respete esta ADR puede modificar información relevante para VR-008.

Un fallo de VR-008 deja el cierre Bloqueado y exige corrección, revalidación y una nueva consolidación antes de que pueda volver a Validado y realizarse un nuevo intento de envío.

### 8.8. Trabajo prohibido dentro de la transacción

No se ejecutan dentro de la transacción:

- llamadas de red a servicios externos;
- envío de correos;
- procesamiento prolongado de archivos;
- tareas no necesarias para decidir o persistir el caso de uso;
- esperas de interacción del usuario.

El envío del MVP es interno y se representa mediante persistencia local.

### 8.9. Conflictos y fallas transitorias

Un timeout de bloqueo, bloqueo mutuo o conflicto transaccional:

- revierte completamente la operación;
- no deja estados parciales;
- se representa como conflicto temporal de concurrencia;
- se registra para diagnóstico sin exponer detalles internos al usuario;
- no se reintenta automáticamente dentro del MVP.

El usuario puede repetir explícitamente la operación después de recibir el conflicto.

El tiempo máximo de espera por bloqueo debe ser finito, externalizado mediante configuración y validado en pruebas. Su valor inicial se fijará durante la implementación y no se codificará en el Dominio.

### 8.10. Restricciones relacionales

La base de datos debe aplicar, como mínimo:

- claves primarias;
- claves foráneas;
- restricciones de nulabilidad;
- unicidad donde exista una identidad de negocio inequívoca;
- restricciones para impedir más de un envío exitoso por cierre;
- restricciones estructurales sobre relaciones obligatorias;
- índices necesarios para localizar cierres y entidades dependientes.

Las reglas de negocio complejas permanecen en el Dominio.

Una restricción de base de datos puede reforzar una invariante, pero no sustituye su representación y prueba en el Dominio cuando la regla pertenece al negocio.

### 8.11. Trazabilidad persistente

La persistencia conserva:

- estado actual de cada entidad;
- fecha y hora de creación;
- fecha y hora de modificación relevante;
- responsable autenticado;
- transiciones relevantes;
- Resultados de Validación;
- causa de bloqueo o rechazo;
- justificación de descarte cuando corresponda;
- registro interno del envío.

La trazabilidad se implementa mediante registros explícitos de auditoría o transición junto con el estado actual.

Esta decisión no prescribe event sourcing.

### 8.12. Migraciones

Los cambios de esquema deben:

- estar versionados en Git;
- ejecutarse de forma determinista;
- formar parte del proceso reproducible de despliegue;
- incluir datos iniciales versionados cuando correspondan;
- evitar cambios destructivos automáticos;
- ser revisados junto con el código que depende de ellos;
- conservar compatibilidad con la línea base documental aplicable.

Flyway es el único mecanismo autorizado para crear y evolucionar el esquema.

Las migraciones se almacenan como scripts SQL versionados bajo `classpath:db/migration`.

Hibernate no crea ni actualiza el esquema. Se configura únicamente para validar que el mapeo sea compatible con el esquema migrado.

No se combinan Flyway, `schema.sql`, `data.sql` y generación automática de DDL para administrar el mismo esquema.

El catálogo fijo de reglas y los datos iniciales estrictamente necesarios se incorporan mediante migraciones reproducibles. La inicialización de credenciales se resolverá conforme a ADR-0005.

### 8.13. Evidencias de Soporte

La base relacional persiste:

- metadatos de la Evidencia de Soporte;
- relación con el Evento Operativo;
- estado de vigencia;
- responsable y fechas;
- referencia abstracta al contenido físico.

Esta ADR no selecciona dónde se almacena el contenido físico.

El contenido binario no se incorpora al Dominio ni se pasa a las reglas como un tipo dependiente de Infraestructura.

---

## 9. Selección tecnológica

### 9.1. Componentes seleccionados

| Componente | Decisión |
|---|---|
| Motor relacional | PostgreSQL |
| Acceso a datos | Spring Data JPA |
| Implementación JPA | Hibernate administrado por Spring Boot |
| Driver | Driver JDBC oficial de PostgreSQL |
| Migraciones | Flyway con scripts SQL versionados |
| API transaccional | `TransactionRunner` de Aplicación implementado con `TransactionTemplate` |
| Bloqueo del cierre | `PESSIMISTIC_WRITE`, materializado como bloqueo de fila |
| Nivel de aislamiento inicial | `READ_COMMITTED` |
| Política de reintentos | Sin reintentos automáticos en el MVP |
| Validación de esquema | Hibernate `validate`; generación automática deshabilitada |
| Pruebas de persistencia | Testcontainers con PostgreSQL |
| Gestión de versiones | Maven y versiones administradas por la línea de Spring Boot seleccionada |

### 9.2. Evaluación del adaptador de persistencia

Se evaluaron tres alternativas dentro del stack Java seleccionado:

| Criterio | Peso | Spring Data JPA | Spring Data JDBC | jOOQ |
|---|---:|---:|---:|---:|
| Relaciones y navegación persistente | 3 | 5 | 3 | 4 |
| Bloqueo pesimista | 5 | 5 | 4 | 5 |
| Productividad para el MVP | 3 | 5 | 4 | 3 |
| Independencia del Dominio | 5 | 4 | 5 | 5 |
| Pruebas de integración | 3 | 5 | 5 | 5 |
| Complejidad de configuración | 3 | 4 | 4 | 3 |
| **Total ponderado** |  | **102** | **93** | **95** |

Spring Data JPA se selecciona por:

- soporte directo de metadatos de bloqueo en métodos de repositorio;
- integración con transacciones de Spring;
- productividad para un modelo relacional con múltiples asociaciones;
- ecosistema de pruebas e integración;
- experiencia transferible para un proyecto de portafolio.

La puntuación de independencia del Dominio no es máxima porque JPA puede filtrarse hacia el núcleo si se utilizan entidades persistentes como entidades de negocio. Esta ADR lo prohíbe expresamente.

### 9.3. Separación entre Dominio y JPA

La implementación debe mantener dos representaciones cuando exista persistencia:

```text
Dominio
Entidades, valores, reglas e invariantes
        ↕ mapeo explícito
Infraestructura
Entidades JPA, repositorios Spring Data y consultas
```

Las entidades del Dominio:

- no contienen `@Entity`;
- no contienen `@Table`;
- no contienen relaciones JPA;
- no contienen tipos de Hibernate;
- no conocen `EntityManager`;
- no se exponen directamente a Thymeleaf.

Las entidades JPA permanecen en Infraestructura.

Los adaptadores convierten entre modelos persistentes y objetos del Dominio.

### 9.4. Configuración obligatoria de JPA

La implementación debe:

- desactivar Open EntityManager in View;
- evitar carga diferida desde controladores o vistas;
- evitar cascadas amplias no justificadas;
- declarar explícitamente las relaciones persistentes;
- limitar los repositorios Spring Data a Infraestructura;
- configurar Hibernate para validar, no crear ni actualizar, el esquema;
- evitar que excepciones de JPA o Hibernate atraviesen los puertos de Aplicación sin traducción.

### 9.5. PostgreSQL y aislamiento

PostgreSQL es el motor relacional único del MVP.

Se utiliza inicialmente `READ_COMMITTED` porque la consistencia de VR-008 se obtiene mediante el protocolo común de bloqueo:

```text
1. Iniciar transacción.
2. Bloquear la fila del Cierre Operativo.
3. Recargar la información dependiente.
4. Ejecutar reglas y persistir efectos.
5. Confirmar o revertir.
```

El nivel de aislamiento no sustituye el protocolo. Toda escritura relevante debe bloquear primero el mismo Cierre Operativo.

Un cambio a `REPEATABLE_READ` o `SERIALIZABLE` requiere:

- evidencia de una anomalía no resuelta por el protocolo;
- pruebas contra PostgreSQL;
- evaluación de reintentos;
- actualización de esta ADR o una ADR que la sustituya.

### 9.6. Bloqueo del Cierre Operativo

El adaptador de persistencia debe exponer una operación equivalente a:

```text
findCloseByIdForUpdate(closeId)
```

La implementación utiliza bloqueo pesimista de escritura sobre la fila del cierre.

Puede materializarse mediante:

- `@Lock(LockModeType.PESSIMISTIC_WRITE)` en un repositorio de Infraestructura; o
- una consulta equivalente ejecutada mediante `EntityManager`.

La elección entre ambas formas es interna al adaptador y no modifica el contrato de Aplicación.

El bloqueo se adquiere después de iniciar la transacción y se conserva hasta confirmar o revertir.

### 9.7. Migraciones con Flyway

Flyway administra todo el esquema relacional.

Las migraciones:

- se versionan en Git;
- utilizan SQL revisable;
- se ejecutan en orden determinista;
- fallan el arranque cuando una migración obligatoria no puede aplicarse;
- se prueban contra PostgreSQL;
- no se modifican después de haber sido aplicadas en un entorno compartido;
- incorporan restricciones, índices y datos iniciales requeridos.

No se utiliza generación automática de esquema como mecanismo de producción.

### 9.8. Pruebas contra PostgreSQL

Las pruebas de persistencia y concurrencia utilizan PostgreSQL mediante Testcontainers.

No se sustituye PostgreSQL por una base en memoria para validar:

- bloqueo de filas;
- aislamiento;
- restricciones;
- migraciones;
- comportamiento ante bloqueo mutuo;
- unicidad del envío;
- rollback.

La versión mayor usada por Testcontainers debe coincidir con la versión mayor fijada para desarrollo y despliegue.

### 9.9. Política de versiones

Al iniciar la implementación se seleccionará:

- una versión mayor soportada de PostgreSQL;
- una línea soportada de Spring Boot;
- las versiones compatibles de Spring Data JPA, Hibernate y Flyway;
- una imagen de PostgreSQL fijada para desarrollo y pruebas.

Las versiones exactas quedarán registradas en:

- `pom.xml`;
- configuración local reproducible;
- automatización de integración continua;
- documentación de desarrollo;
- estrategia de despliegue.


---

## 10. Reglas de implementación

1. El Dominio no importa dependencias de persistencia.
2. Los repositorios se definen en Aplicación cuando un caso de uso los requiere.
3. Infraestructura implementa los repositorios.
4. Los controladores no abren ni confirman transacciones de negocio.
5. Cada comando de escritura se ejecuta bajo un límite transaccional explícito.
6. Toda mutación relevante bloquea primero el Cierre Operativo.
7. VR-008 recarga la información después de adquirir el bloqueo.
8. VR-008 no utiliza objetos leídos antes del inicio de su transacción.
9. El envío exitoso y el cambio a Enviado a contabilidad se confirman juntos.
10. Un fallo de VR-008 no crea un registro de envío exitoso.
11. Un cierre Enviado a contabilidad no admite nuevas mutaciones.
12. El registro interno del envío tiene unicidad por cierre.
13. No se realizan llamadas externas dentro de la transacción.
14. Las migraciones forman parte del repositorio.
15. Las pruebas de integración utilizan el mismo motor relacional seleccionado para producción.
16. Las pruebas deben demostrar que una modificación concurrente no puede intercalarse con VR-008.
17. Las pruebas deben demostrar que un rollback no deja cambios parciales.
18. Las pruebas deben demostrar que dos solicitudes de envío no producen dos registros exitosos.
19. Los módulos no modifican directamente las tablas propiedad de otro módulo.
20. La persistencia del contenido físico de Evidencias de Soporte se decide por separado.
21. Las entidades JPA permanecen en Infraestructura.
22. `spring.jpa.open-in-view` permanece deshabilitado.
23. Hibernate valida el esquema, pero no lo crea ni lo actualiza.
24. Flyway es el único mecanismo de evolución del esquema.
25. Las pruebas de bloqueo y concurrencia utilizan PostgreSQL mediante Testcontainers.
26. No existen reintentos automáticos de comandos de negocio dentro del MVP.
27. Las excepciones de persistencia se traducen antes de atravesar los puertos de Aplicación.

---

## 11. Consecuencias positivas

- VR-008 dispone de una puerta local y verificable de concurrencia.
- Las escrituras relacionadas con un cierre quedan serializadas.
- El Dominio permanece independiente del ORM y del motor.
- Las transacciones corresponden a casos de uso y no a solicitudes HTTP implícitas.
- La persistencia relacional refuerza integridad y trazabilidad.
- La estrategia evita transacciones distribuidas.
- El comportamiento puede probarse contra el motor real.
- La estructura es compatible con los stacks evaluados en ADR-0003.

---

## 12. Consecuencias y costos

- Todas las rutas de escritura deben respetar el bloqueo del cierre.
- Las operaciones concurrentes sobre el mismo cierre pueden esperar.
- Las transacciones deben mantenerse breves.
- Es necesario probar la semántica real del motor y adaptador seleccionados.
- Los bloqueos mutuos siguen siendo posibles si se viola el orden aprobado.
- Los adaptadores requieren pruebas de integración adicionales.
- La solución incorpora el costo de mantener mapeos entre Dominio y entidades JPA.
- El esquema detallado todavía debe documentarse en `data-model.md`.
- La estrategia de almacenamiento físico de Evidencias de Soporte requiere una decisión posterior.

---

## 13. Alternativas descartadas

### Acceso directo mediante ORM

Se descarta porque:

- debilita la independencia del Dominio;
- hace menos explícitos los límites transaccionales;
- no resuelve por sí mismo RISK-001;
- dificulta verificar que VR-008 y el envío se ejecuten bajo una sola operación.

### Concurrencia exclusivamente optimista

Se descarta como estrategia principal porque:

- exige actualizar correctamente una versión compartida desde todas las rutas;
- la cantidad de entidades dependientes aumenta el riesgo de omisión;
- el protocolo de bloqueo por cierre es más directo para el nivel de concurrencia esperado.

La versión del cierre puede utilizarse adicionalmente para detectar comandos obsoletos, pero no reemplaza la puerta de bloqueo aprobada.

### Persistencia independiente por módulo

Se descarta porque:

- introduce coordinación distribuida;
- no mejora el MVP de manera proporcional;
- complica VR-008;
- contradice la simplicidad operativa y la unidad desplegable aprobadas.

---

## 14. Criterios de cumplimiento

La decisión se considera correctamente implementada cuando:

1. existe una sola base relacional para la aplicación;
2. el Dominio no referencia ORM, SQL ni drivers;
3. Aplicación define repositorios y el límite transaccional;
4. Infraestructura implementa persistencia y transacciones;
5. toda mutación relevante adquiere primero el bloqueo del cierre;
6. el orden de bloqueo está documentado y probado;
7. VR-008 recarga datos después del bloqueo;
8. una modificación concurrente no puede producir un envío basado en datos no vigentes;
9. un error revierte todos los cambios del caso de uso;
10. no pueden existir dos envíos exitosos para el mismo cierre;
11. el estado Enviado a contabilidad impide modificaciones posteriores;
12. las restricciones relacionales estructurales están activas;
13. las migraciones están versionadas y son reproducibles;
14. la trazabilidad registra responsable, fechas, resultados y causas;
15. las pruebas del Dominio no requieren base de datos;
16. los adaptadores tienen pruebas contra el motor relacional real;
17. no se realizan llamadas externas dentro de la transacción;
18. los módulos respetan la propiedad lógica de datos;
19. PostgreSQL, Spring Data JPA, Hibernate y Flyway están configurados conforme a la sección 9;
20. ADR-0003 está aceptada antes de aceptar esta ADR;
21. Open EntityManager in View está deshabilitado;
22. Hibernate no genera automáticamente el esquema;
23. las migraciones de Flyway se ejecutan correctamente desde una base vacía;
24. las pruebas de persistencia y concurrencia utilizan PostgreSQL mediante Testcontainers.

---

## 15. Documentos relacionados

- Architecture Drivers v0.1 — Impulsores de Arquitectura.
- ADR-0001 — La validación final puede devolver un cierre validado a bloqueado.
- ADR-0002 — Estilo arquitectónico de la aplicación.
- ADR-0003 — Stack tecnológico del MVP.
- Validation Rules v0.2 — Reglas de Validación.
- Domain Model v0.3 — Modelo de Dominio.
- State Machine v0.3 — Máquina de Estados.
- Use Cases v0.2 — Casos de Uso.
- MVP Scope v0.3 — Alcance del MVP.

---

## 16. Control de cambios

Modificar esta decisión requiere:

1. identificar el impulsor o supuesto que cambió;
2. evaluar el impacto sobre VR-008, integridad y concurrencia;
3. repetir las pruebas de concurrencia contra el motor real;
4. registrar una ADR que sustituya o modifique esta decisión;
5. actualizar el modelo de datos y los adaptadores;
6. revisar migraciones y compatibilidad;
7. incorporar el cambio mediante revisión.

---

## 17. Conclusión

Operational Close Validator utilizará:

- PostgreSQL como motor relacional;
- Spring Data JPA con Hibernate dentro de Infraestructura;
- Flyway como único mecanismo de migraciones;
- `TransactionRunner` implementado mediante `TransactionTemplate`;
- aislamiento inicial `READ_COMMITTED`;
- bloqueo pesimista de escritura sobre el Cierre Operativo;
- Testcontainers con PostgreSQL para pruebas de persistencia y concurrencia.

Toda escritura que pueda afectar VR-008 deberá adquirir primero el bloqueo del cierre. El caso de uso de envío adquirirá el mismo bloqueo, recargará la información, ejecutará VR-008 y persistirá la transición y el registro del envío dentro de una sola transacción.

El Dominio permanecerá libre de Spring, JPA, Hibernate, SQL y tipos del driver.

Esta decisión concuerda con:

- el límite local de consistencia aprobado en ADR-0002;
- el stack Java y Spring Boot seleccionado en ADR-0003;
- la persistencia relacional exigida por los impulsores;
- la condición de carrera identificada en RISK-001;
- la transición Validado a Bloqueado aprobada en ADR-0001;
- la simplicidad operativa del MVP.

El documento permanece como propuesta hasta completar su revisión final. La versión mayor exacta de PostgreSQL se fijará al iniciar la implementación conforme a la política de versiones aprobada y no constituye una condición para aceptar esta ADR.