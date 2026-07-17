# Revisión Integral de la Línea Base Técnica

**Versión:** v0.1

**Estado:** Candidata a línea base

**Estado documental:** Versión validada; integración y revisión remota pendientes

**Fase:** 05 — Diseño técnico

**Producto:** Operational Close Validator

---

## 1. Propósito

Este documento registra la revisión integral de la línea base de [OCV] Technical Design [diseño técnico].

No introduce decisiones nuevas. Su función es comprobar que los artefactos técnicos aprobados:

- forman un conjunto coherente;
- materializan el alcance del MVP;
- no contradicen las líneas base conceptuales;
- cubren arquitectura, persistencia, API, seguridad, pruebas y despliegue;
- permiten comenzar implementación sin reabrir decisiones cerradas;
- conservan trazabilidad hacia reglas, estados y casos de uso.

La aprobación de este documento cierra la fase de diseño técnico y habilita la preparación de implementación.

---

## 2. Alcance de la revisión

La revisión cubre los siguientes artefactos:

| ID | Artefacto | Propósito |
|---|---|---|
| TD-01 | Architecture Drivers [impulsores de arquitectura] v0.1 | Restricciones y atributos de calidad |
| TD-02 | ADR-0002 | Estilo arquitectónico |
| TD-03 | ADR-0003 | Stack tecnológico |
| TD-04 | ADR-0004 | Persistencia, transacciones y concurrencia |
| TD-05 | ADR-0005 | Autenticación y sesión |
| TD-06 | Architecture Overview [visión general de arquitectura] v0.1 | Estructura del sistema y límites |
| TD-07 | Data Model [modelo de datos] v0.1 | Modelo relacional y persistencia |
| TD-08 | API Design [diseño de API] v0.1 | Contrato HTTP |
| TD-09 | Security Design [diseño de seguridad] v0.1 | Controles de seguridad |
| TD-10 | Testing Strategy [estrategia de pruebas] v0.1 | Verificación automatizada |
| TD-11 | Deployment Strategy [estrategia de despliegue] v0.1 | Construcción, publicación y recuperación |

También se verifica coherencia con:

- Domain Model v0.3;
- Validation Rules v0.2;
- State Machine v0.3;
- Use Cases v0.2;
- MVP Scope v0.3;
- ADR-0001.

---

## 3. Condiciones previas de aprobación

La revisión integral solo puede aprobarse cuando:

1. los once artefactos TD-01 a TD-11 existen en la rama;
2. cada artefacto muestra su estado final aprobado;
3. no permanece metadata temporal de integración o revisión pendiente;
4. no existen marcadores `TODO`, `TBD`, decisiones abiertas o alternativas sin resolver;
5. `git diff --check` no reporta errores;
6. la rama técnica contiene únicamente documentación y archivos esperados;
7. el diff contra `main` fue revisado de forma completa;
8. no existe implementación de producción agregada antes de cerrar el diseño;
9. las decisiones técnicas no amplían el MVP sin una decisión formal;
10. cualquier discrepancia encontrada fue corregida y registrada antes del merge.

---

## 4. Alcance funcional preservado

La línea base técnica conserva exactamente el MVP aprobado.

### 4.1. Usuario y acceso

- existe un único usuario responsable preconfigurado;
- la autenticación utiliza form login y sesión HTTP;
- no existen roles técnicos de negocio;
- no se incorporan múltiples usuarios, recuperación de contraseña, MFA, OAuth, JWT o administración de identidades.

### 4.2. Eventos Operativos

Tipos incluidos:

```text
INCOME
EXPENSE
DISCOUNT
CANCELLATION
```

La Anulación referencia un Evento anterior del mismo Cierre y produce el efecto inverso aprobado.

### 4.3. Reglas

Reglas incluidas:

```text
VR-001
VR-002
VR-003
VR-006
VR-008
```

Reglas fuera del MVP:

```text
VR-004
VR-005
VR-007
```

### 4.4. Estados

Estados del Cierre:

```text
PREPARATION
BLOCKED
VALIDATED
SENT_TO_ACCOUNTING
```

Estados del Evento:

```text
REGISTERED
PENDING_SUPPORT
PENDING_AUTHORIZATION
WITH_OBSERVATIONS
VALIDATED
```

Estados de la Alerta:

```text
ACTIVE
ACKNOWLEDGED
UNDER_REVIEW
RESOLVED
DISCARDED
```

