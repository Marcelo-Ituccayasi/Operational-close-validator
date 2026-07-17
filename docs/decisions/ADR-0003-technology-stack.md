# ADR-0003: Stack tecnológico del MVP

**Estado:** Aceptada

**Estado documental:** Línea base aprobada

**Fecha:** 2026-07-16

**Producto:** Operational Close Validator

---

## 1. Contexto

ADR-0002 seleccionó un monolito modular por capacidades con puertos y adaptadores, organizado inicialmente en los módulos de negocio Gestión del Cierre Operativo e Identidad y Acceso.

La aplicación debe conservar una sola unidad desplegable, mantener el Dominio independiente de frameworks y persistencia, y permitir que los casos de uso definan límites de consistencia materializados mediante adaptadores de Infraestructura.

Esta ADR seleccionará las tecnologías necesarias para implementar esa arquitectura. La decisión debe favorecer:

- consistencia transaccional local para VR-008;
- persistencia relacional;
- separación verificable entre Dominio, Aplicación y adaptadores;
- pruebas automatizadas por nivel;
- productividad compatible con un MVP;
- construcción y despliegue reproducibles;
- una sola unidad desplegable de aplicación.

La selección del stack no sustituye las decisiones posteriores sobre persistencia, concurrencia, autenticación, almacenamiento de Evidencias de Soporte ni despliegue físico.

---

## 2. Problema de decisión

¿Qué stack tecnológico permite implementar el MVP de Operational Close Validator respetando ADR-0002, manteniendo consistencia transaccional, testabilidad, productividad y una sola unidad desplegable?

---

## 3. Alcance de la decisión

### Esta ADR decide

- lenguaje principal y runtime;
- framework web del backend;
- estrategia tecnológica de la interfaz web;
- mecanismo general de comunicación entre interfaz y backend;
- herramientas generales de construcción;
- herramientas generales de pruebas;
- composición tecnológica de la unidad desplegable;
- política general de versiones soportadas.

### Esta ADR no decide

- motor relacional concreto;
- ORM o mecanismo concreto de acceso a datos;
- esquema físico de persistencia;
- estrategia de migraciones;
- aislamiento transaccional;
- control de concurrencia;
- bloqueo optimista o pesimista;
- reintentos ante conflictos;
- implementación concreta de VR-008;
- mecanismo concreto de autenticación y sesión;
- almacenamiento físico de Evidencias de Soporte;
- proveedor final de despliegue;
- contratos detallados de API;
- estructura física definitiva del repositorio.

Estas decisiones deberán resolverse en ADR y documentos técnicos posteriores.

---

## 4. Restricciones y decisiones heredadas

### 4.1. Restricciones de los impulsores

La alternativa seleccionada debe cumplir CON-001 a CON-007 de Architecture Drivers v0.1:

- aplicación web con interfaz de usuario y backend;
- persistencia relacional;
- catálogo interno, fijo y versionado de reglas;
- usuario responsable preconfigurado con autenticación básica;
- envío interno a contabilidad;
- estado Enviado a contabilidad terminal dentro del MVP;
- configuración reproducible y despliegue público.

### 4.2. Decisiones heredadas de ADR-0002

La alternativa seleccionada debe permitir:

- una sola unidad desplegable de aplicación;
- módulos Gestión del Cierre Operativo e Identidad y Acceso;
- puertos de salida definidos por Aplicación;
- adaptadores implementados por Infraestructura;
- Dominio independiente de frameworks y persistencia;
- pruebas arquitectónicas sobre dependencias;
- VR-008 dentro de Gestión del Cierre Operativo;
- backend o API y recursos de interfaz incluidos en una misma versión desplegada.

Una alternativa que incumpla una restricción obligatoria queda descartada, independientemente de su puntuación.

---

## 5. Perfil del proyecto

### 5.1. Datos confirmados

