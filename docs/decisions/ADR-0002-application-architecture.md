# ADR-0002: Estilo arquitectónico de la aplicación

**Estado:** Aceptada

**Estado documental:** Línea base aprobada

**Fecha:** 2026-07-14

**Producto:** Operational Close Validator

---

## 1. Contexto

Operational Close Validator debe ejecutar reglas de validación sobre Eventos Operativos, gestionar Alertas, mantener trazabilidad y garantizar que un Cierre Operativo solo sea enviado a contabilidad cuando todas las condiciones aprobadas se cumplan simultáneamente.

El comportamiento central que condiciona esta decisión es VR-008: el control final debe ejecutarse inmediatamente antes del envío, y su resultado, la transición del cierre y el registro del envío deben ser consistentes.

Una modificación concurrente durante esta operación no debe permitir un envío basado en Eventos Operativos, Resultados de Validación, Alertas o consolidaciones diferentes de los evaluados por VR-008.

Además:

- las reglas de validación deben poder probarse sin depender de la interfaz de usuario ni de una tecnología concreta de persistencia;
- las invariantes del dominio deben permanecer explícitas y protegidas;
- las operaciones críticas deben contar con límites de consistencia verificables;
- el MVP debe poder desplegarse de forma reproducible y demostrable;
- la solución no debe introducir complejidad operativa desproporcionada respecto al alcance aprobado.

Los principales impulsores relacionados son:

- FR-ARCH-002 — Ejecución de reglas de validación;
- FR-ARCH-005 — Invalidación y revalidación;
- FR-ARCH-006 — Consolidación del Cierre Operativo;
- FR-ARCH-007 — Control final y envío;
- QA-001 — Consistencia;
- QA-002 — Trazabilidad;
- QA-004 — Mantenibilidad;
- QA-005 — Testabilidad;
- RISK-001 — Condición de carrera durante el envío;
- RISK-003 — Acoplamiento de reglas con interfaz o persistencia;
- RISK-005 — Complejidad excesiva para el MVP.

---

## 2. Problema de decisión

¿Qué estilo arquitectónico permite implementar el MVP de Operational Close Validator manteniendo consistencia transaccional, claridad del dominio, testabilidad y simplicidad operativa, sin introducir complejidad innecesaria?

La decisión debe resolver dos dimensiones relacionadas:

- **Topología de despliegue:** una unidad desplegable o múltiples servicios.
- **Organización interna:** cómo se separan las capacidades de negocio, los casos de uso, el dominio y los adaptadores técnicos, y en qué dirección pueden depender entre sí.

---

## 3. Alcance de la decisión

### Esta ADR decide

- la topología general de la aplicación;
- la cantidad de unidades desplegables de aplicación;
- la organización por capacidades de negocio;
- la organización interna de responsabilidades;
- la dirección permitida de las dependencias;
- la ubicación conceptual de reglas, casos de uso y adaptadores;
- el límite general de consistencia de las operaciones críticas;
- la relación general entre el núcleo operativo y la capacidad de identidad y acceso.

### Esta ADR no decide

- lenguaje de programación;
- framework;
- motor de base de datos;
- ORM;
- proveedor de despliegue;
- protocolo concreto de autenticación;
- mecanismo concreto de sesión;
- estrategia concreta de control de concurrencia;
- contratos detallados de API;
- esquema físico de persistencia;
- almacenamiento físico de Evidencias de Soporte.

---

## 4. Restricciones obligatorias

Toda alternativa debe cumplir estas restricciones:

| ID | Restricción | Origen |
|---|---|---|
| CON-001 | Aplicación web con interfaz de usuario y backend | MVP Scope v0.3 |
| CON-002 | Persistencia relacional | MVP Scope v0.3 |
| CON-003 | Catálogo interno, fijo y versionado de reglas, no configurable por el usuario | Validation Rules v0.2, MVP Scope v0.3 |
| CON-004 | Usuario único preconfigurado con autenticación básica, sin prescribir HTTP Basic Authentication | MVP Scope v0.3 |
| CON-005 | Sin integración contable externa; envío interno | UC-009, MVP Scope v0.3 |
| CON-006 | Estado Enviado a contabilidad terminal y no modificable dentro del MVP | State Machine v0.3, ADR-0001 |
| CON-007 | Configuración reproducible y despliegue público | MVP Scope v0.3 |

Una alternativa que incumpla alguna restricción obligatoria queda descartada independientemente de su puntuación en los demás criterios.

---

## 5. Criterios de evaluación

