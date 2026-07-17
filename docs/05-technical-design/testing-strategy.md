# Estrategia de Pruebas

**Versión:** v0.1

**Estado:** Línea base aprobada

**Fase:** 05 — Diseño técnico

**Producto:** Operational Close Validator

---

## 1. Propósito y alcance

Este documento define la estrategia de pruebas del MVP de Operational Close Validator.

Su objetivo es verificar de forma reproducible:

- reglas e invariantes del Dominio;
- transiciones de estado;
- coordinación de casos de uso;
- persistencia, transacciones y concurrencia;
- contrato HTTP;
- autenticación y seguridad;
- almacenamiento y entrega de Evidencias de Soporte;
- flujos completos del MVP.

La estrategia prioriza escenarios de riesgo y comportamiento observable. La cobertura de líneas es un indicador complementario, no un sustituto de los escenarios obligatorios.

Quedan fuera:

- pruebas de carga, estrés y capacidad;
- pruebas de penetración;
- evaluación formal de accesibilidad o usabilidad;
- integraciones externas no incluidas en el MVP;
- validación de certificados, proxy, DNS, copias de seguridad y restauración del entorno desplegado;
- compatibilidad exhaustiva entre navegadores;
- automatización mediante Selenium, WebDriver o una herramienta equivalente.

Las verificaciones propias del entorno desplegado se definirán en `deployment-strategy.md`.

---

## 2. Restricciones heredadas

| Fuente | Restricción de prueba |
|---|---|
| Validation Rules v0.2 | Solo VR-001, VR-002, VR-003, VR-006 y VR-008 pertenecen al MVP |
| State Machine v0.3 | Deben verificarse transiciones permitidas, prohibidas y terminales |
| Use Cases v0.2 | Deben cubrirse los flujos principales y alternativos aprobados |
| ADR-0002 | Dominio y Aplicación permanecen desacoplados de frameworks |
| ADR-0003 | La aplicación utiliza Java, Spring Boot, Spring MVC, Thymeleaf y Maven |
| ADR-0004 | PostgreSQL real, `READ_COMMITTED`, `TransactionTemplate`, bloqueo pesimista y ausencia de reintentos automáticos |
| ADR-0005 | Form login, sesión HTTP, CSRF, un usuario y una sesión activa |
| Architecture Overview v0.1 | Las pruebas respetan los límites del monolito modular y los puertos |
| Data Model v0.1 | Flyway, restricciones, revisiones, trazabilidad e inmutabilidad histórica |
| API Design v0.1 | HTML, `GET`/`POST`, PRG con `303` y códigos HTTP definidos |
| Security Design v0.1 | Controles de sesión, encabezados, login, proxy y Evidencias |
| MVP Scope v0.3 | No se prueban reglas o capacidades fuera del MVP |

No se utiliza H2, una base embebida ni un sustituto de PostgreSQL para pruebas de persistencia o transacciones.

---

## 3. Principios

1. **Comportamiento antes que implementación.** Las pruebas verifican resultados, invariantes y contratos; no acoplan el diseño a clases internas innecesarias.
2. **PostgreSQL real para integración.** Las consultas, restricciones, bloqueos y migraciones se ejecutan contra PostgreSQL mediante Testcontainers.
3. **Dominio sin infraestructura.** Las pruebas unitarias del Dominio no levantan Spring, JPA, archivos ni red.
4. **Transacciones reales para riesgos transaccionales.** VR-008, rollback y concurrencia no se validan con mocks.
5. **Seguridad habilitada.** Ninguna prueba de integración web deshabilita globalmente CSRF o los filtros de Spring Security.
6. **Determinismo.** Se inyectan reloj, generador de UUID y dependencias variables cuando el comportamiento dependa de ellos.
7. **Aislamiento.** Ninguna prueba depende del orden, de datos dejados por otra prueba o de recursos compartidos.
8. **Sin esperas arbitrarias.** Las pruebas concurrentes usan barreras, latches y timeouts acotados; no utilizan `Thread.sleep` para coordinar.
9. **Sin reintentos de CI.** Una prueba inestable debe corregirse, no ocultarse mediante reejecución automática.
10. **Trazabilidad.** Cada regla, transición y caso de uso crítico debe vincularse con al menos una prueba identificable.