`SENT_TO_ACCOUNTING` es terminal dentro del MVP.

### 4.5. Envío

El envío es interno. No existe integración con un sistema contable externo.

VR-008 se ejecuta inmediatamente antes del envío y puede:

- confirmar el envío y pasar el Cierre a Enviado a contabilidad;
- rechazarlo, persistir la causa y pasar el Cierre a Bloqueado.

---

## 5. Coherencia arquitectónica

### 5.1. Estilo

La solución es un monolito modular por capacidades con puertos y adaptadores.

Existe una sola unidad desplegable.

Módulos iniciales:

```text
Operational Close Management
Identity and Access
```

Capas lógicas:

```text
Presentation
Application
Domain
Infrastructure
```

### 5.2. Dirección de dependencias

Se conserva:

```text
Presentation → Application → Domain
Infrastructure → puertos definidos hacia el interior
```

El Dominio no depende de:

- Spring;
- JPA;
- Servlet;
- Spring Security;
- filesystem;
- red;
- adaptadores.

Aplicación no recibe entidades JPA ni tipos de autenticación del framework.

### 5.3. Fronteras

Los controladores:

- adaptan HTTP;
- validan formato;
- construyen comandos o consultas;
- traducen resultados.

No contienen reglas de negocio, consultas JPA ni límites transaccionales.

Los repositorios JPA y el almacenamiento físico permanecen en Infraestructura.

---

## 6. Coherencia de persistencia y transacciones

### 6.1. Base de datos

La persistencia utiliza:

- PostgreSQL;
- Spring Data JPA;
- Flyway;
- esquema controlado;
- UUID;
- importes decimales exactos;
- restricciones relacionales.

Hibernate valida el esquema y no lo crea automáticamente.

### 6.2. Transacciones

Los límites de consistencia de Aplicación utilizan `TransactionTemplate`.

Aislamiento:

```text
READ_COMMITTED
```

No existen reintentos automáticos ante:

- conflictos;
- timeouts;
- deadlocks;
- comandos fallidos.

### 6.3. Bloqueo

Toda operación que pueda cambiar el resultado de VR-008 bloquea primero el Cierre mediante bloqueo pesimista.

Orden global:

```text
Cierre → entidades dependientes → trazabilidad
```

### 6.4. VR-008

Éxito y rechazo son resultados transaccionales completos.

No se permiten estados parciales como:

- Cierre Enviado sin intento de envío;
- intento exitoso sin estado terminal;
- rechazo sin causa;
- Cierre Bloqueado sin Resultado de VR-008 fallido;
- consolidación vigente después de una modificación relevante.

---

## 7. Coherencia del modelo de datos

Se verifican las siguientes decisiones:

- importe nominal positivo;
- `balanceEffect` derivado;
- Ingreso con efecto positivo;
- Egreso y Descuento con efecto negativo;
- Anulación con efecto inverso al Evento referenciado;
- una sola Anulación por Evento;
- relación dentro del mismo Cierre;
- revisión incremental;
- invalidación de resultados dependientes;
- consolidación invalidada por cambios relevantes;
- historial conservado;
- ausencia de event sourcing;
- ausencia de eliminación física del historial operativo.

Las Evidencias y Autorizaciones utilizan vigencia lógica. Su desactivación no elimina la trazabilidad histórica.

---

## 8. Coherencia del contrato HTTP

La interfaz principal utiliza HTML renderizado en servidor.

Métodos del MVP:

```text
GET
POST
```

No existen endpoints públicos `PUT`, `PATCH` o `DELETE`.

Convenciones:

- rutas en inglés;
- recursos en plural;
- campos `lowerCamelCase`;
- UUID en parámetros de entidad;
- mismo origen;
- CORS global deshabilitado.

Post/Redirect/Get utiliza:

```text
303 See Other
```

Un rechazo de negocio persistido también utiliza `303` después de guardar trazabilidad.

Errores:

| Condición | Respuesta |
|---|---|
| Entrada inválida | `400 Bad Request` |
| No autenticado | Redirección a login |
| CSRF inválido | `403 Forbidden` |
| Entidad inexistente | `404 Not Found` |
| Estado o concurrencia incompatible | `409 Conflict` |
| Error técnico | `500 Internal Server Error` sanitizado |

---

## 9. Coherencia de seguridad

### 9.1. Identidad

Existe exactamente:

```text
user_id = responsible-user
```

Configuración obligatoria:

```text
OCV_AUTH_USERNAME
OCV_AUTH_PASSWORD_HASH
```

El hash utiliza:

```text
{bcrypt}<encoded-password>
```

Una configuración ausente, incompatible o insegura detiene el arranque.

### 9.2. Sesión

Se conserva:

- una sesión activa;
- reemplazo de la sesión anterior;
- timeout de 30 minutos configurable;
- cambio de identificador al autenticar;
- `SessionRegistry`;
- `HttpSessionEventPublisher`;
- remember-me deshabilitado.

### 9.3. Cookie y CSRF

Cookie:

```text
JSESSIONID
HttpOnly
Secure en público
SameSite=Lax
Path=/
Domain no configurado
```

Todo `POST`, incluido login, logout y multipart, exige CSRF.

### 9.4. Proxy y encabezados

Solo se interpretan encabezados reenviados desde proxies incluidos explícitamente en `OCV_TRUSTED_PROXY_CIDRS`.

Se conservan HTTPS, HSTS, CSP, `nosniff`, `DENY`, `no-referrer`, `Permissions-Policy` y `no-store`.

---

## 10. Coherencia de Evidencias de Soporte

Entrada:

```text
file XOR contentReference
```

Namespaces:

```text
stored:evidence/{evidenceId}/{sha256}.{extension}
reference:<opaque-business-reference>
```

Formatos binarios permitidos:

```text
PDF
PNG
JPEG
```

Límites:

```text
archivo: 10 MiB
multipart: 12 MiB
```

Se verifica:

- tipo real;
- extensión segura;
- decodificación de imágenes;
- SHA-256;
- path traversal;
- symlinks;
- permanencia dentro del directorio base.

La transferencia física ocurre fuera de la transacción del Cierre. La vinculación ocurre después del bloqueo y dentro de la transacción. Un rollback intenta compensar el archivo.

Una Evidencia desactivada continúa disponible para consulta histórica.

Una referencia `reference:` no provoca red, filesystem ni redirección y responde `404` en el endpoint binario.

---

## 11. Coherencia de pruebas

La estrategia exige:

- JUnit 5;
- AssertJ;
- Mockito únicamente para puertos aislados;
- Spring Boot Test;
- MockMvc;
- Spring Security Test;
- PostgreSQL Testcontainers;
- Flyway;
- ArchUnit;
- JaCoCo.

Las pruebas de persistencia y concurrencia no utilizan H2.

Se cubren:

- reglas;
- estados;
- transacciones;
- rollback;
- bloqueo;
- dos envíos concurrentes;
- HTTP;
- CSRF;
- sesión;
- Evidencias;
- arquitectura;
- flujos de sistema.

Comando obligatorio:

```powershell
./mvnw verify  # Ejecuta todos los gates del proyecto.
```

---

## 12. Coherencia de despliegue

La aplicación se publica como:

- imagen OCI inmutable;
- una sola instancia activa;
- PostgreSQL persistente y privado;
- volumen persistente de Evidencias;
- proxy HTTPS confiable;
- configuración externa;
- logs en `stdout` y `stderr`.

Entornos:

```text
Local
Test
Staging
Production
```

Production utiliza el mismo digest probado en Staging y requiere promoción manual.

Flyway se ejecuta antes de Readiness. No existe downgrade automático.

Se distinguen:

- Startup;
- Liveness;
- Readiness.

Se definen:

- smoke tests;
- rollback de aplicación;
- restauración de datos;
- backup diario;
- restauración probada.

Alta disponibilidad, múltiples instancias y despliegue horizontal permanecen fuera del MVP.

---

## 13. Matriz de coherencia transversal

| Tema | Fuente principal | Confirmación transversal |
|---|---|---|
| Alcance | MVP Scope | Drivers, API, pruebas y despliegue |
| Reglas | Validation Rules | Dominio, API y pruebas |
| Estados | State Machine | Modelo, transacciones y sistema HTTP |
| VR-008 | ADR-0001 y ADR-0004 | Modelo, API, pruebas y despliegue |
| Usuario único | ADR-0005 | Seguridad, datos, pruebas y despliegue |
| PostgreSQL | ADR-0004 | Modelo, pruebas y despliegue |
| Evidencias | Data Model y Security Design | API, pruebas y despliegue |
| Una instancia | Architecture Overview | Seguridad, pruebas y despliegue |
| HTML mismo origen | ADR-0003 y API Design | Seguridad y despliegue |
| Trazabilidad | Drivers | Datos, seguridad, pruebas y operación |

