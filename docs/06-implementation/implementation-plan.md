# Plan de Implementación

**Versión:** v0.1

**Estado:** Candidata a línea base

**Estado documental:** Incorporada en el repositorio; revisión final y aprobación pendientes

**Fase:** 06 — Implementación

**Producto:** Operational Close Validator

---

## 1. Propósito

Este documento convierte las líneas base aprobadas de Operational Close Validator en una secuencia de implementación verificable.

Define:

- orden de construcción;
- dependencias entre incrementos;
- entregables y criterios de terminado;
- pruebas obligatorias;
- estrategia de ramas y pull requests;
- primer vertical slice;
- puntos de revisión;
- condiciones para utilizar Codex;
- condiciones de entrada a despliegue.

Este plan no rediseña el producto. La implementación materializa las decisiones aprobadas en Product Discovery [descubrimiento de producto], Domain Analysis [análisis del dominio], System Behavior [comportamiento del sistema], Product Scope [alcance del producto] y Technical Design [diseño técnico].

Cualquier contradicción o necesidad de ampliar alcance debe resolverse documentalmente antes de modificar código.

---

## 2. Fuentes obligatorias

La implementación se rige por:

### 2.1. Producto y dominio

- Problem Map v0.2.1;
- Problem Statement v0.2;
- Product Thesis v0.2;
- Current Workflow v0.2;
- Failure Mode Analysis v0.1;
- Validation Rules v0.2;
- Domain Model v0.3;
- State Machine v0.3;
- Use Cases v0.2;
- MVP Scope v0.3.

### 2.2. Decisiones y diseño técnico

- ADR-0001 a ADR-0005;
- Architecture Drivers [impulsores de arquitectura] v0.1;
- Architecture Overview [visión general de arquitectura] v0.1;
- Data Model [modelo de datos] v0.1;
- API Design [diseño de API] v0.1;
- Security Design [diseño de seguridad] v0.1;
- Testing Strategy [estrategia de pruebas] v0.1;
- Deployment Strategy [estrategia de despliegue] v0.1;
- Technical Design Baseline Review [revisión integral de la línea base técnica] v0.1.

Ante una discrepancia:

1. detener el incremento afectado;
2. identificar los documentos en conflicto;
3. no resolver la ambigüedad por conveniencia;
4. corregir documentación o aprobar una ADR;
5. reanudar solo después de cerrar la decisión.

---

## 3. Alcance congelado

### 3.1. Incluido

- un usuario responsable preconfigurado;
- form login, sesión HTTP y CSRF;
- Cierres Operativos;
- Eventos `INCOME`, `EXPENSE`, `DISCOUNT` y `CANCELLATION`;
- Evidencias de Soporte;
- Autorizaciones;
- reglas VR-001, VR-002, VR-003, VR-006 y VR-008;
- Alertas;
- validación y revalidación;
- consolidación;
- envío interno a contabilidad;
- estados `PREPARATION`, `BLOCKED`, `VALIDATED` y `SENT_TO_ACCOUNTING`;
- PostgreSQL, JPA y Flyway;
- HTML renderizado en servidor;
- pruebas automatizadas;
- imagen OCI y una sola instancia activa.

### 3.2. Excluido

- múltiples usuarios;
- roles reales;
- MFA, OAuth o JWT;
- recuperación de contraseña;
- endpoints JSON públicos;
- integración contable externa;
- reapertura de Cierres enviados;
- VR-004, VR-005 y VR-007;
- event sourcing;
- múltiples instancias;
- alta disponibilidad;
- Kubernetes;
- object storage;
- antivirus;
- pruebas de carga o penetración obligatorias.

Ninguna exclusión puede incorporarse como ajuste menor dentro de un incremento.

---

## 4. Principios de implementación

### 4.1. Incrementos verticales

Cada incremento funcional atraviesa, cuando aplique:

```text
Presentation
Application
Domain
Infrastructure
Database
Tests
```

No se considera terminado un incremento que solo agregue tablas, entidades, controladores, vistas o servicios sin comportamiento observable.

### 4.2. Dirección de dependencias