| ID | Criterio | Descripción | Prioridad | Peso |
|---|---|---|---|---:|
| DEC-001 | Consistencia transaccional | Capacidad de mantener coherentes VR-008, envío y estado del cierre | Crítica | 5 |
| DEC-002 | Claridad del dominio | Capacidad de representar entidades, reglas e invariantes sin acoplamiento innecesario | Alta | 3 |
| DEC-003 | Testabilidad | Facilidad para probar reglas y transiciones de forma automatizada | Alta | 3 |
| DEC-004 | Simplicidad operativa | Esfuerzo requerido para ejecutar, desplegar y mantener el MVP | Alta | 3 |
| DEC-005 | Persistencia relacional | Adecuación para relaciones, restricciones y trazabilidad | Crítica | 5 |
| DEC-006 | Seguridad suficiente | Soporte para autenticación y protección de operaciones | Alta | 3 |
| DEC-007 | Despliegue público | Facilidad para publicar una versión demostrable | Alta | 3 |
| DEC-008 | Tiempo de implementación | Viabilidad dentro de la capacidad y calendario del proyecto | Alta | 3 |
| DEC-009 | Evolución | Capacidad de ampliar roles, reglas e integraciones después del MVP | Media | 1 |
| DEC-010 | Adecuación al aprendizaje | Valor técnico y profesional de la alternativa para el proyecto | Media | 1 |

Escala aplicada:

- 1 — Muy deficiente.
- 2 — Deficiente.
- 3 — Aceptable.
- 4 — Buena.
- 5 — Muy buena.

La puntuación ponderada se calcula multiplicando la puntuación de la alternativa por el peso del criterio.

DEC-010 no puede compensar una deficiencia en un criterio crítico ni el incumplimiento de una restricción obligatoria.

---

## 6. Alternativas consideradas

### Alternativa A — Monolito en capas con dependencias descendentes

#### Descripción

Una sola aplicación y una sola unidad desplegable, organizada en capas técnicas descendentes:

```text
Presentación
UI / API / Controladores
        ↓
Lógica de negocio
Reglas y coordinación de operaciones
        ↓
Acceso a datos
Consultas y mapeo
        ↓
Persistencia relacional
```

Las capas superiores dependen directamente de las inferiores.

La capa de lógica de negocio combina reglas del dominio y coordinación de casos de uso. La capa de acceso a datos contiene consultas, mapeo y comunicación con la persistencia.

#### Cómo resolvería los escenarios críticos

**VR-008 y envío**

La operación puede ejecutarse dentro de una transacción relacional que abarque:

- ejecución de VR-008;
- actualización del estado del Cierre Operativo;
- registro del intento o resultado del envío.

El límite transaccional está contenido dentro de una sola aplicación.

**Cambio de una regla**

Modificar una regla implica cambiar la capa de lógica de negocio.

Cuando la regla requiere nuevos datos, también pueden cambiar la capa de acceso a datos, el modelo persistente y las consultas relacionadas. La interfaz de usuario solo debe cambiar cuando también cambia la información que debe capturar o presentar.

**Invalidación de resultados**

La lógica de negocio identifica el cambio relevante y coordina:

- invalidación de Resultados de Validación;
- cambio de estado del Evento Operativo;
- bloqueo del Cierre Operativo cuando corresponda;
- invalidación de la consolidación afectada.

La persistencia se ejecuta mediante la capa de acceso a datos.

**Despliegue público**

Se despliega una sola unidad de aplicación.

La configuración y el diagnóstico operativo son simples. Las pruebas de la lógica de negocio requieren sustituir o simular sus dependencias hacia las capas inferiores.

#### Ventajas

- Implementación inicial directa.
- Transacciones relacionales fáciles de delimitar.
- Una sola unidad desplegable.
- Baja complejidad operativa.
- Estructura ampliamente conocida.
- Menor costo inicial de organización.

#### Desventajas

- Combina reglas de dominio y coordinación de casos de uso en una misma capa.
- Mantiene dependencias descendentes hacia el acceso a datos.
- Las reglas pueden quedar condicionadas por contratos concretos de persistencia.
- Los límites entre lógica de dominio y lógica de aplicación son menos explícitos.
- La sustitución de infraestructura requiere dobles o abstracciones incorporadas de forma disciplinada.

#### Riesgos

- Crecimiento progresivo de una capa de lógica de negocio con responsabilidades heterogéneas.
- Acoplamiento entre reglas, coordinación y acceso a datos.
- Mayor dificultad para verificar automáticamente los límites internos.
- Menor protección explícita frente a RISK-003.

---

### Alternativa B — Monolito modular por capacidades con puertos y adaptadores

#### Descripción

Una sola unidad desplegable de aplicación, organizada en módulos por capacidad de negocio.

Los módulos iniciales son:

- **Gestión del Cierre Operativo:** módulo núcleo que contiene Eventos Operativos, Evidencias de Soporte, Autorizaciones, Reglas de Validación, Resultados de Validación, Alertas, consolidación y envío interno.
- **Identidad y Acceso:** módulo de soporte responsable del usuario preconfigurado, autenticación y sesión. Su alcance es deliberadamente reducido y no se exige que contenga un modelo de dominio complejo.

El comportamiento relacionado con VR-008 permanece dentro de Gestión del Cierre Operativo para conservar sus invariantes y su límite local de consistencia.

Los módulos no constituyen unidades desplegables independientes.

Cada módulo organiza internamente sus responsabilidades mediante aplicación, dominio y adaptadores:

```text
Adaptadores de entrada
UI / API / Controladores
        ↓
Aplicación
Casos de uso y puertos de salida
        ↓
Dominio
Entidades, reglas e invariantes

Adaptadores de salida
Persistencia, almacenamiento, reloj y sesión
        ↓ implementan
Puertos definidos por Aplicación
```

Las dependencias internas de cada módulo siguen esta dirección:

```text
Adaptadores de entrada → Aplicación → Dominio
Adaptadores de salida → Puertos de Aplicación
```

La composición de puertos y adaptadores se realiza en un punto de ensamblaje externo al Dominio.

#### Cómo resolvería los escenarios críticos

**VR-008 y envío**

El caso de uso de envío, ubicado en la parte de Aplicación de Gestión del Cierre Operativo, coordina:

- lectura del estado requerido;
- ejecución de VR-008 en el Dominio;
- transición del Cierre Operativo;
- registro del intento o resultado del envío.

Aplicación define el límite de consistencia del caso de uso. Un adaptador de Infraestructura materializa ese límite mediante el mecanismo transaccional y de concurrencia que se seleccione posteriormente.

La alternativa permite implementar la consistencia requerida dentro de una sola unidad de aplicación y sin coordinación distribuida.

**Cambio de una regla**

Modificar una regla afecta principalmente al Dominio de Gestión del Cierre Operativo.

Cuando cambian los datos requeridos por la regla, también pueden verse afectados:

- contratos o puertos de Aplicación;
- adaptadores de persistencia;
- modelo de datos;
- interfaz de usuario, cuando deba capturar o presentar información nueva.

Los cambios quedan localizados en las responsabilidades relacionadas y no obligan a modificar componentes independientes.

**Invalidación de resultados**

El Dominio expresa:

- qué cambios invalidan Resultados de Validación;
- qué transición corresponde al Evento Operativo;
- cuándo el Cierre Operativo debe quedar Bloqueado;
- cuándo una consolidación deja de estar vigente.

El caso de uso de Aplicación coordina la operación completa y persiste sus efectos mediante los puertos correspondientes.

**Despliegue público**

Se despliega una sola unidad de aplicación que contiene:

- backend o API;
- recursos de la interfaz web;
- módulos Gestión del Cierre Operativo e Identidad y Acceso;
- adaptadores requeridos por la aplicación.

La base de datos relacional y el almacenamiento físico de Evidencias de Soporte son dependencias externas de Infraestructura, no servicios de aplicación independientes.

Las pruebas del Dominio pueden ejecutarse sin levantar la interfaz, la aplicación completa ni una persistencia real.

#### Ventajas

- Mantiene el Dominio independiente de detalles de Infraestructura.
- Permite probar reglas e invariantes de forma aislada.
- Define límites explícitos por capacidad de negocio.
- Mantiene VR-008 dentro de un único módulo de negocio.
- Protege la lógica central mediante una dirección explícita de dependencias.
- Mantiene una sola unidad desplegable.
- Permite sustituir adaptadores sin modificar las reglas del Dominio.
- Facilita verificar automáticamente dependencias prohibidas y ciclos.

#### Desventajas

- Mayor complejidad inicial que un monolito en capas.
- Requiere disciplina para mantener módulos y dependencias.
- Exige definir puertos únicamente cuando existe una necesidad real.
- Introduce trabajo inicial de ensamblaje y organización.
- Puede convertirse en sobrediseño si se crean abstracciones sin un impulsor concreto.

#### Riesgos

- Pérdida progresiva de límites si no se revisan las dependencias.
- Creación de puertos o adaptadores innecesarios.
- Curva de aprendizaje mayor que una estructura tradicional.
- Uso incorrecto del término modular sin límites verificables entre capacidades.
- Fragmentación artificial del núcleo operativo, lo que podría debilitar las invariantes de VR-008.

---

### Alternativa C — Arquitectura distribuida o microservicios

#### Descripción

Múltiples unidades desplegables que separan responsabilidades en servicios independientes.

Una posible división sería:

- servicio de Eventos Operativos y validaciones;
- servicio de Alertas;
- servicio de Cierres, consolidación y envío;
- servicio de Identidad y Acceso.

Cada servicio sería propietario de sus datos y los expondría mediante contratos explícitos. La implementación física de esa propiedad no se determina en esta alternativa.

#### Cómo resolvería los escenarios críticos

**VR-008 y envío**

VR-008 tendría que consultar y coordinar datos cuya propiedad estaría distribuida entre varios servicios.

Para conservar consistencia fuerte sería necesario:

- centralizar la operación en un único propietario transaccional; o
- introducir coordinación distribuida.

Centralizar la operación debilitaría la separación propuesta. Introducir coordinación distribuida aumentaría significativamente la complejidad y el riesgo operativo.

Una solución basada únicamente en consistencia eventual y compensaciones no proporciona por sí sola la garantía aprobada para el envío.

**Cambio de una regla**

Una regla podría quedar localizada en el servicio responsable de validaciones. Cuando cambien sus datos o contratos, será necesario coordinar versiones y compatibilidad entre servicios consumidores.

**Invalidación de resultados**

Un cambio en Eventos Operativos debería comunicar la invalidación a los servicios propietarios de Resultados de Validación, Alertas o Cierres.

La coordinación podría realizarse mediante eventos o llamadas síncronas, pero introduciría:

- latencia;
- estados intermedios;
- reintentos;
- idempotencia;
- nuevos puntos de falla.

**Despliegue público**

Se desplegarían múltiples unidades.

La solución podría requerir infraestructura operativa adicional para:

- configuración;
- comunicación entre servicios;
- observabilidad;
- gestión de fallas;
- trazabilidad distribuida;
- ejecución de pruebas integradas.

#### Ventajas

- Propiedad explícita de capacidades y datos.
- Independencia de despliegue.
- Escalabilidad independiente.
- Aislamiento de fallas cuando está correctamente implementado.
- Posibilidad de asignar equipos independientes por servicio.

#### Desventajas

- Coordinación transaccional compleja.
- Mayor dificultad para implementar VR-008 con consistencia fuerte.
- Mayor costo operativo.
- Mayor esfuerzo de diagnóstico.
- Pruebas de integración y contratos más complejas.
- Necesidad de observabilidad distribuida.
- Sobrediseño respecto al alcance del MVP.

#### Riesgos

- Inconsistencia temporal o permanente entre servicios.
- Condiciones de carrera durante VR-008.
- Retraso significativo del MVP.
- Dificultad para reconstruir trazabilidad completa.
- Complejidad desproporcionada respecto al tamaño del equipo y del producto.

---

## 7. Evaluación comparativa

### Matriz de puntuación

| Criterio | Prioridad | Peso | A. Monolito en capas | B. Monolito modular | C. Microservicios |
|---|---:|---:|---:|---:|---:|
| DEC-001 — Consistencia transaccional | Crítica | 5 | 5 | 5 | 1 |
| DEC-002 — Claridad del dominio | Alta | 3 | 3 | 5 | 3 |
| DEC-003 — Testabilidad | Alta | 3 | 3 | 5 | 2 |
| DEC-004 — Simplicidad operativa | Alta | 3 | 5 | 4 | 1 |
| DEC-005 — Persistencia relacional | Crítica | 5 | 5 | 5 | 3 |
| DEC-006 — Seguridad suficiente | Alta | 3 | 4 | 4 | 3 |
| DEC-007 — Despliegue público | Alta | 3 | 5 | 5 | 2 |
| DEC-008 — Tiempo de implementación | Alta | 3 | 5 | 4 | 1 |
| DEC-009 — Evolución | Media | 1 | 2 | 5 | 4 |
| DEC-010 — Adecuación al aprendizaje | Media | 1 | 3 | 5 | 3 |

### Puntuación ponderada

| Criterio | Peso | A. Monolito en capas | B. Monolito modular | C. Microservicios |
|---|---:|---:|---:|---:|
| DEC-001 | 5 | 25 | 25 | 5 |
| DEC-002 | 3 | 9 | 15 | 9 |
| DEC-003 | 3 | 9 | 15 | 6 |
| DEC-004 | 3 | 15 | 12 | 3 |
| DEC-005 | 5 | 25 | 25 | 15 |
| DEC-006 | 3 | 12 | 12 | 9 |
| DEC-007 | 3 | 15 | 15 | 6 |
| DEC-008 | 3 | 15 | 12 | 3 |
| DEC-009 | 1 | 2 | 5 | 4 |
| DEC-010 | 1 | 3 | 5 | 3 |
| **Total** |  | **130** | **141** | **63** |

### Justificación de puntuaciones