| Dimensión | Información |
|---|---|
| Implementadores efectivos | 1 |
| Disponibilidad | 30–35 horas por semana |
| Sistema operativo principal | Windows con PowerShell |
| Tipo de producto | MVP web demostrable y producto de portafolio |
| Arquitectura aprobada | Monolito modular por capacidades con puertos y adaptadores |
| Persistencia requerida | Relacional |
| Topología requerida | Una unidad desplegable de aplicación |
| Curva de aprendizaje aceptable | Moderada |
| Experiencia con pruebas automatizadas | 3 — Proyecto propio funcional |
| Experiencia desplegando aplicaciones web | 3 — Proyecto propio funcional |
| Presupuesto mensual máximo | Aproximadamente USD 500 |
| Objetivo profesional prioritario | Construir proyectos de portafolio técnicamente sólidos, desplegables y demostrables, que evidencien arquitectura, pruebas, persistencia y prácticas profesionales |

### 5.2. Experiencia por tecnología

Escala utilizada:

- 0 — Ninguna.
- 1 — Conceptos básicos o cursos.
- 2 — Proyectos guiados.
- 3 — Proyecto propio funcional.
- 4 — Experiencia laboral o proyectos complejos.
- 5 — Dominio autónomo.

| Tecnología | Nivel | Evidencia |
|---|---:|---|
| Python | 3 | Proyecto propio funcional |
| Django | 2 | Proyectos guiados |
| TypeScript | 3 | Proyecto propio funcional |
| Node.js | 3 | Proyecto propio funcional |
| React | 2 | Proyectos guiados |
| Java | 2 | Proyectos guiados |
| Spring Boot | 3 | Proyecto propio funcional |

### 5.3. Interpretación del perfil

- La disponibilidad semanal permite asumir una curva de aprendizaje moderada, pero no justifica introducir complejidad sin un impulsor aprobado.
- Ninguna alternativa dispone de una ventaja decisiva por experiencia: las tres se encuentran entre nivel guiado y proyecto propio.
- Spring Boot cuenta con experiencia práctica directa de nivel 3, aunque Java permanece en nivel 2.
- TypeScript y Node.js cuentan con nivel 3, pero React permanece en nivel 2 y añade una segunda cadena de construcción.
- Python cuenta con nivel 3 y Django con nivel 2.
- El presupuesto no discrimina entre las alternativas mientras se conserve una sola unidad desplegable y una base relacional administrada de costo moderado.
- El objetivo profesional favorece una solución que haga visibles arquitectura, transacciones, pruebas y límites de dependencias, sin sacrificar la viabilidad del MVP.

---

## 6. Criterios de evaluación

| ID | Criterio | Descripción | Prioridad | Peso |
|---|---|---|---|---:|
| TS-001 | Consistencia transaccional y concurrencia relacional | Capacidad del ecosistema para implementar posteriormente VR-008 bajo un límite local consistente | Crítica | 5 |
| TS-002 | Persistencia relacional | Disponibilidad de adaptadores maduros para relaciones, restricciones, transacciones y migraciones | Crítica | 5 |
| TS-003 | Modularidad y dirección de dependencias | Capacidad para materializar módulos por capacidad y proteger el Dominio | Alta | 3 |
| TS-004 | Testabilidad | Facilidad para probar Dominio, Aplicación y adaptadores por separado | Alta | 3 |
| TS-005 | Una sola unidad desplegable | Facilidad para construir backend e interfaz como una misma versión desplegada | Alta | 3 |
| TS-006 | Productividad | Viabilidad de implementación dentro del calendario y capacidad reales | Alta | 3 |
| TS-007 | Ecosistema web y seguridad | Herramientas disponibles para API, interfaz, validación, autenticación y protección de operaciones | Alta | 3 |
| TS-008 | Despliegue reproducible | Facilidad para generar un paquete o imagen reproducible | Alta | 3 |
| TS-009 | Herramientas de desarrollo | Calidad de build, depuración, tipado, pruebas y automatización | Alta | 3 |
| TS-010 | Experiencia actual del implementador | Adecuación a conocimientos verificables del implementador efectivo | Alta | 3 |
| TS-011 | Valor profesional y de aprendizaje | Transferibilidad y valor para el portafolio, sin desplazar criterios críticos | Media | 1 |
| TS-012 | Costo operativo | Capacidad para operar el MVP dentro del presupuesto confirmado | Media | 1 |