---

## 14. Exclusiones preservadas

La línea base no incorpora:

- reglas VR-004, VR-005 o VR-007;
- estado Provisional;
- conciliación externa;
- múltiples usuarios;
- roles reales;
- MFA;
- JSON público;
- integración contable externa;
- reapertura de Cierres enviados;
- antivirus;
- almacenamiento distribuido;
- alta disponibilidad;
- múltiples instancias;
- Kubernetes;
- pruebas de carga o penetración;
- event sourcing.

Una incorporación futura requiere decisión y actualización de los artefactos afectados.

---

## 15. Hallazgos y tratamiento

La revisión integral clasifica hallazgos como:

| Severidad | Definición | Tratamiento |
|---|---|---|
| Bloqueante | Contradicción, decisión abierta o riesgo de implementación incorrecta | Corregir antes del PR |
| Mayor | Ambigüedad que afecta varios componentes | Corregir antes de aprobar la revisión |
| Menor | Redacción o metadata sin impacto técnico | Corregir antes del merge |
| Observación | Mejora futura fuera del MVP | Registrar sin ampliar alcance |

No se acepta cerrar la revisión con hallazgos Bloqueantes o Mayores abiertos.

---

## 16. Criterios de entrada a implementación

La implementación puede comenzar únicamente después de:

1. aprobar este documento;
2. revisar el diff completo de `docs/technical-design` contra `main`;
3. abrir y revisar el pull request;
4. fusionar mediante fast-forward o el mecanismo normal aprobado del repositorio, sin reescribir historial;
5. verificar los documentos en `main`;
6. crear la etiqueta de línea base técnica;
7. crear el plan de implementación;
8. crear una rama de implementación desde `main` actualizado.

No se inicia código de producción directamente desde `docs/technical-design`.

---

## 17. Evidencia requerida para el cierre

Antes de aprobar:

```powershell
git status -sb  # Confirma la rama y ausencia de cambios no esperados.

git diff --check main...docs/technical-design  # Verifica el diff completo.

git diff --stat main...docs/technical-design  # Resume archivos y magnitud.

git log --oneline main..docs/technical-design  # Revisa los commits de la fase.
```

Después del merge:

```powershell
git switch main  # Cambia a la rama principal.

git pull --ff-only  # Sincroniza main sin reescribir historial.

git log -5 --oneline --decorate  # Verifica el merge de la línea base.
```

La etiqueta propuesta es:

```text
technical-design-baseline-v0.1
```

La etiqueta se crea únicamente después de verificar `main`.

---

## 18. Siguiente fase

El siguiente artefacto después de cerrar y fusionar esta revisión es:

```text
docs/06-implementation/implementation-plan.md
```

Ese documento debe convertir las líneas base en incrementos implementables sin rediseñar el producto.

Debe definir:

- orden de construcción;
- dependencias entre incrementos;
- criterios de terminado;
- pruebas por incremento;
- estrategia de ramas;
- primer vertical slice;
- puntos de revisión;
- condiciones para utilizar Codex.

---

## 19. Criterios de aceptación

La revisión puede aprobarse cuando:

1. enumera los once artefactos técnicos;
2. confirma el alcance exacto del MVP;
3. verifica arquitectura y dirección de dependencias;
4. verifica persistencia, bloqueo y VR-008;
5. verifica modelo de datos e invalidaciones;
6. verifica contrato HTTP y PRG;
7. verifica identidad, sesión, CSRF y proxy;
8. verifica almacenamiento y entrega de Evidencias;
9. verifica estrategia de pruebas y CI;
10. verifica despliegue, migraciones y recuperación;
11. confirma exclusiones;
12. no introduce decisiones nuevas;
13. no mantiene hallazgos Bloqueantes o Mayores;
14. exige revisión integral del diff;
15. exige merge y verificación en `main`;
16. exige etiqueta antes de implementación;
17. identifica el plan de implementación como siguiente artefacto.

---

## 20. Conclusión

La línea base técnica de Operational Close Validator está diseñada para implementar un MVP consistente, trazable y desplegable mediante un monolito modular, PostgreSQL, interfaz renderizada en servidor, seguridad basada en sesión, pruebas automatizadas y una sola instancia operativa.

La aprobación de esta revisión no autoriza cambios de alcance. Autoriza traducir las decisiones congeladas a un plan de implementación y, después, a código verificable.