Se conserva:

```text
Presentation → Application → Domain
Infrastructure → puertos definidos hacia el interior
```

No se permite:

- Dominio dependiente de Spring o JPA;
- entidades JPA usadas como modelo de Dominio;
- controladores accediendo a repositorios;
- transacciones de negocio abiertas en Presentación;
- reglas de negocio en plantillas;
- H2 para persistencia;
- reintentos automáticos de conflictos o deadlocks;
- estado de negocio en la sesión HTTP.

### 4.3. Main siempre estable

`main` debe permanecer compilable, verificable, sin pruebas deshabilitadas, sin secretos y sin migraciones rotas.

---

## 5. Estrategia de ramas y commits

### 5.1. Ramas

```text
main
docs/implementation-plan
feat/<increment-id>-<short-description>
fix/<short-description>
test/<short-description>
chore/<short-description>
```

Ejemplos:

```text
feat/ip-00-project-bootstrap
feat/ip-02-authentication-session
feat/ip-04-operational-events
test/vr-008-concurrency
```

Toda rama de implementación parte de `main` actualizado:

```powershell
git switch main  # Cambia a la rama principal.
git pull --ff-only origin main  # Sincroniza sin reescribir historial.
git switch -c feat/ip-00-project-bootstrap  # Crea la rama del incremento.
```

No se usa force push sobre ramas compartidas.

### 5.2. Commits

Formato:

```text
<type>: <imperative summary>
```

Tipos permitidos:

```text
feat fix test refactor chore docs build ci
```

Ejemplos:

```text
build: bootstrap Spring Boot project
feat: add operational close creation
test: cover concurrent close submission
fix: compensate orphan evidence file
```

---

## 6. Gates globales

Cada incremento debe superar:

```powershell
./mvnw verify  # Ejecuta compilación, pruebas, arquitectura y cobertura.
git diff --check  # Detecta errores de espacios y formato.
git status -sb  # Confirma que no existen archivos no esperados.
```

Cuando incluya imagen:

```powershell
docker build .  # Verifica que la imagen OCI pueda construirse.
```

Un gate fallido bloquea el merge. No se permite resolverlo deshabilitando pruebas, reduciendo cobertura sin aprobación, sustituyendo PostgreSQL por H2 o ignorando Flyway.

---

## 7. Definición global de terminado

Un incremento está terminado cuando:

1. cumple su alcance;
2. cubre reglas y transiciones aplicables;
3. rechaza estados inválidos;
4. las migraciones funcionan desde una base vacía;
5. `./mvnw verify` pasa;
6. no contiene pruebas deshabilitadas;
7. no conserva `TODO` o `TBD` bloqueantes;
8. sanitiza errores técnicos;
9. no expone secretos en logs;
10. fue revisado manualmente;
11. actualiza documentación afectada;
12. deja `main` estable después del merge.

Cobertura mínima para Dominio y Aplicación:

```text
instructions ≥ 80%
branches ≥ 70%
```

La cobertura no sustituye escenarios críticos.

---

## 8. Secuencia de implementación

```text
IP-00  Bootstrap y gates
IP-01  Esqueleto arquitectónico
IP-02  Identidad, autenticación y sesión
IP-03  Ciclo básico del Cierre Operativo
IP-04  Eventos Operativos
IP-05  Evidencias y Autorizaciones
IP-06  Validación, Alertas e invalidación
IP-07  Consolidación
IP-08  VR-008 y envío interno
IP-09  Hardening de seguridad y observabilidad
IP-10  Empaquetado, CI/CD y despliegue
```

Dependencia principal:

```text
IP-00 → IP-01 → IP-02 → IP-03 → IP-04 → IP-05
      → IP-06 → IP-07 → IP-08 → IP-09 → IP-10
```

El orden no se altera sin revisar este plan.

---

## 9. IP-00 — Bootstrap y gates

### Objetivo

Crear una aplicación mínima, reproducible y protegida por CI.

### Incluye