---

## 4. Niveles de prueba

| Nivel | Propósito | Dependencias |
|---|---|---|
| Unitaria | Verificar lógica pura y decisiones aisladas | Sin Spring, PostgreSQL ni sistema de archivos |
| Integración de adaptador | Verificar persistencia, archivos, seguridad y HTTP | Dependencia real bajo control |
| Integración de Aplicación | Verificar casos de uso, transacciones e invalidaciones | Spring y adaptadores reales relevantes |
| Sistema HTTP | Verificar flujos completos desde la frontera HTTP | Contexto completo, PostgreSQL y almacenamiento temporal |
| Arquitectura | Verificar límites entre módulos y capas | Análisis de clases compiladas |

No se fijan porcentajes para una “pirámide” de pruebas. La cantidad por nivel se determina por costo, riesgo y capacidad de aislar el comportamiento.

---

## 5. Pruebas unitarias

### 5.1. Dominio

Las pruebas del Dominio verifican:

- creación y mutación válida de entidades;
- rechazo de invariantes inválidas;
- cálculo de `balanceEffect`;
- importe nominal positivo;
- comportamiento de Anulaciones;
- revisiones e invalidación lógica;
- resultados de validación;
- Alertas y sus transiciones;
- máquinas de estado;
- reglas del catálogo aprobado.

No se mockean entidades, objetos de valor ni reglas del Dominio.

### 5.2. Reglas de validación

Cada regla incluida debe cubrir, como mínimo:

| Regla | Escenarios obligatorios |
|---|---|
| VR-001 | condición satisfecha; autorización requerida ausente; inconsistencia de trazabilidad |
| VR-002 | monto coincidente; monto diferente; datos requeridos inválidos |
| VR-003 | evidencia válida y legible; evidencia ausente; ilegible; inactiva |
| VR-006 | autorización formal vigente; autorización ausente; inactiva; descuento y Anulación |
| VR-008 | éxito; evento no validado; Alerta bloqueante; resultado fallido o no vigente; consolidación ausente o no vigente |

También se verifica:

- aplicabilidad de cada regla;
- severidad y resultado;
- estado resultante;
- generación o mantenimiento de Alerta;
- imposibilidad de validar un Evento con una regla aplicable fallida;
- invalidación de resultados dependientes después de una corrección.

VR-008 puede tener lógica pura probada unitariamente, pero su comportamiento final se aprueba únicamente mediante pruebas transaccionales con persistencia real.

### 5.3. Máquinas de estado

Deben cubrirse todas las transiciones permitidas y prohibidas del MVP.

Estados del Evento:

```text
REGISTERED
PENDING_SUPPORT
PENDING_AUTHORIZATION
WITH_OBSERVATIONS
VALIDATED
```

Estados del Cierre:

```text
PREPARATION
BLOCKED
VALIDATED
SENT_TO_ACCOUNTING
```

Estados de la Alerta:

```text
ACTIVE
ACKNOWLEDGED
UNDER_REVIEW
RESOLVED
DISCARDED
```

Escenarios críticos:

- un cambio relevante lleva un Evento Validado a Registrado;
- una revalidación puede mantener un estado no validado;
- reconocer o revisar una Alerta no la resuelve;
- una Alerta solo queda Resuelta después de una revalidación exitosa;
- descartar exige justificación y no valida automáticamente el Evento;
- Enviado a contabilidad es terminal dentro del MVP.

### 5.4. Aplicación aislada

Los casos de uso se prueban con fakes o stubs de puertos cuando no se requiere comportamiento técnico real.

Se verifica:

- validación del `AuthenticatedPrincipal`;
- comandos y consultas;
- decisiones de flujo;
- errores de negocio;
- invocación de puertos requerida por el contrato;
- invalidación de resultados y consolidaciones;
- ausencia de mutación ante precondiciones fallidas.