### 6.1. Escala

- 1 — Muy deficiente.
- 2 — Deficiente.
- 3 — Aceptable.
- 4 — Buena.
- 5 — Muy buena.

### 6.2. Regla de selección

La alternativa seleccionada será la de mayor puntuación ponderada entre las que cumplan todas las restricciones obligatorias.

Una excepción solo será válida cuando:

1. el criterio de exclusión haya sido definido antes de puntuar;
2. el criterio esté trazado a una restricción o impulsor aprobado;
3. la excepción quede registrada explícitamente;
4. no se utilice TS-011 para compensar una deficiencia crítica.

No se ajustarán puntuaciones para hacer coincidir la matriz con una preferencia previa.

---

## 7. Alternativas consideradas

### Alternativa A — Python + Django + Django Templates

#### Composición

```text
Lenguaje: Python
Runtime: intérprete de Python soportado
Backend: Django
Interfaz: Django Templates + HTML + CSS + JavaScript
Comunicación: formularios HTTP y respuestas HTML;
              endpoints JSON únicamente cuando sean necesarios
Pruebas: pytest o unittest
Build: gestión de dependencias y recursos estáticos del ecosistema Python/Django
Unidad desplegable: proceso Django que sirve backend e interfaz
```

La tecnología concreta de persistencia queda fuera de esta ADR. La alternativa debe permitir un adaptador relacional seleccionado posteriormente.

#### Ajuste a ADR-0002

Django puede organizarse por capacidades mediante paquetes o aplicaciones internas. Para conservar ADR-0002:

- el Dominio debe utilizar objetos Python independientes del ORM;
- los casos de uso deben residir fuera de vistas y modelos persistentes;
- las vistas actúan como adaptadores de entrada;
- la persistencia se implementa mediante adaptadores;
- los límites no quedan garantizados por el framework y deben verificarse mediante estructura y pruebas.

#### Escenarios relevantes

- **VR-008:** un caso de uso de Aplicación define el límite de consistencia; la implementación concreta se selecciona en ADR-0004.
- **Pruebas:** el Dominio puede probarse con Python sin levantar Django cuando permanece libre del ORM.
- **Unidad desplegable:** backend y plantillas forman parte del mismo proceso y versión.
- **Interfaz:** reduce la cantidad de herramientas y pasos de construcción respecto a una SPA independiente.

#### Ventajas

- Una sola cadena principal de construcción.
- Interfaz y backend integrados.
- Herramientas maduras para aplicaciones web.
- Pocos procesos durante desarrollo y despliegue.
- Adecuado para flujos operativos basados principalmente en formularios y estados.

#### Costos y riesgos

- El ORM y los modelos del framework pueden filtrarse al Dominio.
- La separación entre casos de uso y vistas requiere disciplina explícita.
- La interfaz puede requerir JavaScript adicional para interacciones avanzadas.
- La estructura del framework no impone por sí misma puertos y adaptadores.

---

### Alternativa B — TypeScript + Node.js LTS + NestJS + React + Vite

#### Composición

```text
Lenguaje: TypeScript
Runtime: versión soportada de Node.js LTS
Backend: NestJS
Interfaz: React + TypeScript
Build de interfaz: Vite
Comunicación: API HTTP/JSON
Pruebas backend: Jest + Supertest
Pruebas frontend: Vitest
Unidad desplegable: proceso NestJS que sirve la API y los recursos
                    estáticos producidos por Vite
```

La tecnología concreta de persistencia queda fuera de esta ADR. La alternativa debe permitir un adaptador relacional seleccionado posteriormente.

#### Ajuste a ADR-0002

NestJS proporciona módulos, encapsulación de providers e inyección de dependencias que pueden utilizarse para materializar los límites aprobados.

El framework no impone puertos y adaptadores. Para conservar ADR-0002:

- el Dominio debe ser TypeScript sin dependencias de NestJS;
- los controladores actúan como adaptadores de entrada;
- los casos de uso coordinan las operaciones;
- Aplicación define puertos mediante contratos TypeScript;
- los adaptadores concretos se registran en el punto de ensamblaje;
- pruebas arquitectónicas deben detectar dependencias prohibidas.

#### Escenarios relevantes

- **VR-008:** el controlador delega en un caso de uso; Aplicación coordina la regla, transición y registro mediante un puerto transaccional.
- **Pruebas:** Dominio y Aplicación pueden probarse sin iniciar NestJS cuando los contratos permanecen independientes.
- **Unidad desplegable:** Vite produce recursos estáticos y NestJS los sirve junto con la API.
- **Interfaz:** React permite una interfaz cliente con estado e interacciones más ricas, a cambio de una segunda cadena de construcción.

#### Política de versiones

Se utilizará una versión soportada de Node.js en estado Active LTS o Maintenance LTS.

La versión mayor exacta se fijará al iniciar la implementación y quedará registrada en:

- archivos de configuración reproducible;
- documentación de desarrollo;
- automatización de integración continua;
- imagen o paquete de despliegue.

#### Ventajas

- TypeScript compartido entre backend e interfaz.
- Contratos tipados en ambos lados de la aplicación.
- Módulos y providers útiles para componer adaptadores.
- Herramientas separadas para pruebas de backend y frontend.
- Interfaz adecuada para interacciones de cliente más complejas.
- Construcción compatible con una sola versión desplegada.

#### Costos y riesgos

- Dos cadenas de construcción: backend y frontend.
- Mayor cantidad de dependencias y configuración inicial.
- La SPA requiere gestionar navegación, estado, errores HTTP y contratos.
- Los decoradores y módulos de NestJS pueden filtrarse hacia Aplicación o Dominio si no se controlan.
- La selección de React debe justificarse por necesidades reales de interacción, no únicamente por valor de aprendizaje.

---

### Alternativa C — Java + Spring Boot + Thymeleaf

#### Composición

```text
Lenguaje: Java
Runtime: versión LTS de la JVM compatible con el framework
Backend: Spring Boot
Interfaz: Thymeleaf + HTML + CSS + JavaScript
Comunicación: formularios HTTP y respuestas HTML;
              endpoints JSON únicamente cuando sean necesarios
Pruebas: JUnit + Mockito + herramientas de integración de Spring
Build: Maven o Gradle
Unidad desplegable: aplicación Spring Boot que sirve backend e interfaz
```

La tecnología concreta de persistencia queda fuera de esta ADR. La alternativa debe permitir un adaptador relacional seleccionado posteriormente.

#### Ajuste a ADR-0002

Spring proporciona inyección de dependencias y composición de componentes. Para conservar ADR-0002:

- el Dominio debe mantenerse independiente de anotaciones de persistencia;
- los controladores actúan como adaptadores de entrada;
- los servicios de Aplicación coordinan casos de uso;
- Aplicación define contratos;
- Infraestructura implementa adaptadores;
- los paquetes y pruebas arquitectónicas verifican los límites.

#### Escenarios relevantes

- **VR-008:** un servicio de Aplicación define el límite de consistencia; el mecanismo concreto se selecciona posteriormente.
- **Pruebas:** el Dominio puede probarse con JUnit sin levantar Spring.
- **Unidad desplegable:** Spring Boot y Thymeleaf forman una sola aplicación.
- **Interfaz:** evita una SPA independiente y reduce los pasos de construcción del cliente.

#### Ventajas

- Tipado estático y herramientas maduras.
- Una sola cadena principal de construcción.
- Interfaz y backend integrados en la misma aplicación.
- Buen soporte para pruebas unitarias e integración.
- Composición explícita mediante inyección de dependencias.

#### Costos y riesgos

- Mayor cantidad de código estructural.
- Curva de aprendizaje del ecosistema Spring.
- Riesgo de filtrar anotaciones de framework o persistencia al Dominio.
- La productividad real depende significativamente de la experiencia previa del implementador.