- Maven Wrapper;
- Spring Boot;
- Java LTS aprobado;
- `pom.xml`;
- perfiles `local` y `test`;
- JUnit 5, AssertJ y Mockito;
- Spring Boot Test y MockMvc;
- Testcontainers PostgreSQL;
- Flyway;
- ArchUnit;
- JaCoCo;
- workflow de CI;
- UTF-8 y UTC;
- aplicación arrancable sin lógica de negocio.

### Pruebas

- el contexto arranca;
- PostgreSQL Testcontainers inicia;
- Flyway ejecuta;
- ArchUnit corre;
- `./mvnw verify` pasa desde un clon limpio.

### Salida

Una persona puede clonar el repositorio y verificarlo sin secretos reales ni configuración especial.

---

## 10. IP-01 — Esqueleto arquitectónico

### Objetivo

Materializar los límites del monolito modular antes del negocio.

### Incluye

- módulos `Operational Close Management` e `Identity and Access`;
- capas Domain, Application, Presentation e Infrastructure;
- puertos de repositorio, reloj y actor;
- `AuthenticatedPrincipal(userId, username)`;
- resultados de Aplicación;
- `TransactionTemplate`;
- adaptadores JPA;
- mapeo Dominio/JPA separado;
- `open-in-view=false`;
- reglas ArchUnit.

### Pruebas

- dependencias permitidas;
- Dominio sin Spring/JPA;
- Aplicación sin JPA;
- Presentación sin repositorios;
- transacción explícita con PostgreSQL.

### Salida

La arquitectura admite un caso de uso sin romper límites ni crear capas genéricas innecesarias.

---

## 11. IP-02 — Identidad, autenticación y sesión

### Objetivo

Implementar el único usuario responsable y la sesión segura.

### Incluye

- `user_id = responsible-user`;
- `OCV_AUTH_USERNAME`;
- `OCV_AUTH_PASSWORD_HASH`;
- `{bcrypt}<encoded-password>`;
- aprovisionamiento idempotente;
- form login;
- logout `POST`;
- CSRF;
- una sesión activa;
- sustitución de sesión anterior;
- timeout configurable de 30 minutos;
- `SessionRegistry`;
- `HttpSessionEventPublisher`;
- cookie `JSESSIONID`, HttpOnly, Secure en público, SameSite=Lax, Path `/`, Domain no configurado y cookie de sesión;
- limitador en memoria: 10 fallos en 5 minutos por IP confiable y username normalizado, bloqueo de 5 minutos y sin bloqueo persistente de cuenta;
- fail-fast;
- eventos de seguridad aprobados.

### No incluye

Múltiples cuentas, roles, MFA, OAuth, JWT, remember-me o recuperación de contraseña.

### Pruebas

- aprovisionamiento inicial y repetido;
- login correcto e incorrecto;
- CSRF;
- logout;
- expiración;
- segundo login reemplaza al primero;
- rate limit por IP confiable y username normalizado, bloqueo y desbloqueo después de 5 minutos;
- proxy confiable;
- configuración inválida detiene arranque;
- logs sin secretos.

### Salida

El usuario puede autenticar, mantener una sola sesión y cerrar sesión.

---

## 12. IP-03 — Ciclo básico del Cierre Operativo

### Objetivo

Implementar el primer vertical slice funcional completo.

### Primer vertical slice

```text
login → crear Cierre → persistir → visualizar → listar
```

### Incluye

- UUID;
- período;
- moneda;
- responsable;
- saldo inicial;
- saldo real cuando corresponda;
- estado `PREPARATION`;
- revisión;
- timestamps y actor;
- creación, listado, detalle y edición permitida;
- rutas HTML aprobadas;
- PRG `303`;
- errores `400`, `404` y `409`.

### Restricciones

- importes con `BigDecimal`;
- máximo cuatro decimales;
- saldos no negativos;
- no introducir estado Provisional;
- no implementar envío.

### Pruebas

Creación válida, entrada inválida, UUID inválido, entidad inexistente, edición permitida, edición rechazada, persistencia PostgreSQL, CSRF y PRG.

### Salida

El sistema demuestra el flujo completo Presentation–Application–Domain–Infrastructure–PostgreSQL.