Mockito se limita a puertos o colaboradores externos a la unidad. No se verifica orden incidental de llamadas, salvo cuando el orden sea una decisión aprobada.

---

## 6. Persistencia y Flyway

### 6.1. Entorno

Las pruebas usan:

- PostgreSQL mediante Testcontainers;
- una imagen con versión fijada, nunca `latest`;
- Flyway como único mecanismo de creación del esquema;
- la misma versión mayor de PostgreSQL seleccionada para despliegue cuando esa decisión quede cerrada.

El contenedor no se comparte entre ejecuciones de CI.

### 6.2. Migraciones

Debe verificarse:

- arranque desde una base vacía;
- aplicación ordenada de todas las migraciones;
- rechazo de una migración inválida;
- esquema resultante compatible con JPA;
- catálogos y restricciones esperados;
- ausencia de creación automática del esquema por Hibernate.

Cuando exista una versión desplegada anterior, se añade una prueba de actualización desde esa versión hasta la actual.

### 6.3. Mapeos y repositorios

Se prueban únicamente operaciones aprobadas, no un CRUD genérico.

Cobertura mínima:

- mapeo explícito Dominio ↔ JPA;
- UUID y tipos temporales;
- `numeric(19,4)`;
- relaciones y claves foráneas;
- consultas por cierre, estado, vigencia y revisión;
- bloqueo pesimista del Cierre;
- índices y restricciones únicas relevantes;
- ausencia de eliminación física de entidades operativas;
- registros históricos append-only;
- inmutabilidad del contenido de Resultados de Validación;
- persistencia de actor estable y username histórico.

Restricciones críticas:

- período único de Cierre;
- importes no negativos donde corresponda;
- Anulación del mismo Cierre, no autorreferente y no dirigida a otra Anulación;
- una sola Anulación por Evento referenciado;
- vigencia coherente de Evidencias y Autorizaciones;
- estados permitidos;
- Cierre Enviado sin mutaciones posteriores.

### 6.4. Aislamiento

Las pruebas de repositorio pueden utilizar transacción de prueba para limpieza, pero las pruebas de comportamiento transaccional de Aplicación no quedan envueltas en una transacción exterior que oculte commits o rollbacks.

La limpieza se realiza de forma explícita y reproducible.

---

## 7. Transacciones y concurrencia

### 7.1. VR-008

La prueba transaccional de rechazo debe comprobar en un solo commit:

- Resultado de VR-008 fallido y vigente;
- causas estructuradas;
- intento de envío rechazado;
- transición de Validado a Bloqueado;
- trazabilidad de actor y momento;
- ausencia de envío exitoso.

La prueba de éxito debe comprobar en un solo commit:

- Resultado de VR-008 satisfecho y vigente;
- consolidación vigente;
- intento de envío exitoso;
- transición a Enviado a contabilidad;
- fecha y actor;
- ausencia de estado parcial.

Una excepción técnica antes del commit debe revertir todos los cambios de esa transacción.

### 7.2. Bloqueo

Toda operación que pueda alterar el resultado de VR-008 debe probar que adquiere primero el bloqueo pesimista del Cierre.

Orden aprobado:

```text
Cierre → entidades dependientes → trazabilidad
```

Las pruebas concurrentes usan dos transacciones y conexiones reales.

Escenarios obligatorios:

1. **Dos envíos simultáneos.** Existe exactamente un envío exitoso; el otro comando se rechaza o entra en conflicto sin duplicar el envío.
2. **Edición antes del envío.** La edición invalida resultados y consolidación; el envío posterior falla VR-008 y deja el Cierre Bloqueado.
3. **Envío antes de edición.** El envío confirma el estado terminal; la edición posterior se rechaza sin mutación.
4. **Validación y edición.** El resultado vigente corresponde a la revisión final; no existen actualizaciones perdidas.
5. **Modificación del Evento revertido.** La Anulación dependiente se recalcula, incrementa revisión e invalida resultados y consolidación.
6. **Timeout o conflicto de bloqueo.** No existe reintento automático y el resultado técnico se traduce según el contrato.
7. **Rollback.** Una falla intermedia no deja estados, Alertas, resultados o intentos parcialmente persistidos.