---

## 8. Evaluación comparativa

### 8.1. Matriz de puntuación

| Criterio | Prioridad | Peso | A. Django | B. NestJS + React | C. Spring Boot |
|---|---:|---:|---:|---:|---:|
| TS-001 — Consistencia y concurrencia relacional | Crítica | 5 | 4 | 4 | 4 |
| TS-002 — Persistencia relacional | Crítica | 5 | 5 | 4 | 5 |
| TS-003 — Modularidad y dependencias | Alta | 3 | 3 | 4 | 4 |
| TS-004 — Testabilidad | Alta | 3 | 4 | 4 | 5 |
| TS-005 — Una unidad desplegable | Alta | 3 | 5 | 4 | 5 |
| TS-006 — Productividad | Alta | 3 | 4 | 4 | 4 |
| TS-007 — Ecosistema web y seguridad | Alta | 3 | 5 | 5 | 5 |
| TS-008 — Despliegue reproducible | Alta | 3 | 4 | 4 | 4 |
| TS-009 — Herramientas de desarrollo | Alta | 3 | 4 | 5 | 5 |
| TS-010 — Experiencia actual | Alta | 3 | 3 | 3 | 3 |
| TS-011 — Valor profesional y aprendizaje | Media | 1 | 4 | 5 | 5 |
| TS-012 — Costo operativo | Media | 1 | 5 | 5 | 5 |

### 8.2. Puntuación ponderada

| Criterio | Peso | A. Django | B. NestJS + React | C. Spring Boot |
|---|---:|---:|---:|---:|
| TS-001 | 5 | 20 | 20 | 20 |
| TS-002 | 5 | 25 | 20 | 25 |
| TS-003 | 3 | 9 | 12 | 12 |
| TS-004 | 3 | 12 | 12 | 15 |
| TS-005 | 3 | 15 | 12 | 15 |
| TS-006 | 3 | 12 | 12 | 12 |
| TS-007 | 3 | 15 | 15 | 15 |
| TS-008 | 3 | 12 | 12 | 12 |
| TS-009 | 3 | 12 | 15 | 15 |
| TS-010 | 3 | 9 | 9 | 9 |
| TS-011 | 1 | 4 | 5 | 5 |
| TS-012 | 1 | 5 | 5 | 5 |
| **Total** |  | **150** | **149** | **160** |

### 8.3. Justificación de puntuaciones

#### TS-001 — Consistencia y concurrencia relacional

Las tres alternativas permiten definir un límite local de consistencia dentro de una sola aplicación.

Ninguna recibe puntuación 5 porque el mecanismo concreto depende de ADR-0004, que deberá seleccionar persistencia, bloqueo, aislamiento y reintentos.

#### TS-002 — Persistencia relacional

Django y Spring Boot disponen de una integración relacional central y ampliamente utilizada dentro de sus ecosistemas.

NestJS no define una estrategia relacional única; requiere seleccionar y componer un adaptador adicional. Esto no impide cumplir el criterio, pero incrementa las decisiones y pruebas necesarias.

#### TS-003 — Modularidad y dirección de dependencias

Django permite organizar paquetes por capacidad, pero su estructura habitual no protege automáticamente la separación entre Dominio, Aplicación y modelos persistentes.

NestJS y Spring Boot proporcionan mecanismos de composición e inyección de dependencias que facilitan materializar adaptadores y contratos. En ambos casos, los límites deben protegerse mediante estructura y pruebas arquitectónicas.

#### TS-004 — Testabilidad

Los tres lenguajes permiten probar el Dominio sin framework.

Spring Boot obtiene una puntuación mayor porque el ecosistema permite combinar pruebas unitarias, pruebas por segmento, integración y pruebas contra infraestructura real dentro de una estrategia consistente.

#### TS-005 — Una unidad desplegable

Django y Spring Boot integran backend e interfaz renderizada en servidor mediante una sola cadena principal de construcción.

NestJS y React también pueden formar una sola versión desplegada, pero requieren construir el cliente por separado e incorporar los recursos resultantes en el paquete del backend.