---

## 13. IP-04 — Eventos Operativos

### Objetivo

Implementar Eventos y sus efectos sobre el Cierre.

### Incluye

- `INCOME`, `EXPENSE`, `DISCOUNT`, `CANCELLATION`;
- importe nominal positivo;
- `balanceEffect` derivado;
- `reversedEventId`;
- revisión;
- creación, edición, listado y detalle;
- bloqueo pesimista del Cierre para operaciones que afectan VR-008;
- invalidación preparatoria de resultados dependientes.

### Invariantes de Anulación

- mismo Cierre;
- no auto referencia;
- no Anulación de Anulación;
- importe igual al original;
- efecto inverso;
- una sola Anulación por Evento;
- modificar el original recalcula la Anulación e incrementa revisión.

### Pruebas

Efectos por tipo, referencias inválidas, doble Anulación, cruce entre Cierres, modificación del original, revisión, bloqueo y contrato HTTP.

### Salida

Los Eventos producen efectos consistentes y trazables.

---

## 14. IP-05 — Evidencias y Autorizaciones

### Objetivo

Implementar soporte documental y autorizaciones con historial.

### Evidencias

- `file XOR contentReference`;
- PDF, PNG y JPEG;
- máximo 10 MiB por archivo;
- máximo 12 MiB multipart;
- Apache Tika;
- decodificación de imágenes;
- SHA-256;
- `stored:evidence/{evidenceId}/{sha256}.{extension}`;
- `reference:<opaque-business-reference>`;
- volumen externo al web root;
- transferencia física fuera de la transacción del Cierre;
- vinculación dentro de transacción después del lock;
- compensación si falla la vinculación;
- desactivación lógica;
- consulta histórica;
- endpoint de contenido.

### Autorizaciones

Registro, sustitución, desactivación lógica, vigencia, actor y timestamps.

### Pruebas

Tipo real, extensión, contenido corrupto, path traversal, symlinks, digest incorrecto, archivo faltante, evidencia histórica, referencia sin red ni redirect y compensación.

### Salida

Eventos con Evidencias y Autorizaciones activas o históricas, almacenadas de forma segura.

---

## 15. IP-06 — Validación, Alertas e invalidación

### Objetivo

Implementar VR-001, VR-002, VR-003 y VR-006.

### Incluye

- motor de validación de Dominio;
- una ruta de validar/revalidar;
- Resultado de Validación inmutable;
- vigencia e invalidación;
- causas, actor y fecha;
- creación de Alertas;
- estados de Alerta aprobados;
- resolución solo después de revalidación satisfactoria;
- transiciones `PREPARATION`, `BLOCKED` y `VALIDATED`;
- estados derivados del Evento.

### Restricciones

- no implementar VR-004, VR-005 ni VR-007;
- no borrar historial;
- editar datos no resuelve automáticamente una Alerta;
- cambios relevantes invalidan resultados y consolidación.

### Pruebas

Cada regla satisfecha y fallida, combinaciones de fallas, Alertas, revalidación, invalidación, historial, transiciones y rechazo persistido con PRG.

### Salida

Un Cierre puede bloquearse, corregirse, revalidarse y alcanzar `VALIDATED`.

---

## 16. IP-07 — Consolidación

### Objetivo

Implementar la consolidación previa al envío.

### Incluye

- saldo inicial;
- totales por tipo;
- efecto neto;
- saldo esperado;
- saldo real;
- diferencia;
- revisión fuente;
- vigencia e invalidación;
- creación y consulta.

### Fórmulas

```text
expectedBalance = initialBalance + sum(balanceEffect)
difference = actualBalance - expectedBalance
```

### Restricciones

- totales nominales no negativos;
- saldos no negativos;
- `balanceEffect` no editable;
- consolidación inválida no puede usarse para envío.

### Pruebas

Mezclas de tipos, Anulaciones, cuatro decimales, revisión fuente, invalidación, estados y persistencia.

### Salida

Un Cierre validado puede producir una consolidación vigente y trazable.

---