#### DEC-001 — Consistencia transaccional

- **Alternativa A (5):** permite ejecutar VR-008, la transición y el registro del envío dentro de una sola transacción relacional y una sola aplicación.
- **Alternativa B (5):** permite implementar el mismo límite local de consistencia, pero separa explícitamente coordinación, reglas y mecanismo transaccional.
- **Alternativa C (1):** distribuir la propiedad de los datos evaluados por VR-008 introduce coordinación distribuida y dificulta mantener la consistencia fuerte requerida.

#### DEC-002 — Claridad del dominio

- **Alternativa A (3):** la lógica de negocio está separada del acceso a datos, pero combina reglas de dominio y coordinación de casos de uso en una misma capa y mantiene dependencias descendentes. La separación es aceptable, aunque menos explícita que en la alternativa B.
- **Alternativa B (5):** el Dominio expresa entidades, reglas e invariantes sin depender de adaptadores técnicos. Los límites por capacidad y la dirección de dependencias son explícitos y verificables.
- **Alternativa C (3):** cada servicio puede definir claramente su dominio local, pero las invariantes que atraviesan servicios son más difíciles de representar y verificar globalmente.

#### DEC-003 — Testabilidad

- **Alternativa A (3):** la lógica de negocio puede probarse utilizando dobles para sus dependencias inferiores. Sin embargo, la dirección descendente y la mezcla de reglas con coordinación de aplicación introducen más fricción que los puertos explícitos de la alternativa B.
- **Alternativa B (5):** el Dominio puede probarse directamente y los casos de uso pueden probarse mediante puertos sustituidos por dobles controlados.
- **Alternativa C (2):** aunque cada servicio puede probarse de forma aislada, los escenarios críticos requieren pruebas de integración, contratos y coordinación entre múltiples procesos.

#### DEC-004 — Simplicidad operativa

- **Alternativa A (5):** una sola unidad, configuración simple y despliegue directo.
- **Alternativa B (4):** mantiene una sola unidad desplegable, aunque exige disciplina y organización interna adicional.
- **Alternativa C (1):** múltiples unidades y posibles necesidades adicionales de configuración, comunicación, observabilidad y diagnóstico.

#### DEC-005 — Persistencia relacional

- **Alternativa A (5):** utiliza directamente relaciones, restricciones y transacciones dentro de una sola persistencia.
- **Alternativa B (5):** aprovecha las mismas capacidades relacionales mediante adaptadores, sin acoplar las reglas del Dominio al mecanismo persistente.
- **Alternativa C (3):** puede utilizar persistencia relacional, pero la propiedad distribuida dificulta las relaciones y transacciones que atraviesan servicios.

#### DEC-006 — Seguridad suficiente

- **Alternativa A (4):** una sola aplicación permite implementar autenticación y protección de operaciones con baja complejidad.
- **Alternativa B (4):** mantiene la misma simplicidad operativa y separa explícitamente Identidad y Acceso del núcleo operativo.
- **Alternativa C (3):** proteger múltiples servicios y propagar identidad introduce coordinación adicional.

#### DEC-007 — Despliegue público

- **Alternativa A (5):** un solo artefacto de aplicación y un proceso de despliegue directo.
- **Alternativa B (5):** un solo artefacto de aplicación con módulos internos que no alteran la topología operativa.
- **Alternativa C (2):** múltiples artefactos y mayor configuración para una demostración pública reproducible.

#### DEC-008 — Tiempo de implementación

- **Alternativa A (5):** menor costo inicial y estructura directa.
- **Alternativa B (4):** requiere esfuerzo inicial moderado para módulos, puertos, adaptadores y pruebas de límites.
- **Alternativa C (1):** requiere coordinación, contratos, observabilidad y pruebas distribuidas que no se justifican para el MVP.

#### DEC-009 — Evolución

- **Alternativa A (2):** el acoplamiento descendente puede aumentar el impacto de los cambios y dificultar la evolución localizada.
- **Alternativa B (5):** los límites permiten localizar los cambios. Una nueva regla afecta principalmente al Dominio de Gestión del Cierre Operativo; un nuevo rol puede afectar Identidad y Acceso, autorización de casos de uso y Presentación sin obligar a modificar componentes no relacionados.
- **Alternativa C (4):** los servicios pueden evolucionar con independencia, pero requieren coordinación de contratos y versiones.

#### DEC-010 — Adecuación al aprendizaje

- **Alternativa A (3):** permite aplicar principios fundamentales de diseño en capas y transacciones.
- **Alternativa B (5):** permite practicar modularidad, inversión de dependencias, puertos, adaptadores y pruebas arquitectónicas sin complejidad distribuida.
- **Alternativa C (3):** ofrece aprendizaje sobre sistemas distribuidos, pero su costo desvía el proyecto de los objetivos prioritarios del MVP.