#### TS-006 — Productividad

Las tres alternativas reciben puntuación 4:

- existe disponibilidad de 30–35 horas por semana;
- el implementador tiene experiencia entre nivel 2 y 3 en cada ecosistema;
- ninguna alternativa exige comenzar desde cero;
- todas requieren disciplina adicional para implementar ADR-0002 correctamente.

No se asigna puntuación 5 porque ninguna tecnología se encuentra en nivel 4 o 5 de experiencia.

#### TS-007 — Ecosistema web y seguridad

Los tres ecosistemas ofrecen herramientas suficientes para interfaz web, validación, seguridad y pruebas.

La estrategia concreta de autenticación permanece fuera de esta ADR.

#### TS-008 — Despliegue reproducible

Las tres alternativas permiten fijar versiones y producir un paquete o imagen reproducible.

La puntuación no presupone un proveedor de despliegue concreto.

#### TS-009 — Herramientas de desarrollo

TypeScript y Java proporcionan comprobaciones estáticas integradas y ecosistemas de build y pruebas consolidados.

Python dispone de herramientas equivalentes, pero parte del tipado y análisis debe configurarse de forma adicional.

#### TS-010 — Experiencia actual

Las tres alternativas reciben puntuación 3:

- Django combina Python nivel 3 con Django nivel 2;
- NestJS + React combina TypeScript y Node.js nivel 3 con React nivel 2;
- Spring Boot combina Spring Boot nivel 3 con Java nivel 2.

No existe una diferencia suficiente para alterar la decisión por experiencia.

#### TS-011 — Valor profesional y aprendizaje

NestJS + React y Spring Boot permiten evidenciar con claridad arquitectura modular, contratos, pruebas y separación de responsabilidades.

Django también aporta valor profesional, pero requiere más esfuerzo documental para evitar que el diseño parezca una aplicación centrada en el framework.

#### TS-012 — Costo operativo

El presupuesto aproximado de USD 500 mensuales es suficiente para las tres alternativas bajo la topología aprobada.

El proveedor y costo final se decidirán posteriormente.

---

## 9. Decisión

Se adopta:

**Alternativa C — Java + Spring Boot + Thymeleaf.**

### 9.1. Stack seleccionado

| Componente | Decisión |
|---|---|
| Lenguaje | Java |
| Runtime | Versión LTS de Java soportada por la línea de Spring Boot seleccionada |
| Backend | Spring Boot con Spring MVC |
| Interfaz | Thymeleaf + HTML + CSS + JavaScript |
| Comunicación principal | Formularios HTTP y respuestas HTML |
| Comunicación adicional | Endpoints HTTP/JSON únicamente cuando un caso de uso lo requiera |
| Construcción | Maven |
| Pruebas unitarias | JUnit Jupiter + Mockito |
| Pruebas de integración | Herramientas de prueba de Spring Boot |
| Unidad desplegable | Aplicación Spring Boot que contiene backend, plantillas y recursos web |

### 9.2. Decisiones deliberadamente pendientes

Esta ADR no selecciona:

- motor relacional;
- ORM o biblioteca de acceso a datos;
- herramienta de migraciones;
- nivel de aislamiento;
- mecanismo de bloqueo;
- estrategia concreta de reintentos;
- mecanismo de autenticación y sesión;
- proveedor de despliegue.

Estas decisiones corresponden a ADR-0004, ADR-0005 y documentos técnicos posteriores.

### 9.3. Política de versiones

Al iniciar la implementación se fijarán:

- una versión LTS de Java;
- una línea estable y soportada de Spring Boot compatible con esa versión;
- versiones gestionadas mediante Maven;
- versiones exactas registradas en configuración reproducible y automatización.

No se seleccionará automáticamente la versión más reciente si no existe compatibilidad verificada con las dependencias requeridas.

---

## 10. Fundamento

La alternativa C obtiene la mayor puntuación ponderada:

```text
A. Python + Django + Django Templates:              150
B. TypeScript + NestJS + React + Vite:              149
C. Java + Spring Boot + Thymeleaf:                   160
```