## 17. IP-08 — VR-008 y envío interno

### Objetivo

Implementar la operación transaccional crítica del MVP.

### Secuencia obligatoria

```text
begin transaction
lock Close with PESSIMISTIC_WRITE
reject if SENT_TO_ACCOUNTING
reload required data
evaluate VR-008
persist result and trace
apply success or failure state
commit
```

Aislamiento:

```text
READ_COMMITTED
```

No existen reintentos automáticos.

### Falla de VR-008

- persiste resultado fallido y causa;
- persiste trazabilidad;
- `VALIDATED` pasa a `BLOCKED`;
- no crea envío exitoso;
- responde `303` después de persistir el rechazo.

### Éxito de VR-008

- persiste resultado actual, fecha y usuario;
- crea registro de envío interno;
- pasa a `SENT_TO_ACCOUNTING`;
- todo ocurre en una sola transacción.

### Concurrencia

Prueba con dos transacciones reales: solo una completa, no existe doble envío, no quedan estados parciales y no hay retries.

### Rollback

Probar fallas después de evaluar, persistir resultado, crear intento y cambiar estado. Ninguna deja combinaciones imposibles.

### Salida

El Cierre se envía internamente una sola vez o queda bloqueado con causa persistida.

---

## 18. IP-09 — Hardening de seguridad y observabilidad

### Objetivo

Completar controles transversales antes del despliegue.

### Incluye

- HSTS;
- CSP;
- `X-Content-Type-Options`;
- `X-Frame-Options`;
- `Referrer-Policy`;
- `Permissions-Policy`;
- `Cache-Control`;
- proxy confiable mediante `OCV_TRUSTED_PROXY_CIDRS`;
- errores sanitizados;
- correlation ID;
- logs estructurados;
- eventos de seguridad;
- startup, liveness y readiness;
- validación de configuración y volumen.

Liveness no depende de PostgreSQL. Readiness sí depende de migraciones, PostgreSQL, volumen y configuración válida.

### Pruebas

Encabezados, cookie, CSRF, proxy confiable/no confiable, fail-fast, health checks, ausencia de secretos y error técnico sin Alerta de negocio.

### Salida

La aplicación cumple los controles de seguridad y operación aprobados.

---

## 19. IP-10 — Empaquetado, CI/CD y despliegue

### Objetivo

Construir un artefacto desplegable, promovible y recuperable.

### Incluye

- Dockerfile multietapa;
- imagen OCI inmutable;
- runtime Java compatible;
- usuario no root;
- configuración externa;
- volumen persistente;
- versión y commit;
- publicación por digest;
- Staging;
- smoke tests;
- aprobación manual para Production;
- backups, restore y rollback.

### Variables aprobadas

```text
SPRING_PROFILES_ACTIVE
OCV_ENVIRONMENT
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
OCV_AUTH_USERNAME
OCV_AUTH_PASSWORD_HASH
OCV_SESSION_EXPIRY_MINUTES
OCV_LOGIN_MAX_FAILURES
OCV_LOGIN_WINDOW_SECONDS
OCV_LOGIN_BLOCK_SECONDS
OCV_TRUSTED_PROXY_CIDRS
OCV_EVIDENCE_STORAGE_PATH
OCV_EVIDENCE_MAX_FILE_SIZE_BYTES
OCV_EVIDENCE_MAX_REQUEST_SIZE_BYTES
OCV_BUSINESS_TIME_ZONE
```

### Flyway

- aplica antes de Readiness;
- Hibernate no crea tablas;
- una migración fallida bloquea publicación;
- no existe downgrade automático;
- una migración destructiva requiere backup, Staging y procedimiento de restauración.

### Smoke tests

Staging verifica flujo representativo. Production verifica únicamente operaciones no destructivas aprobadas y reutiliza exactamente el digest probado.

### Salida

La misma imagen probada en Staging puede promoverse manualmente a Production.

---

## 20. Matriz de capacidades