---

## 8. Decisión

Se adopta:

**Alternativa B — Monolito modular por capacidades con puertos y adaptadores.**

La aplicación tendrá las siguientes características:

- una sola unidad desplegable de aplicación;
- backend o API y recursos de interfaz web incluidos en una misma versión desplegada;
- módulos de negocio Gestión del Cierre Operativo e Identidad y Acceso;
- Gestión del Cierre Operativo como módulo núcleo;
- Identidad y Acceso como módulo de soporte con alcance reducido;
- organización interna mediante Aplicación, Dominio y adaptadores;
- puertos de salida definidos por Aplicación;
- adaptadores de salida implementados por Infraestructura;
- composición de dependencias en un punto de ensamblaje externo al Dominio;
- reglas de validación ubicadas en el Dominio de Gestión del Cierre Operativo;
- casos de uso ubicados en la parte de Aplicación del módulo correspondiente;
- límite de consistencia definido por el caso de uso y materializado por el mecanismo transaccional de Infraestructura;
- persistencia relacional y almacenamiento de Evidencias de Soporte como dependencias externas de Infraestructura, no como servicios de aplicación independientes.

VR-008, las entidades evaluadas, la transición del Cierre Operativo y el registro del envío permanecerán dentro de Gestión del Cierre Operativo.

La decisión no autoriza dividir Validaciones, Alertas, consolidación o envío en servicios o módulos transaccionales independientes dentro del MVP.

---

## 9. Fundamento

La decisión prioriza los siguientes criterios:

1. **DEC-001 — Consistencia transaccional.**
2. **DEC-005 — Persistencia relacional.**
3. **DEC-003 — Testabilidad.**
4. **DEC-002 — Claridad del dominio.**
5. **DEC-004 — Simplicidad operativa.**

La alternativa B permite implementar la consistencia requerida por VR-008 dentro de una sola unidad y un límite local verificable.

También permite aprovechar relaciones, restricciones y transacciones relacionales sin introducir dependencias desde el Dominio hacia la tecnología de persistencia.

La separación entre Aplicación, Dominio y adaptadores permite probar:

- reglas e invariantes sin Infraestructura;
- casos de uso mediante puertos sustituidos;
- adaptadores mediante pruebas de integración;
- límites arquitectónicos mediante verificaciones automatizadas.

La puntuación de la alternativa B supera a la alternativa A principalmente por claridad del dominio, testabilidad y evolución controlada, no porque la alternativa A sea incapaz de satisfacer la consistencia o persistencia requeridas.

La decisión no se toma únicamente por preferencia personal o valor de aprendizaje.

---

## 10. Reglas arquitectónicas resultantes

1. **Las reglas de validación residen en el Dominio de Gestión del Cierre Operativo** y no dependen de interfaz, persistencia, mecanismos de sesión ni controladores.

2. **Los casos de uso residen en la parte de Aplicación del módulo correspondiente** y coordinan entidades, reglas, puertos y efectos externos.

3. **Aplicación define los puertos de salida** necesarios para ejecutar sus casos de uso, como repositorios, unidad de trabajo, almacenamiento de evidencia, reloj y contexto del principal autenticado.

4. **Los adaptadores de salida implementan los puertos definidos por Aplicación.**

5. **Aplicación define el límite de consistencia de cada caso de uso**, mientras Infraestructura proporciona el mecanismo transaccional y de concurrencia concreto.

6. **VR-008, la transición del Cierre Operativo y el registro del envío comparten un mismo límite de consistencia.**

7. **Las dependencias internas de cada módulo siguen una dirección explícita:**

   ```text
   Adaptadores de entrada → Aplicación → Dominio
   Adaptadores de salida → Puertos de Aplicación
   ```

8. **El Dominio no depende de frameworks web, ORM, controladores, drivers de base de datos, mecanismos de sesión, sistemas de archivos ni protocolos de autenticación.**

9. **Las pruebas del Dominio se ejecutan sin levantar la aplicación completa, sin persistencia real y sin interfaz de usuario.**

10. **Cambiar una tecnología de Infraestructura no debe exigir modificar las reglas del Dominio.** El cambio puede requerir nuevos adaptadores, configuración, migraciones y pruebas de integración.

11. **El adaptador de entrada recibe la credencial o sesión y delega su verificación a Identidad y Acceso.** El caso de uso recibe un principal autenticado y verifica que la operación esté autorizada. El Dominio de Gestión del Cierre Operativo no conoce el protocolo de autenticación ni el mecanismo de sesión.