La diferencia se concentra en:

- testabilidad;
- integración relacional;
- herramientas de desarrollo;
- construcción natural de una sola unidad desplegable;
- ajuste del ecosistema a límites transaccionales explícitos.

La experiencia del implementador no fuerza la decisión, pero la hace viable: existe un proyecto propio funcional con Spring Boot y disponibilidad de 30–35 horas semanales.

Thymeleaf se selecciona sobre una SPA porque el MVP aprobado está compuesto principalmente por:

- formularios;
- consultas de estado;
- registro y corrección de eventos;
- gestión de alertas;
- consolidación;
- envío interno.

No existe un requisito aprobado que necesite estado complejo en el cliente, navegación autónoma o sincronización intensiva de una SPA.

La decisión no se toma por prestigio tecnológico. Se toma porque la alternativa C obtiene la mayor puntuación bajo la regla definida antes de evaluar.

---

## 11. Aplicación de ADR-0002 al stack seleccionado

### 11.1. Separación de responsabilidades

```text
Adaptadores de entrada
Spring MVC Controllers
        ↓
Aplicación
Casos de uso y puertos
        ↓
Dominio
Entidades, reglas e invariantes

Adaptadores de salida
Persistencia, reloj, evidencia y autenticación
        ↓ implementan
Puertos definidos por Aplicación
```

### 11.2. Restricciones de dependencia

- El Dominio utiliza Java sin dependencias de Spring.
- El Dominio no contiene anotaciones de Spring, persistencia ni serialización.
- Los controladores Spring MVC actúan como adaptadores de entrada.
- Los casos de uso residen en Aplicación.
- Aplicación define contratos mediante interfaces Java cuando existe una necesidad real.
- Infraestructura implementa los contratos y participa en el ensamblaje de Spring.
- Thymeleaf pertenece a Presentación.
- Las transacciones se aplican al límite del caso de uso y no a los controladores.
- VR-008 se coordina desde Aplicación.
- Las pruebas arquitectónicas detectan dependencias prohibidas.

### 11.3. Unidad desplegable

El proceso de construcción debe producir:

1. código compilado de la aplicación;
2. plantillas Thymeleaf;
3. recursos estáticos;
4. configuración externa;
5. un único paquete o imagen de despliegue.

La base de datos y el almacenamiento físico de Evidencias de Soporte permanecen como dependencias externas de Infraestructura.

---

## 12. Estrategia general de pruebas

| Nivel | Herramienta o enfoque |
|---|---|
| Dominio | JUnit Jupiter sin contexto de Spring |
| Aplicación | JUnit Jupiter + Mockito o dobles manuales |
| Adaptadores web | Pruebas de Spring MVC |
| Adaptadores de persistencia | Pruebas de integración contra el motor real seleccionado |
| Integración | Contexto controlado de Spring Boot |
| Extremo a extremo | Flujos críticos sobre la aplicación desplegada |
| Arquitectura | Reglas automatizadas de dependencias y ciclos |

Las pruebas del Dominio no deben cargar el contexto de Spring.

Las pruebas de persistencia no sustituyen las pruebas del Dominio.

---

## 13. Consecuencias positivas

- Una sola cadena principal de construcción.
- Backend e interfaz integrados en la misma aplicación.
- Ecosistema adecuado para límites transaccionales explícitos.
- Buen soporte para pruebas unitarias, integración y arquitectura.
- Tipado estático en el backend.
- Menor complejidad de interfaz que una SPA.
- Coherencia directa con la unidad desplegable aprobada.
- Viabilidad respaldada por experiencia previa con Spring Boot.
- Capacidad de producir un proyecto de portafolio con arquitectura y pruebas visibles.

---

## 14. Consecuencias y costos