Las pruebas controlan el orden mediante `CountDownLatch`, `CyclicBarrier` o un mecanismo equivalente con timeout.

---

## 8. Contrato HTTP y Presentación

### 8.1. Pruebas de controlador

Las pruebas de slice verifican:

- binding de parámetros;
- validación sintáctica;
- adaptación del principal;
- construcción del comando;
- selección de vista;
- mensajes flash;
- traducción del resultado de Aplicación.

Los controladores se prueban sin acceder directamente a repositorios JPA.

### 8.2. Respuestas

| Caso | Resultado esperado |
|---|---|
| `GET` válido | `200 OK` |
| `POST` exitoso | `303 See Other` |
| rechazo de negocio persistido | `303 See Other` con mensaje flash de error |
| formulario inválido antes del caso de uso | `400 Bad Request` y mismo formulario |
| UUID inválido | `400 Bad Request` |
| entidad inexistente | `404 Not Found` |
| estado incompatible sin mutación | `409 Conflict` |
| conflicto técnico de concurrencia | `409 Conflict` |
| CSRF ausente o inválido | `403 Forbidden` |
| error técnico inesperado | `500 Internal Server Error` sanitizado |

Debe verificarse que:

- ningún `GET` modifica estado;
- PRG utiliza `303`, no una redirección genérica;
- un rechazo de VR-008 persiste su trazabilidad antes de redirigir;
- actor, fechas, estados, revisiones y valores derivados no se aceptan desde el formulario;
- no existen endpoints `PUT`, `PATCH` o `DELETE` del MVP;
- no existe resolución manual de Alertas;
- desactivar Evidencia o Autorización recibe únicamente los campos aprobados.

---

## 9. Autenticación y seguridad

Las pruebas de seguridad usan Spring Security real y el usuario preconfigurado de prueba.

Deben cubrir:

### 9.1. Identidad

- aprovisionamiento en base vacía;
- rechazo de registros incompatibles;
- formato `{bcrypt}`;
- rotación de credencial;
- ausencia de credenciales y hashes completos en logs.

### 9.2. Login y sesión

- login válido e inválido con mensaje uniforme;
- HTTP Basic deshabilitado;
- cambio del identificador de sesión;
- una sola sesión activa;
- reemplazo de la sesión anterior;
- timeout externalizable;
- logout únicamente por `POST`;
- remember-me deshabilitado;
- `SessionRegistry` y `HttpSessionEventPublisher`.

Los tests de timeout usan un valor reducido en perfil de prueba; no esperan treinta minutos reales.

### 9.3. Cookie y encabezados

En perfil público se verifica:

- `JSESSIONID`;
- `HttpOnly`;
- `Secure`;
- `SameSite=Lax`;
- `Domain` no configurado;
- CSP aprobada;
- HSTS solo sobre HTTPS;
- `nosniff`;
- `DENY`;
- `no-referrer`;
- `Permissions-Policy`;
- `Cache-Control: no-store`;
- CORS no habilitado.

### 9.4. CSRF

Todo `POST` debe probarse con y sin token, incluidos:

- login;
- logout;
- formularios de negocio;
- multipart de Evidencias.

No se deshabilita CSRF para facilitar pruebas.

### 9.5. Limitación del login y proxy

Se verifica:

- 10 fallos en 5 minutos;
- bloqueo de 5 minutos;
- clave por IP confiable y username normalizado;
- limpieza del contador después de un login exitoso;
- intentos bloqueados sin extensión indefinida;
- almacenamiento en memoria limitado;
- `Forwarded` y `X-Forwarded-*` ignorados sin proxy confiable;
- CIDR inválido como fallo de arranque.

La ventana temporal se prueba mediante reloj controlado, no mediante espera real.

### 9.6. Fail-fast