12. **La Evidencia de Soporte se representa en el núcleo mediante información de negocio y una referencia abstracta.** Su almacenamiento físico se implementa mediante un adaptador y no forma parte del Dominio.

13. **La composición de puertos y adaptadores ocurre en un punto de ensamblaje externo al Dominio.**

14. **Gestión del Cierre Operativo no depende de la implementación interna de Identidad y Acceso.** Recibe únicamente el principal autenticado requerido por sus casos de uso. Identidad y Acceso no depende de Gestión del Cierre Operativo.

15. **VR-008 permanece dentro de Gestión del Cierre Operativo** y no requiere coordinar transacciones entre módulos de negocio independientes.

16. **Los módulos no acceden directamente a las estructuras persistentes internas de otro módulo.** Toda dependencia entre módulos debe realizarse mediante contratos explícitos de Aplicación.

17. **No se crean puertos, adaptadores o módulos sin una necesidad derivada de un caso de uso, atributo de calidad o restricción aprobada.**

---

## 11. Consecuencias positivas

- **Consistencia transaccional local:** VR-008, transición y envío pueden implementarse dentro de una sola operación local, sin coordinación distribuida.
- **Testabilidad:** las reglas e invariantes pueden probarse sin Infraestructura.
- **Claridad del dominio:** las responsabilidades de negocio permanecen separadas de mecanismos técnicos.
- **Límites por capacidad:** Gestión del Cierre Operativo e Identidad y Acceso mantienen responsabilidades explícitas.
- **Simplicidad operativa:** existe una sola unidad desplegable de aplicación.
- **Evolución controlada:** las modificaciones pueden localizarse en las responsabilidades relacionadas.
- **Infraestructura sustituible:** cambiar una tecnología no obliga a modificar las reglas del Dominio.
- **Verificación arquitectónica:** las dependencias prohibidas y los ciclos pueden detectarse automáticamente.
- **Aprendizaje aplicable:** el proyecto permite practicar modularidad y puertos y adaptadores sin introducir sistemas distribuidos.

---

## 12. Consecuencias y costos

- **Complejidad inicial moderada:** es necesario definir módulos, responsabilidades, puertos, adaptadores y composición.
- **Disciplina requerida:** el equipo debe respetar la dirección de dependencias y los límites de los módulos.
- **Pruebas arquitectónicas:** será necesario incorporar verificaciones que detecten dependencias prohibidas y ciclos.
- **Riesgo de sobrediseño:** los puertos y adaptadores deben responder a necesidades reales, no a abstracciones hipotéticas.
- **Documentación adicional:** los módulos, contratos y límites deben mantenerse documentados.
- **No resuelve la concurrencia automáticamente:** una ADR posterior deberá seleccionar el mecanismo concreto para detectar o impedir modificaciones conflictivas durante VR-008.
- **No resuelve el almacenamiento físico de Evidencias de Soporte:** una decisión posterior deberá definir si se almacenan archivos, referencias o URLs y bajo qué restricciones.
- **No garantiza intercambiabilidad sin costo:** cambiar Infraestructura puede exigir adaptadores, migraciones, configuración y pruebas de integración.
- **Una sola unidad desplegable limita el escalamiento independiente:** esa capacidad no se considera necesaria para el MVP.
- **Identidad y Acceso debe mantenerse deliberadamente simple:** no debe evolucionar hacia una plataforma completa de gestión de identidades dentro del MVP.

---

## 13. Alternativas descartadas

### Alternativa A — Monolito en capas con dependencias descendentes

#### Motivo del descarte

La alternativa A satisface:

- las restricciones obligatorias;
- DEC-001 — Consistencia transaccional;
- DEC-005 — Persistencia relacional;
- DEC-004 — Simplicidad operativa;
- DEC-007 — Despliegue público;
- DEC-008 — Tiempo de implementación.

Se descarta porque obtiene un resultado inferior en:

- DEC-002 — Claridad del dominio;
- DEC-003 — Testabilidad;
- DEC-009 — Evolución.

También ofrece menos protección explícita frente a RISK-003, al combinar reglas y coordinación de casos de uso dentro de una misma capa con dependencias descendentes hacia el acceso a datos.

La alternativa B conserva una topología operativa igualmente simple, pero proporciona límites internos más explícitos y verificables.

### Alternativa C — Arquitectura distribuida o microservicios

#### Motivo del descarte

La alternativa C no satisface adecuadamente los impulsores prioritarios del MVP:

- **DEC-001 y RISK-001:** distribuir la propiedad de Eventos Operativos, Alertas, Resultados de Validación y Cierres dificulta ejecutar VR-008 bajo un único límite de consistencia.
- **DEC-004:** introduce complejidad operativa desproporcionada.
- **DEC-007:** dificulta un despliegue público simple y reproducible.
- **DEC-008:** incrementa considerablemente el tiempo de implementación.
- **RISK-005:** representa sobrediseño para un producto con una sola organización, un único usuario preconfigurado y un flujo operativo integrado.

La posibilidad de despliegue o escalamiento independiente no compensa el costo introducido en consistencia, trazabilidad, pruebas y operación.

---

## 14. Criterios de cumplimiento

La decisión se considera correctamente aplicada cuando:

1. **El Dominio no contiene dependencias técnicas prohibidas.** Las reglas, entidades e invariantes no referencian frameworks web, ORM, controladores, drivers de base de datos, mecanismos de sesión ni almacenamiento físico.

2. **Los casos de uso coordinan las operaciones.** Registrar eventos, validar, corregir, consolidar y enviar se implementan en Aplicación y no en Presentación ni en adaptadores de Persistencia.

3. **Las pruebas del Dominio se ejecutan sin Infraestructura.** Las pruebas de reglas y transiciones no requieren una base de datos, servidor web ni sistema de archivos.

4. **VR-008 comparte un límite de consistencia con la transición y el registro del envío.** Una modificación conflictiva no puede producir un envío exitoso basado en datos no vigentes.

5. **La aplicación se despliega como una sola unidad.** El artefacto o versión desplegada incluye backend o API y recursos de interfaz web.

6. **Las dependencias internas siguen la dirección aprobada.**

   ```text
   Adaptadores de entrada → Aplicación → Dominio
   Adaptadores de salida → Puertos de Aplicación
   ```

7. **No existen ciclos de dependencias entre módulos o responsabilidades internas.**

8. **VR-008 permanece dentro de Gestión del Cierre Operativo.** Su ejecución no requiere coordinar transacciones entre módulos de negocio independientes.

9. **Los límites se verifican automáticamente.** Pruebas arquitectónicas o reglas equivalentes detectan dependencias prohibidas y ciclos.

10. **Identidad permanece desacoplada del dominio operativo.** Gestión del Cierre Operativo recibe un principal autenticado, pero no depende del mecanismo de sesión ni de la implementación interna de Identidad y Acceso.

11. **Los puertos corresponden a necesidades reales de Aplicación.** No existen interfaces vacías o abstracciones creadas únicamente para anticipar escenarios futuros.

12. **Los adaptadores implementan los contratos definidos por Aplicación.** La persistencia, almacenamiento de Evidencias de Soporte y contexto de autenticación no definen reglas del Dominio.

13. **La base de datos y el almacenamiento de evidencia no se implementan como servicios de aplicación independientes dentro del MVP.**

---

## 15. Documentos relacionados

- Architecture Drivers v0.1 — Impulsores de Arquitectura.
- Validation Rules v0.2 — Reglas de Validación.
- Domain Model v0.3 — Modelo de Dominio.
- State Machine v0.3 — Máquina de Estados.
- Use Cases v0.2 — Casos de Uso.
- MVP Scope v0.3 — Alcance del MVP.
- ADR-0001 — La validación final puede devolver un cierre validado a bloqueado.

---

## 16. Control de cambios

Modificar esta decisión requiere:

1. identificar el impulsor que cambió;
2. evaluar el impacto sobre consistencia, claridad del dominio, testabilidad, despliegue y simplicidad operativa;
3. determinar si el cambio puede resolverse dentro de la arquitectura aprobada;
4. registrar una nueva ADR que modifique o sustituya expresamente esta decisión cuando corresponda;
5. actualizar los diseños técnicos afectados;
6. incorporar los cambios mediante revisión independiente.

---

## 17. Conclusión

La arquitectura de Operational Close Validator será un **monolito modular por capacidades con puertos y adaptadores**.

La aplicación se organizará inicialmente en:

- Gestión del Cierre Operativo, como módulo núcleo;
- Identidad y Acceso, como módulo de soporte.

Cada módulo organizará sus responsabilidades mediante Aplicación, Dominio y adaptadores, respetando una dirección explícita de dependencias.

VR-008 permanecerá dentro de Gestión del Cierre Operativo y su caso de uso definirá un límite local de consistencia que deberá ser materializado por la estrategia transaccional y de concurrencia seleccionada posteriormente.

Esta alternativa permite implementar la consistencia transaccional necesaria para VR-008 dentro de un límite local y verificable, protege las reglas del Dominio frente a detalles técnicos, permite pruebas aisladas y mantiene la simplicidad operativa de una sola unidad desplegable.

El costo adicional de organización y disciplina es aceptable frente a los beneficios obtenidos en testabilidad, mantenibilidad, trazabilidad y claridad del dominio.