- Java permanece en nivel 2 de experiencia y exige consolidación práctica.
- Spring Boot puede introducir acoplamiento si sus anotaciones se filtran al Dominio.
- Thymeleaf limita la autonomía del cliente frente a una SPA.
- Interacciones avanzadas pueden requerir JavaScript específico.
- La aplicación debe evitar clases de servicio genéricas que mezclen Aplicación, Dominio e Infraestructura.
- La selección no resuelve persistencia, concurrencia ni autenticación.
- Es necesario mantener pruebas arquitectónicas.
- La JVM puede requerir mayor memoria que otras alternativas; el dimensionamiento se resolverá en la estrategia de despliegue.
- El uso de Spring no autoriza introducir microservicios ni componentes distribuidos.

---

## 15. Alternativas descartadas

### 15.1. Python + Django + Django Templates

Se descarta porque obtiene una puntuación inferior en:

- modularidad y dirección de dependencias;
- testabilidad por niveles;
- herramientas de análisis estático;
- protección explícita frente al acoplamiento entre Dominio y ORM.

La alternativa sigue siendo viable y productiva, pero requiere mayor disciplina para materializar ADR-0002 sin adaptar el Dominio a los modelos del framework.

### 15.2. TypeScript + NestJS + React + Vite

Se descarta porque:

- obtiene una puntuación total ligeramente inferior;
- requiere dos cadenas de construcción;
- React se encuentra en nivel 2 de experiencia;
- no existe un requisito aprobado que justifique una SPA;
- la persistencia relacional exige una selección adicional no integrada por el framework.

La alternativa sigue siendo técnicamente válida. Se reconsideraría si apareciera un impulsor aprobado que requiera una interfaz cliente rica o contratos compartidos en TypeScript.

---

## 16. Criterios de cumplimiento

La decisión se considera correctamente aplicada cuando:

1. el código del Dominio es Java sin dependencias de Spring;
2. los casos de uso residen en Aplicación;
3. los controladores Spring MVC delegan en casos de uso;
4. Thymeleaf se limita a Presentación;
5. Aplicación define los puertos requeridos;
6. Infraestructura implementa los adaptadores;
7. las transacciones no se coordinan desde controladores;
8. VR-008 se ejecuta desde un caso de uso;
9. las pruebas del Dominio no cargan Spring;
10. backend, plantillas y recursos forman una sola versión desplegada;
11. las versiones exactas están fijadas en Maven y configuración reproducible;
12. se utiliza una versión soportada de Java y Spring Boot;
13. no se selecciona persistencia fuera de ADR-0004;
14. no se selecciona autenticación fuera de ADR-0005;
15. las dependencias prohibidas se verifican automáticamente;
16. el stack no amplía el alcance funcional del MVP.

---

## 17. Documentos relacionados

- ADR-0002 — Estilo arquitectónico de la aplicación.
- ADR-0004 — Estrategia de persistencia y control de concurrencia.
- Architecture Drivers v0.1 — Impulsores de Arquitectura.
- Validation Rules v0.2 — Reglas de Validación.
- Domain Model v0.3 — Modelo de Dominio.
- State Machine v0.3 — Máquina de Estados.
- Use Cases v0.2 — Casos de Uso.
- MVP Scope v0.3 — Alcance del MVP.
- ADR-0001 — La validación final puede devolver un cierre validado a bloqueado.

---

## 18. Control de cambios

Modificar esta decisión requiere:

1. identificar el impulsor o dato de perfil que cambió;
2. reevaluar los criterios afectados;
3. recalcular la matriz;
4. registrar una nueva ADR que sustituya o modifique esta decisión;
5. actualizar ADR-0004 y los diseños técnicos afectados;
6. incorporar el cambio mediante revisión.

---

## 19. Conclusión

El stack tecnológico seleccionado para Operational Close Validator es:

**Java + Spring Boot + Spring MVC + Thymeleaf + Maven.**

La decisión respeta el monolito modular por capacidades aprobado en ADR-0002, mantiene una sola unidad desplegable y proporciona una base adecuada para pruebas, transacciones y persistencia relacional.

La selección de motor relacional, adaptador de persistencia, migraciones y mecanismo de concurrencia permanece en ADR-0004.

El documento continúa como propuesta hasta completar la revisión de coherencia y registrar formalmente su aceptación.