El contexto no inicia ante:

- username o hash ausente;
- hash BCrypt inválido;
- identidad incompatible;
- timeout o limitador inválido;
- proxy inválido;
- cookie insegura en perfil público;
- almacenamiento requerido inseguro o no utilizable.

---

## 10. Evidencias de Soporte

### 10.1. Registro

Debe verificarse la exclusión mutua:

```text
file XOR contentReference
```

Escenarios:

- archivo válido;
- referencia textual válida;
- ambos informados;
- ninguno informado;
- archivo vacío;
- archivo mayor de 10 MiB;
- solicitud multipart mayor de 12 MiB;
- extensión no permitida;
- MIME declarado falso;
- firma no permitida;
- PNG o JPEG no decodificable;
- referencia mayor que el límite persistido.

### 10.2. Almacenamiento

Se prueba el adaptador real sobre un directorio temporal:

- namespace `stored:`;
- namespace `reference:`;
- generación de extensión desde MIME detectado;
- cálculo SHA-256;
- clave opaca;
- path traversal;
- symlink no autorizado;
- salida del directorio base;
- permisos insuficientes;
- ausencia de solicitudes salientes para `reference:`.

### 10.3. Transacción y compensación

Debe verificarse:

- transferencia física antes de la transacción del Cierre;
- vinculación después de adquirir el bloqueo;
- eliminación compensatoria después de rollback;
- registro sanitizado cuando la compensación falla;
- Evidencia no activa después de una transacción fallida;
- conservación histórica al sustituir o desactivar.

### 10.4. Entrega

| Escenario | Resultado |
|---|---|
| imagen almacenada válida | autenticación, `inline`, MIME seguro, `nosniff`, `no-store` |
| PDF almacenado válido | autenticación y `attachment` |
| referencia `reference:` | `404 Not Found` en `/content` |
| archivo físico ausente | `404 Not Found` |
| SHA-256 diferente | `500 Internal Server Error` sanitizado |
| extensión o metadatos incoherentes | `500 Internal Server Error` |
| usuario no autenticado | redirección a login |
| Evidencia desactivada | contenido disponible para trazabilidad |

---

## 11. Pruebas de sistema HTTP

Las pruebas de sistema levantan:

- contexto completo de Spring Boot;
- filtros de seguridad;
- PostgreSQL Testcontainers;
- Flyway;
- almacenamiento temporal real;
- reloj y generadores controlables.

La frontera de prueba es HTTP mediante MockMvc. Esto verifica el sistema desplegable sin incorporar automatización de navegador al MVP.

Flujos obligatorios:

1. **Flujo conforme.** Crear Cierre, registrar Evento conforme, validar, consolidar, ejecutar VR-008 y enviar.
2. **VR-002.** Registrar ingreso con diferencia de monto, generar Alerta, corregir, revalidar, reconsolidar y enviar.
3. **VR-003.** Registrar egreso que requiere soporte, fallar por ausencia o ilegibilidad, incorporar Evidencia, revalidar y continuar.
4. **VR-006.** Registrar Descuento o Anulación sin autorización, bloquear, registrar Autorización, revalidar y continuar.
5. **Anulación.** Registrar Evento original y Anulación; modificar el original y comprobar recálculo e invalidaciones dependientes.
6. **Rechazo final.** Partir de Validado, introducir una condición no vigente, ejecutar VR-008, persistir rechazo y dejar el Cierre Bloqueado.
7. **Recuperación.** Corregir la causa del rechazo, revalidar, reconsolidar y enviar.
8. **Estado terminal.** Rechazar toda mutación relevante después de Enviado a contabilidad.
9. **Alerta.** Reconocer y poner en revisión sin resolver; resolver solo mediante revalidación; descartar con justificación.
10. **Seguridad.** Acceso anónimo, login, CSRF, sesión y logout dentro de un flujo protegido.

---

## 12. Pruebas de arquitectura

Se utiliza ArchUnit o una verificación equivalente para garantizar:

- Dominio no depende de Spring, JPA, Servlet ni adaptadores;
- Aplicación no depende de `HttpSession`, `Authentication` o entidades JPA;
- Presentación depende de Aplicación y no de repositorios JPA;
- entidades JPA permanecen en Infraestructura;
- `UserDetails` y tipos de Spring Security permanecen en Identidad y Acceso;
- los módulos no violan la dirección de dependencias aprobada;
- no aparecen roles técnicos en reglas del Dominio;
- no se introduce una dependencia circular entre módulos.

Estas pruebas forman parte del build obligatorio.

---

## 13. Datos y determinismo

Las pruebas utilizan:

- test data builders o fixtures explícitas;
- UUID conocidos cuando el identificador forme parte de la aserción;
- `Clock` fijo o controlable;
- zona horaria UTC para instantes técnicos;
- directorios temporales por prueba;
- nombres y datos que expresen el escenario;
- limpieza explícita de base y archivos.

No utilizan:

- fecha u hora del sistema sin control;
- orden de ejecución;
- datos compartidos mutables;
- puertos fijos;
- credenciales reales;
- secretos del entorno del desarrollador;
- llamadas de red externas.

Los métodos de prueba y símbolos de código se nombran en inglés. Las descripciones y documentación permanecen en español.

---

## 14. Herramientas y convención de ejecución

| Herramienta | Uso |
|---|---|
| JUnit 5 | Framework de pruebas |
| AssertJ | Aserciones |
| Mockito | Dobles de puertos en pruebas aisladas |
| Spring Boot Test | Integración de Aplicación y sistema |
| MockMvc | Contrato HTTP sin navegador |
| Spring Security Test | Login, sesión y CSRF |
| Testcontainers PostgreSQL | Persistencia, transacciones y concurrencia |
| Flyway | Migraciones del esquema |
| Apache Tika | Detección real de contenido en pruebas de Evidencias |
| ArchUnit | Límites de arquitectura |
| JaCoCo | Métricas de cobertura |

Convención Maven:

| Patrón | Ejecución |
|---|---|
| `*Test` | Surefire: unitarias, slices y arquitectura rápida |
| `*IT` | Failsafe: PostgreSQL, sistema, seguridad completa y concurrencia |

Comando local completo:

```powershell
./mvnw verify  # Ejecuta compilación, pruebas, integración y controles de cobertura.
```

No se acepta una configuración que ejecute únicamente pruebas unitarias antes de integrar cambios.

---

## 15. Cobertura y trazabilidad

### 15.1. Cobertura obligatoria por comportamiento

Debe existir cobertura completa de:

- reglas VR-001, VR-002, VR-003, VR-006 y VR-008;
- transiciones permitidas y prohibidas del MVP;
- casos de uso principales y alternativos;
- éxito y rechazo transaccional de VR-008;
- escenarios concurrentes de la sección 7;
- respuestas HTTP de la sección 8;
- controles de seguridad de la sección 9;
- escenarios de Evidencias de la sección 10;
- flujos de sistema de la sección 11.

### 15.2. Cobertura de código

JaCoCo actúa como guardrail:

```text
Dominio + Aplicación:
- cobertura de instrucciones >= 80 %
- cobertura de ramas >= 70 %
```

No se define un porcentaje individual para Presentación o Infraestructura porque los adaptadores se aprueban mediante escenarios de integración.

Una línea cubierta sin una aserción relevante no satisface un criterio de prueba.

### 15.3. Matriz de trazabilidad

El repositorio debe conservar una matriz ligera que relacione:

```text
regla / caso de uso / transición / riesgo → clase o conjunto de pruebas
```

La matriz puede mantenerse dentro de este documento, en nombres de pruebas o en un archivo dedicado cuando crezca. No requiere una herramienta de gestión externa.

---

## 16. Integración continua

La CI ejecuta, como mínimo:

1. compilación;
2. pruebas unitarias;
3. pruebas de arquitectura;
4. PostgreSQL Testcontainers y Flyway;
5. pruebas transaccionales y de concurrencia;
6. pruebas HTTP y de seguridad;
7. pruebas de Evidencias;
8. pruebas de sistema;
9. verificación JaCoCo;
10. empaquetado reproducible.