| Capacidad | Incremento |
|---|---|
| Build reproducible y CI | IP-00 |
| Límites arquitectónicos | IP-01 |
| Identidad y sesión | IP-02 |
| Cierres | IP-03 |
| Eventos y Anulaciones | IP-04 |
| Evidencias y Autorizaciones | IP-05 |
| VR-001, VR-002, VR-003, VR-006 | IP-06 |
| Alertas | IP-06 |
| Consolidación | IP-07 |
| VR-008 y envío | IP-08 |
| Seguridad transversal | IP-02 / IP-05 / IP-09 |
| Health checks | IP-09 |
| Imagen y despliegue | IP-10 |

---

## 21. Estrategia de pruebas

### Dominio

Invariantes, reglas, estados, Anulaciones, invalidaciones, Alertas, consolidación y VR-008.

### Aplicación

Coordinación, puertos, actor, reloj, transacciones, resultados y ausencia de retries. Mockito solo para puertos aislados.

### Persistencia

Testcontainers PostgreSQL para mappings, constraints, queries, locks, Flyway, rollback y concurrencia. H2 está prohibido.

### Presentación

MockMvc para rutas, validación, `400`, `403`, `404`, `409`, `500`, PRG `303`, vistas, CSRF, principal y multipart.

### Sistema

Flujos completos de login, Cierre, Eventos, Evidencias, Autorizaciones, validación, corrección, consolidación, envío, rechazo VR-008 y concurrencia.

No se exige navegador automatizado en el MVP.

---

## 22. Migraciones

Toda modificación del esquema utiliza Flyway.

Convención de referencia:

```text
V001__create_identity_tables.sql
V002__create_operational_close_tables.sql
V003__create_operational_event_tables.sql
```

No se permite:

- editar una migración ya publicada en `main`;
- reutilizar números;
- usar `ddl-auto=create`;
- crear tablas manualmente;
- introducir cambios destructivos sin procedimiento.

Cada migración se prueba desde base vacía y sobre el estado anterior.

---

## 23. Revisión por incremento

### Entrada

- alcance delimitado;
- documentos fuente identificados;
- pruebas críticas enumeradas;
- exclusiones confirmadas;
- rama creada desde `main`.

### Intermedia

- arquitectura respetada;
- migraciones revisadas;
- invariantes cubiertas;
- diff manejable;
- sin expansión de alcance.

### Salida

- `./mvnw verify`;
- revisión completa del diff;
- prueba manual mínima;
- documentación actualizada;
- aprobación antes del merge.

---

## 24. Uso de Codex

### Permitido

- scaffolding dentro de un incremento aprobado;
- implementación de clases acotadas;
- generación inicial de pruebas;
- refactor mecánico;
- mappings;
- build y CI;
- análisis de errores;
- explicación de diffs.

### Prohibido

- decidir alcance;
- inventar reglas;
- modificar ADR;
- agregar dependencias relevantes sin revisión;
- implementar varios incrementos en un solo cambio;
- hacer merge;
- escribir secretos;
- deshabilitar pruebas;
- cambiar contratos HTTP;
- sustituir PostgreSQL por H2;
- agregar usuarios, JWT o JSON público;
- alterar VR-008.

Toda solicitud a Codex debe incluir incremento, objetivo, documentos fuente, restricciones, archivos permitidos, pruebas y exclusiones.

Todo código generado se revisa línea por línea y se verifica antes de aceptarlo.

---

## 25. Gestión de desviaciones

Se considera desviación cualquier necesidad de:

- cambiar una decisión aprobada;
- agregar una dependencia relevante;
- modificar contrato HTTP;
- cambiar estados;
- ampliar alcance;
- relajar seguridad;
- relajar pruebas;
- cambiar topología.

Proceso:

1. detener el cambio;
2. describir la causa;
3. identificar artefactos afectados;
4. evaluar alternativas;
5. aprobar ADR o corrección documental;
6. actualizar este plan;
7. continuar.

Una desviación no se resuelve solo en código o en la descripción de un PR.

---

## 26. Milestones

### M1 — Foundation

```text
IP-00, IP-01, IP-02
```

Resultado: build reproducible, arquitectura protegida y autenticación operativa.

### M2 — Operational Capture

```text
IP-03, IP-04, IP-05
```

Resultado: Cierres, Eventos, Evidencias y Autorizaciones.

### M3 — Validation and Close

```text
IP-06, IP-07, IP-08
```

Resultado: validación, Alertas, consolidación y envío interno.

### M4 — Release Readiness

```text
IP-09, IP-10
```

Resultado: seguridad, observabilidad, imagen, despliegue y recuperación.

---

## 27. Riesgos y tratamiento

| Riesgo | Tratamiento |
|---|---|
| Mucha infraestructura antes del primer flujo | IP-03 como vertical slice temprano |
| Mezclar Dominio y JPA | Mappings separados y ArchUnit |
| Preparar tarde la concurrencia | Lock del Cierre desde IP-04 |
| Perder historial | Vigencia lógica y revisión incremental |
| Evidencias huérfanas | Compensación y pruebas de fallo |
| Doble envío | Lock pesimista y prueba concurrente |
| Sesión inconsistente | IP-02 antes del negocio |
| CI tardía | IP-00 |
| Migraciones no reproducibles | Flyway y Testcontainers desde el inicio |
| Scope creep | Exclusiones y checkpoints |
| Código generado no revisado | Política de Codex |
| Despliegue no probado | Staging, smoke y restore en IP-10 |

---

## 28. Criterios de entrada a código

El primer incremento puede comenzar cuando:

1. este documento esté aprobado;
2. `docs/implementation-plan` se haya fusionado en `main`;
3. `main` esté sincronizada;
4. el plan esté disponible en `main`;
5. no existan cambios locales pendientes;
6. se cree `feat/ip-00-project-bootstrap` desde `main`;
7. se revise el alcance de IP-00;
8. se confirme que no se reabre diseño técnico.

---

## 29. Criterios de salida del MVP

El MVP está listo para publicación cuando:

1. IP-00 a IP-10 están fusionados;
2. `./mvnw verify` pasa;
3. las reglas incluidas están cubiertas;
4. VR-008 y concurrencia están verificadas;
5. las migraciones aplican desde cero;
6. la imagen OCI se construye;
7. Staging usa el digest candidato;
8. smoke tests pasan;
9. el restore fue probado;
10. no existen hallazgos críticos de seguridad;
11. Production reutiliza el mismo digest;
12. README y CHANGELOG están actualizados;
13. se crea la etiqueta de release.

---

## 30. Pull request del plan

El PR debe contener únicamente:

```text
docs/06-implementation/implementation-plan.md
```

Título propuesto:

```text
docs: establish implementation plan
```

Descripción:

```markdown
## Summary

Establishes the implementation plan for Operational Close Validator.

## Scope

- Documentation only
- Defines IP-00 through IP-10
- Preserves the approved MVP and technical baseline
- Defines branches, gates, tests, milestones and Codex constraints

## Validation

- No production code
- No scope expansion
- No technical decision changes
- Implementation begins only after merge
```

---

## 31. Criterios de aprobación

El plan puede aprobarse cuando:

1. usa únicamente líneas base aprobadas;
2. no agrega capacidades fuera del MVP;
3. define un orden implementable;
4. el primer vertical slice es observable;
5. VR-008 queda precedido por locks e invalidaciones;
6. Evidencias incluyen almacenamiento, compensación y seguridad;
7. autenticación precede al negocio;
8. CI comienza en IP-00;
9. PostgreSQL y Flyway se usan desde el inicio;
10. H2 permanece excluido;
11. los gates son obligatorios;
12. cada incremento tiene salida verificable;
13. Codex no decide alcance;
14. las desviaciones requieren decisión formal;
15. el código comienza únicamente después del merge.

---

## 32. Conclusión

Operational Close Validator se implementará mediante incrementos verticales pequeños y verificables.

El orden protege primero reproducibilidad, arquitectura, identidad y persistencia; después comportamiento de negocio, concurrencia, seguridad y despliegue.

La implementación no reinterpreta las líneas base. Las convierte en código probado, trazable y desplegable dentro del alcance congelado del MVP.