La ejecución falla cuando:

- falla una prueba;
- una prueba obligatoria está deshabilitada;
- una migración falla;
- no inicia PostgreSQL de prueba;
- se incumple la cobertura mínima;
- se detecta una violación arquitectónica;
- queda un proceso o recurso sin cerrar;
- una prueba excede su timeout;
- se requiere un reintento para obtener éxito.

La CI publica:

- reportes Surefire y Failsafe;
- reporte JaCoCo;
- logs sanitizados necesarios para diagnosticar fallos.

No publica credenciales, hashes, cookies, tokens CSRF ni contenido de Evidencias.

---

## 17. Exclusiones y riesgos aceptados

| Exclusión | Tratamiento |
|---|---|
| Rendimiento y carga | Se evalúan después del MVP si aparecen objetivos cuantitativos |
| Penetración | Se conserva el diseño preventivo y se difiere una evaluación especializada |
| Navegador real | MockMvc cubre el contrato; automatización visual queda fuera del MVP |
| Sistemas contables externos | El envío permanece interno |
| Alta disponibilidad | Las pruebas asumen una sola instancia |
| Almacenamiento distribuido | Se prueba el adaptador local aprobado |
| Compatibilidad multi-base | Solo PostgreSQL está soportado |
| Recuperación de desastres | Pertenece a despliegue y operación |

Estas exclusiones no reducen la obligación de probar concurrencia funcional, seguridad web, rollback ni persistencia real.

---

## 18. Criterios de aceptación

La estrategia puede aprobarse cuando:

1. cubre únicamente las reglas incluidas en el MVP;
2. distingue pruebas unitarias, integración y sistema;
3. exige PostgreSQL Testcontainers y prohíbe sustitutos de persistencia;
4. verifica Flyway desde una base vacía;
5. prueba `TransactionTemplate`, rollback y `READ_COMMITTED`;
6. prueba el bloqueo pesimista con transacciones concurrentes reales;
7. verifica ausencia de reintentos automáticos;
8. prueba ambos resultados atómicos de VR-008;
9. cubre todas las máquinas de estado del MVP;
10. verifica PRG con `303` y los códigos HTTP aprobados;
11. mantiene Spring Security y CSRF habilitados en integración;
12. cubre sesión única, cookies, encabezados, limitador y proxy;
13. cubre registro, almacenamiento, compensación y entrega de Evidencias;
14. verifica límites arquitectónicos;
15. evita tiempo, red y datos compartidos no controlados;
16. define ejecución Maven y gates de CI;
17. utiliza cobertura de código solo como guardrail;
18. mantiene trazabilidad entre requisitos y pruebas;
19. no incorpora navegador automatizado, carga o penetración al alcance del MVP;
20. permanece coherente con las líneas base técnicas aprobadas.

---

## 19. Documentos relacionados

- Validation Rules v0.2 — Reglas de Validación.
- State Machine v0.3 — Máquina de Estados.
- Use Cases v0.2 — Casos de Uso.
- MVP Scope v0.3 — Alcance del MVP.
- ADR-0002 — Estilo arquitectónico.
- ADR-0003 — Stack tecnológico.
- ADR-0004 — Persistencia y concurrencia.
- ADR-0005 — Autenticación y sesión.
- Architecture Overview v0.1 — Visión General de Arquitectura.
- Data Model v0.1 — Modelo de Datos.
- API Design v0.1 — Diseño de API.
- Security Design v0.1 — Diseño de Seguridad.

---

## 20. Conclusión

La estrategia verifica el MVP mediante lógica aislada, PostgreSQL real, transacciones controladas, pruebas HTTP, seguridad habilitada y flujos completos.

El criterio principal de aprobación es la cobertura de comportamiento crítico: reglas, estados, VR-008, concurrencia, seguridad y Evidencias. Las métricas de código complementan esa evidencia sin sustituirla.