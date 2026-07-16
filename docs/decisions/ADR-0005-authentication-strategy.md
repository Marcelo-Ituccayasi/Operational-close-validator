# ADR-0005: Estrategia de autenticación y sesión

**Estado:** Propuesta

**Estado documental:** Borrador

**Fecha:** 2026-07-16

**Producto:** Operational Close Validator

---

## 1. Contexto

El MVP de Operational Close Validator requiere un único usuario responsable preconfigurado con autenticación básica.

En este contexto, **autenticación básica** significa una autenticación mínima mediante nombre de usuario y contraseña. No significa utilizar el protocolo HTTP Basic Authentication.

El alcance aprobado no incluye:

- registro público de usuarios;
- administración de usuarios;
- recuperación de contraseña;
- múltiples roles reales;
- inicio de sesión mediante proveedores externos;
- autenticación multifactor;
- delegación de identidad;
- integración con un directorio corporativo.

ADR-0002 establece un módulo de soporte denominado Identidad y Acceso. Gestión del Cierre Operativo no depende del mecanismo de autenticación ni de sesión: sus casos de uso reciben un principal autenticado mediante un contrato explícito.

ADR-0003 selecciona Java, Spring Boot, Spring MVC y Thymeleaf.

ADR-0004 selecciona PostgreSQL, Spring Data JPA, Hibernate y Flyway, y reserva a Identidad y Acceso la propiedad del usuario preconfigurado y de sus credenciales o referencias de credenciales.

Esta ADR debe seleccionar una estrategia que:

- proteja las credenciales;
- resulte adecuada para una aplicación web renderizada en servidor;
- proporcione una sesión corta y revocable;
- mantenga el Dominio independiente de Spring Security;
- evite introducir JWT, OAuth 2.0 u otros mecanismos no requeridos;
- permita desplegar públicamente el MVP sin credenciales dentro del repositorio;
- mantenga trazabilidad del usuario responsable.

---

## 2. Problema de decisión

¿Qué estrategia de autenticación, aprovisionamiento de credenciales, sesión y autorización permite proteger el MVP público con un único usuario responsable, sin ampliar el alcance funcional ni acoplar el Dominio a Spring Security?

---

## 3. Alcance de la decisión

### Esta ADR decide

- mecanismo de autenticación para navegador;
- estrategia de sesión;
- almacenamiento lógico del usuario preconfigurado;
- aprovisionamiento y rotación de credenciales;
- codificación de contraseña;
- identidad estable del principal autenticado;
- política general de autorización;
- protección CSRF;
- protección frente a fijación de sesión;
- límite de sesiones concurrentes;
- tiempo de expiración por inactividad;
- comportamiento de cierre de sesión;
- política de cookies de sesión;
- tratamiento de intentos fallidos;
- trazabilidad de eventos de autenticación;
- puertos entre Identidad y Acceso y Gestión del Cierre Operativo;
- estrategia mínima de pruebas de seguridad.

### Esta ADR no decide

- administración de múltiples usuarios;
- autoservicio de cambio o recuperación de contraseña;
- autenticación multifactor;
- OAuth 2.0;
- OpenID Connect;
- SAML;
- LDAP;
- passkeys;
- autenticación para aplicaciones móviles;
- emisión de JWT;
- autenticación entre servicios;
- proveedor de secretos;
- proveedor de despliegue;
- certificados TLS;
- política completa de encabezados HTTP;
- Content Security Policy;
- observabilidad detallada;
- almacenamiento físico de Evidencias de Soporte.

Los aspectos operativos restantes se documentarán en `security-design.md` y `deployment-strategy.md`.

---

## 4. Restricciones y decisiones heredadas

### 4.1. Restricciones del producto

- Existe un único usuario responsable preconfigurado.
- No existe registro público.
- No existe matriz real de roles en el MVP.
- Gestión y contabilidad se simulan dentro del mismo flujo.
- Toda acción relevante debe asociarse al usuario autenticado.
- La aplicación se despliega públicamente.
- El estado Enviado a contabilidad es terminal.
- Las credenciales no pueden formar parte del repositorio público.

### 4.2. Reglas heredadas de ADR-0002

- Identidad y Acceso es un módulo de soporte.
- Gestión del Cierre Operativo no depende de la implementación interna de Identidad y Acceso.
- Los adaptadores de entrada reciben la sesión o credencial.
- Los casos de uso reciben un principal autenticado.
- El Dominio no conoce protocolos de autenticación ni mecanismos de sesión.
- Identidad y Acceso no depende de Gestión del Cierre Operativo.
- La composición ocurre fuera del Dominio.

### 4.3. Reglas heredadas de ADR-0003 y ADR-0004

- La interfaz utiliza Spring MVC y Thymeleaf.
- La aplicación es una sola unidad desplegable.
- PostgreSQL es la persistencia relacional.
- Spring Data JPA y Hibernate permanecen en Infraestructura.
- Flyway administra el esquema.
- Las entidades del Dominio no contienen anotaciones técnicas.
- Los secretos se proporcionan mediante configuración externa.
- La inicialización de credenciales no se realiza mediante migraciones que incorporen secretos al repositorio.

---

## 5. Impulsores de la decisión

| ID | Criterio | Descripción | Prioridad | Peso |
|---|---|---|---|---:|
| AUTH-001 | Protección de credenciales | Evitar texto plano, hashes inseguros y credenciales dentro del repositorio | Crítica | 5 |
| AUTH-002 | Adecuación al navegador | Integración natural con Spring MVC y Thymeleaf | Alta | 3 |
| AUTH-003 | Control de sesión | Expiración, revocación, cierre de sesión y protección frente a fijación | Alta | 3 |
| AUTH-004 | Protección CSRF | Proteger operaciones que modifican estado | Alta | 3 |
| AUTH-005 | Simplicidad operativa | Evitar infraestructura y protocolos innecesarios | Alta | 3 |
| AUTH-006 | Coherencia arquitectónica | Mantener Identidad y Acceso separado del Dominio operativo | Alta | 3 |
| AUTH-007 | Testabilidad | Permitir pruebas de login, sesión, autorización y CSRF | Alta | 3 |
| AUTH-008 | Ausencia de dependencia externa | No requerir un proveedor de identidad para el MVP | Alta | 3 |
| AUTH-009 | Evolución controlada | Permitir sustituir el mecanismo posteriormente sin modificar el Dominio | Media | 1 |

### 5.1. Escala

- 1 — Muy deficiente.
- 2 — Deficiente.
- 3 — Aceptable.
- 4 — Buena.
- 5 — Muy buena.

---

## 6. Alternativas consideradas

### Alternativa A — HTTP Basic Authentication

#### Descripción

El navegador envía nombre de usuario y contraseña mediante el encabezado `Authorization` en cada solicitud autenticada.

#### Ventajas

- Configuración inicial reducida.
- No requiere una pantalla de inicio de sesión personalizada.
- No requiere una sesión de aplicación para autenticar cada solicitud.
- Adecuado para herramientas técnicas y clientes no interactivos.

#### Desventajas

- El navegador reutiliza la credencial de larga duración.
- La experiencia de cierre de sesión es limitada.
- No se ajusta naturalmente a una aplicación Thymeleaf orientada a formularios.
- Reduce el control sobre expiración, revocación y mensajes de autenticación.
- La credencial se valida repetidamente.
- El término “autenticación básica” del alcance podría confundirse con este protocolo.

#### Riesgos

- Dificultad para implementar una experiencia de sesión clara.
- Mayor exposición operacional de la credencial de larga duración.
- Menor alineación con la interfaz web seleccionada.

---

### Alternativa B — Form login con sesión HTTP del servidor

#### Descripción

El usuario se autentica mediante un formulario HTML.

Spring Security valida la credencial y conserva el contexto autenticado dentro de una sesión HTTP del servidor. El navegador recibe únicamente una cookie de sesión.

La contraseña se valida al iniciar sesión y no en cada solicitud.

#### Ventajas

- Integración natural con Spring MVC y Thymeleaf.
- Protección CSRF disponible para formularios.
- Cierre de sesión e invalidación de sesión explícitos.
- Protección frente a fijación de sesión.
- La contraseña de larga duración se intercambia por una credencial temporal.
- No requiere servicios externos.
- Permite mantener un principal autenticado estable para los casos de uso.

#### Desventajas

- Requiere administrar tiempo de expiración y cookies.
- Las sesiones en memoria se pierden al reiniciar la aplicación.
- Requiere pruebas específicas de CSRF y sesión.
- La aplicación debe proteger el endpoint de login frente a abuso.

#### Riesgos

- Configuración incorrecta de cookies.
- Desactivación accidental de CSRF.
- Filtrado de tipos de Spring Security hacia Aplicación o Dominio.
- Uso de la sesión para almacenar estado de negocio.

---

### Alternativa C — Autenticación stateless mediante JWT

#### Descripción

El usuario se autentica y recibe un token firmado que presenta en solicitudes posteriores.

#### Ventajas

- Adecuado para clientes desacoplados.
- No requiere conservar una sesión de autenticación en el servidor.
- Facilita una futura API consumida por múltiples clientes.

#### Desventajas

- No existe un requisito aprobado para clientes externos.
- Introduce emisión, firma, expiración, almacenamiento y revocación de tokens.
- La revocación inmediata requiere mecanismos adicionales.
- Incrementa el riesgo de almacenar el token incorrectamente en el navegador.
- Agrega complejidad a una aplicación renderizada en servidor.

#### Riesgos

- Ampliación tecnológica sin necesidad funcional.
- Errores en expiración, renovación o revocación.
- Aparición de una falsa percepción de seguridad por utilizar tokens.

---

### Alternativa D — Proveedor externo mediante OAuth 2.0 u OpenID Connect

#### Descripción

La aplicación delega la autenticación a un proveedor de identidad externo.

#### Ventajas

- Evita gestionar directamente la contraseña del usuario.
- Facilita autenticación multifactor y políticas corporativas.
- Proporciona una ruta de evolución para múltiples usuarios.

#### Desventajas

- Introduce una dependencia externa.
- Requiere configuración del proveedor, clientes, secretos y redirecciones.
- No está justificado para un único usuario preconfigurado.
- Complica el despliegue reproducible del MVP.
- Puede introducir costos y disponibilidad externa.

#### Riesgos

- Bloqueo del acceso por configuración o disponibilidad del proveedor.
- Complejidad desproporcionada para el alcance aprobado.
- Dependencia de un servicio que no forma parte del producto.

---

## 7. Evaluación comparativa

### 7.1. Matriz de puntuación

| Criterio | Peso | A. HTTP Basic | B. Form + sesión | C. JWT | D. OIDC |
|---|---:|---:|---:|---:|---:|
| AUTH-001 — Protección de credenciales | 5 | 3 | 5 | 4 | 5 |
| AUTH-002 — Adecuación al navegador | 3 | 2 | 5 | 3 | 4 |
| AUTH-003 — Control de sesión | 3 | 1 | 5 | 3 | 4 |
| AUTH-004 — Protección CSRF | 3 | 2 | 5 | 4 | 4 |
| AUTH-005 — Simplicidad operativa | 3 | 5 | 4 | 2 | 2 |
| AUTH-006 — Coherencia arquitectónica | 3 | 5 | 5 | 4 | 3 |
| AUTH-007 — Testabilidad | 3 | 4 | 5 | 4 | 3 |
| AUTH-008 — Ausencia de dependencia externa | 3 | 5 | 5 | 5 | 1 |
| AUTH-009 — Evolución controlada | 1 | 2 | 4 | 5 | 5 |

### 7.2. Puntuación ponderada

| Criterio | Peso | A. HTTP Basic | B. Form + sesión | C. JWT | D. OIDC |
|---|---:|---:|---:|---:|---:|
| AUTH-001 | 5 | 15 | 25 | 20 | 25 |
| AUTH-002 | 3 | 6 | 15 | 9 | 12 |
| AUTH-003 | 3 | 3 | 15 | 9 | 12 |
| AUTH-004 | 3 | 6 | 15 | 12 | 12 |
| AUTH-005 | 3 | 15 | 12 | 6 | 6 |
| AUTH-006 | 3 | 15 | 15 | 12 | 9 |
| AUTH-007 | 3 | 12 | 15 | 12 | 9 |
| AUTH-008 | 3 | 15 | 15 | 15 | 3 |
| AUTH-009 | 1 | 2 | 4 | 5 | 5 |
| **Total** |  | **89** | **131** | **100** | **93** |

### 7.3. Fundamento de la evaluación

La alternativa A es simple, pero no proporciona una experiencia de sesión y cierre adecuada para la aplicación web seleccionada.

La alternativa B obtiene la mayor puntuación porque combina controles de sesión, CSRF, integración con Thymeleaf y ausencia de dependencias externas.

La alternativa C es válida para una API desacoplada, pero añade emisión y revocación de tokens sin un cliente externo aprobado.

La alternativa D ofrece controles robustos, pero introduce una dependencia externa no justificada para un único usuario.

---

## 8. Decisión

Se adopta:

**Alternativa B — Form login con sesión HTTP del servidor.**

### 8.1. Componentes seleccionados

| Componente | Decisión |
|---|---|
| Framework de seguridad | Spring Security |
| Mecanismo de autenticación | Form login |
| Persistencia del contexto | Sesión HTTP del servidor |
| Repositorio de sesión | Memoria del proceso Servlet |
| Registro de sesiones concurrentes | `SessionRegistry` en memoria + `HttpSessionEventPublisher` |
| Cookie de sesión | `JSESSIONID` |
| Fuente de usuario | PostgreSQL mediante adaptador de Identidad y Acceso |
| Identificador estable | `responsible-user` |
| Codificación de contraseña | `DelegatingPasswordEncoder` con BCrypt para nuevas credenciales |
| Protección CSRF | Habilitada mediante token en sesión |
| Protección frente a fijación | Cambio del identificador de sesión al autenticar |
| Sesiones concurrentes | Máximo una sesión activa |
| Segundo inicio de sesión | Invalida la sesión anterior |
| Expiración por inactividad | 30 minutos, externalizable |
| Remember-me | Deshabilitado |
| Logout | Solicitud POST con CSRF, invalidación de sesión y eliminación de cookie |
| Autorización de negocio | Usuario autenticado; sin matriz de roles |
| Autenticación HTTP Basic | Deshabilitada |
| JWT | No utilizado |
| OAuth 2.0 / OIDC | No utilizado |

---

## 9. Usuario preconfigurado

### 9.1. Identidad estable

El MVP utiliza una identidad interna estable:

```text
userId: responsible-user
```

El nombre utilizado para iniciar sesión es configurable, pero el identificador interno no cambia.

Las referencias de auditoría utilizan `userId`. Cuando sea útil para lectura histórica, también pueden conservar una copia del nombre de usuario visible en el momento de la acción.

### 9.2. Registro persistente

Identidad y Acceso conserva un único registro lógico con, como mínimo:

- identificador estable;
- nombre de usuario;
- hash de contraseña;
- estado habilitado;
- versión de credencial;
- fecha de aprovisionamiento;
- fecha de actualización.

No se crean:

- tablas de roles;
- permisos configurables;
- perfiles adicionales;
- relaciones de pertenencia;
- flujos de invitación.

La base de datos debe impedir nombres de usuario duplicados.

La aplicación falla al iniciar si existe más de un registro de usuario o si el registro existente utiliza un identificador interno diferente de `responsible-user`.

El MVP no conserva usuarios inactivos adicionales.

### 9.3. Aprovisionamiento

Las credenciales se proporcionan mediante configuración externa:

```text
OCV_AUTH_USERNAME
OCV_AUTH_PASSWORD_HASH
```

`OCV_AUTH_PASSWORD_HASH` contiene un hash completo compatible con `DelegatingPasswordEncoder`, incluyendo el prefijo del algoritmo:

```text
{bcrypt}<hash>
```

La contraseña en texto plano:

- no se incluye en Git;
- no se incluye en migraciones;
- no se incluye en archivos de ejemplo;
- no se registra en logs;
- no se conserva en propiedades de la aplicación;
- no se pasa como argumento visible de línea de comandos en procesos automatizados.

### 9.4. Sincronización al iniciar

Antes de declarar la aplicación lista para recibir tráfico, Identidad y Acceso ejecuta una operación transaccional que:

1. valida que la tabla contenga cero o un registro;
2. verifica que cualquier registro existente tenga el identificador interno `responsible-user`;
3. exige las dos variables de configuración;
4. crea el registro cuando la base está vacía;
5. sincroniza el nombre de usuario y el hash configurados;
6. incrementa la versión de credencial cuando cambia el hash;
7. registra la actualización para trazabilidad;
8. falla el arranque ante datos incompatibles o configuración ausente.

La configuración externa es la fuente de verdad de la credencial del usuario preconfigurado.

### 9.5. Rotación

La contraseña se rota mediante:

1. generación externa de un nuevo hash;
2. actualización segura de `OCV_AUTH_PASSWORD_HASH`;
3. nuevo despliegue o reinicio controlado;
4. invalidación de las sesiones en memoria como consecuencia del reinicio;
5. sincronización del hash y de la versión de credencial;
6. verificación del nuevo acceso.

La recarga dinámica de credenciales sin reiniciar la aplicación queda fuera del MVP.

El MVP no contiene una pantalla para cambiar contraseña.

### 9.6. Generación del hash

El repositorio deberá proporcionar una utilidad local y revisable que:

- solicite la contraseña mediante entrada interactiva;
- no muestre la contraseña;
- no la almacene;
- produzca un hash con prefijo;
- utilice el mismo `PasswordEncoder` de la aplicación;
- permita comprobar el tiempo de verificación.

No se utiliza `NoOpPasswordEncoder`.

No se utiliza texto plano ni SHA-256 directo para almacenar contraseñas.

---

## 10. Política de codificación de contraseña

Se utiliza `DelegatingPasswordEncoder`.

Para las credenciales nuevas, el identificador de codificación es:

```text
bcrypt
```

El valor persistido utiliza este formato:

```text
{bcrypt}<encoded-password>
```

El factor de trabajo de BCrypt:

- se mide en el entorno de despliegue;
- se ajusta para que una verificación tarde aproximadamente un segundo;
- no será menor que la configuración predeterminada del framework;
- queda fijado en configuración reproducible;
- solo se modifica mediante revisión técnica.

La presencia del prefijo permite cambiar el algoritmo en el futuro sin modificar el contrato de Identidad y Acceso.

---

## 11. Flujo de autenticación

### 11.1. Inicio de sesión

```text
1. El usuario solicita una página protegida.
2. Spring Security redirige a /login.
3. Thymeleaf presenta el formulario con token CSRF.
4. El usuario envía nombre y contraseña mediante POST.
5. Identidad y Acceso busca el usuario preconfigurado.
6. PasswordEncoder compara la contraseña con el hash.
7. Si la autenticación es válida:
   a. se cambia el identificador de sesión;
   b. se crea el contexto autenticado;
   c. se registra el evento de éxito;
   d. se redirige a la página originalmente solicitada o al inicio.
8. Si falla:
   a. no se revela si falló el usuario o la contraseña;
   b. se registra un evento sanitizado;
   c. se aplica el limitador de intentos;
   d. se muestra un mensaje genérico.
```

### 11.2. Principal autenticado

El adaptador de entrada transforma el objeto de Spring Security en un contrato propio:

```text
AuthenticatedPrincipal
- userId
- username
```

Los casos de uso reciben `AuthenticatedPrincipal`.

El Dominio no recibe:

- `Authentication`;
- `SecurityContext`;
- `UserDetails`;
- `GrantedAuthority`;
- `HttpSession`;
- cookies;
- solicitudes HTTP.

### 11.3. Autorización

Dentro del MVP:

- toda operación de negocio requiere autenticación;
- no existe una matriz de roles;
- no existen permisos configurables;
- no se simulan roles mediante autorizaciones técnicas diferentes;
- Gestión y contabilidad continúan representadas en el mismo flujo;
- la autorización se expresa como “usuario responsable autenticado”.

No se define una autoridad técnica `ROLE_RESPONSIBLE` dentro del MVP.

La autorización web utiliza únicamente la condición de usuario autenticado. El identificador `responsible-user` se utiliza para identidad y trazabilidad, no como rol o permiso.

Las reglas de negocio no dependen de cadenas de autoridades.

---

## 12. Rutas y superficie pública

### 12.1. Rutas permitidas sin autenticación

Solo pueden quedar públicas:

- `/login`;
- recursos estáticos estrictamente necesarios para la página de login;
- endpoint de liveness sin información sensible;
- páginas de error que no expongan datos internos.

### 12.2. Rutas protegidas

Requieren autenticación:

- registro y modificación de Eventos Operativos;
- gestión de Evidencias de Soporte;
- gestión de Autorizaciones;
- validaciones;
- Alertas;
- consolidación;
- envío a contabilidad;
- consultas operativas;
- trazabilidad;
- cualquier endpoint JSON de negocio.

### 12.3. CORS

El MVP utiliza la misma procedencia para interfaz y backend.

No se habilita CORS de forma global.

Una futura interfaz alojada en otro origen requiere una nueva decisión o actualización explícita del diseño de seguridad.

---

## 13. Gestión de sesión

### 13.1. Repositorio de sesión

La sesión se conserva en memoria dentro del proceso de la aplicación.

No se incorpora Spring Session ni una tabla de sesiones en el MVP.

Consecuencias aceptadas:

- un reinicio invalida todas las sesiones;
- no existe continuidad de sesión entre múltiples instancias;
- el despliegue inicial utiliza una sola instancia de aplicación.

Escalar a múltiples instancias requiere revisar esta decisión.

### 13.2. Duración

La sesión expira después de 30 minutos de inactividad.

El valor:

- se configura externamente;
- es igual en desarrollo controlado, pruebas de aceptación y producción salvo justificación;
- no puede ser ilimitado;
- se verifica mediante pruebas.

### 13.3. Concurrencia

Se permite una sola sesión activa para `responsible-user`.

Un nuevo inicio de sesión válido:

- crea la nueva sesión;
- invalida la sesión anterior;
- registra el reemplazo de sesión.

El control se implementa mediante un `SessionRegistry` en memoria y un `HttpSessionEventPublisher` que mantiene actualizado el ciclo de vida de las sesiones.

La implementación de `UserDetails` utilizada como principal técnico define `equals` y `hashCode` de manera estable a partir de `userId`, para que el registro de sesiones identifique correctamente al único usuario.

No se bloquea indefinidamente el acceso por conservar una sesión anterior.

### 13.4. Fijación de sesión

Después de una autenticación correcta se cambia el identificador de sesión.

No se deshabilita la protección frente a fijación de sesión.

### 13.5. Estado permitido

La sesión contiene únicamente información técnica necesaria para autenticación y presentación.

No se almacena en sesión:

- el Cierre Operativo como agregado;
- Eventos Operativos;
- Resultados de Validación;
- Alertas;
- consolidaciones;
- decisiones de VR-008;
- datos que deban ser fuente de verdad.

La fuente de verdad permanece en el Dominio y la persistencia.

---

## 14. Política de cookies

La cookie de sesión debe configurarse con:

| Propiedad | Decisión |
|---|---|
| Nombre | `JSESSIONID` |
| `HttpOnly` | Habilitado |
| `Secure` | Obligatorio en el despliegue público |
| `SameSite` | `Lax` |
| Path | `/` |
| Domain | No configurado salvo necesidad demostrada |
| Persistencia | Cookie de sesión; sin expiración de largo plazo |

No se utiliza almacenamiento local del navegador para conservar credenciales o tokens de autenticación.

La aplicación pública requiere HTTPS.

Cuando exista un proxy inverso, la aplicación debe interpretar correctamente el esquema original para no generar cookies inseguras.

---

## 15. Protección CSRF

CSRF permanece habilitado.

Para formularios Thymeleaf:

- las solicitudes `POST`, `PUT`, `PATCH` y `DELETE` requieren token;
- el token se conserva en la sesión;
- los formularios incorporan el campo oculto correspondiente;
- las pruebas verifican la aceptación y el rechazo;
- logout se realiza mediante `POST`;
- no se deshabilita CSRF de manera global.

Un endpoint solo puede excluirse de CSRF cuando:

1. no utiliza autenticación basada en cookie;
2. la exclusión está justificada;
3. cuenta con una protección alternativa;
4. queda documentado en `security-design.md`;
5. dispone de pruebas específicas.

El MVP no requiere exclusiones para operaciones de negocio.

Los formularios `multipart/form-data` utilizados para cargar Evidencias de Soporte deben incluir el token CSRF en el cuerpo del formulario o en un encabezado protegido. El token no se incluye en la URL. La integración se valida mediante pruebas contra el flujo real de carga de archivos.

---

## 16. Cierre de sesión

El cierre de sesión:

1. se ejecuta mediante `POST`;
2. requiere token CSRF;
3. invalida la sesión;
4. elimina la cookie `JSESSIONID`;
5. limpia el contexto de seguridad;
6. registra el evento;
7. redirige a `/login?logout`.

Puede utilizarse el encabezado `Clear-Site-Data` para limpiar cookies del sitio cuando sea compatible con la estrategia de despliegue.

No se implementa logout mediante una solicitud `GET`.

---

## 17. Intentos fallidos y abuso del login

### 17.1. Mensaje de error

El usuario recibe un mensaje genérico:

```text
Credenciales inválidas.
```

La respuesta no revela:

- si el usuario existe;
- si está deshabilitado;
- si la contraseña fue incorrecta;
- el algoritmo de hash;
- detalles internos de la base de datos.

### 17.2. Limitación de intentos

El endpoint de login debe disponer de un limitador configurable.

Valor inicial propuesto:

```text
10 intentos fallidos por 5 minutos
por combinación de dirección IP confiable y nombre de usuario normalizado
```

La dirección IP se obtiene de la conexión directa. Los encabezados reenviados solo se aceptan cuando la aplicación está detrás de un proxy explícitamente confiable; nunca se confía en encabezados aportados directamente por un cliente público.

Al superar el límite:

- se rechazan temporalmente nuevos intentos;
- no se deshabilita permanentemente la cuenta;
- no se modifica el registro del usuario;
- se registra un evento de seguridad;
- el contador puede mantenerse en memoria para la única instancia del MVP.

No se implementa bloqueo persistente de cuenta, porque un atacante podría impedir el acceso al único usuario.

Los valores definitivos se validarán mediante pruebas y se documentarán en `security-design.md`.

---

## 18. Trazabilidad de seguridad

Se registran, como mínimo:

- aprovisionamiento inicial;
- rotación de hash;
- autenticación exitosa;
- autenticación fallida;
- rechazo por limitación de intentos;
- reemplazo de sesión concurrente;
- cierre de sesión;
- expiración de sesión cuando sea observable;
- fallo de configuración de identidad.

Los eventos no incluyen:

- contraseña;
- hash completo;
- cookie;
- identificador completo de sesión;
- token CSRF;
- encabezado `Authorization`;
- secretos de configuración.

Los logs deben evitar registrar parámetros del formulario de login.

La trazabilidad de las operaciones de negocio utiliza `AuthenticatedPrincipal.userId`.

---

## 19. Límites arquitectónicos

### 19.1. Identidad y Acceso

Contiene:

- aprovisionamiento del usuario;
- consulta de credencial;
- adaptación a `UserDetailsService`;
- configuración de `PasswordEncoder`;
- configuración de Spring Security;
- gestión de eventos de autenticación;
- mapeo hacia `AuthenticatedPrincipal`.

### 19.2. Presentación

Contiene:

- página de login;
- formularios Thymeleaf;
- mensajes de autenticación;
- acciones de logout;
- adaptación del principal hacia los casos de uso.

### 19.3. Aplicación

Define:

- contrato `AuthenticatedPrincipal`;
- puertos estrictamente necesarios para obtener el principal;
- casos de uso que exigen un principal autenticado.

Aplicación no conoce filtros, cookies ni sesiones.

### 19.4. Dominio

No depende de:

- Spring Security;
- autenticación;
- sesiones;
- nombres de rol;
- cookies;
- CSRF;
- HTTP.

El Dominio puede recibir el identificador del responsable como dato de auditoría mediante un tipo propio.

---

## 20. Reglas de implementación

1. HTTP Basic Authentication permanece deshabilitado.
2. Form login es el único mecanismo interactivo del MVP.
3. Todas las rutas de negocio requieren autenticación.
4. CSRF permanece habilitado.
5. Logout utiliza `POST`.
6. Remember-me permanece deshabilitado.
7. Solo existe el usuario interno `responsible-user`.
8. No existen registros adicionales de usuarios activos o inactivos.
9. Las credenciales provienen de configuración externa.
10. El repositorio no contiene contraseñas ni hashes reales.
11. El hash utiliza el formato de `DelegatingPasswordEncoder`.
12. BCrypt es el algoritmo de nuevas credenciales.
13. No se utiliza `NoOpPasswordEncoder`.
14. El identificador de sesión cambia después del login.
15. Solo existe una sesión activa.
16. Una sesión nueva invalida la anterior.
17. El control de concurrencia de sesiones utiliza `SessionRegistry` y `HttpSessionEventPublisher`.
18. El principal técnico implementa `equals` y `hashCode` estables mediante `userId`.
19. La sesión expira por inactividad.
20. La cookie pública utiliza `HttpOnly`, `Secure` y `SameSite=Lax`.
21. El principal se adapta a un contrato propio antes de llegar a Aplicación.
22. El Dominio no importa Spring Security.
23. No se almacena estado de negocio en la sesión.
24. No se utilizan JWT.
25. No se integran proveedores de identidad externos.
26. No se definen roles técnicos dentro del MVP.
27. Los mensajes de error no permiten enumerar usuarios.
28. Los intentos de login están limitados.
29. Los encabezados reenviados solo se confían desde proxies explícitamente configurados.
30. Las sesiones se conservan en memoria en el MVP.
31. Un reinicio invalida las sesiones existentes.
32. Una rotación de credencial requiere reinicio e invalida las sesiones.
33. Los eventos de seguridad no registran secretos.
34. Los formularios multipart de Evidencias de Soporte mantienen protección CSRF.
35. Las pruebas verifican autenticación, sesión, CSRF y logout.
36. La seguridad no amplía el alcance funcional del MVP.

---

## 21. Estrategia de pruebas

### 21.1. Aprovisionamiento

Debe verificarse que:

- la aplicación falla sin configuración de credenciales;
- se crea el usuario en una base vacía;
- no se crea un segundo usuario;
- una configuración incompatible impide el arranque;
- una rotación actualiza la versión de credencial;
- el hash nunca aparece en logs.

### 21.2. Autenticación

Debe verificarse que:

- una credencial válida inicia sesión;
- una contraseña inválida falla;
- un usuario inválido produce el mismo mensaje;
- HTTP Basic no autentica;
- una ruta protegida redirige a login;
- una ruta pública mínima permanece accesible.

### 21.3. Sesión

Debe verificarse que:

- el identificador cambia al autenticar;
- la sesión expira;
- una nueva sesión invalida la anterior;
- el `SessionRegistry` elimina sesiones cerradas o expiradas;
- el ciclo de vida se propaga mediante `HttpSessionEventPublisher`;
- logout invalida la sesión;
- el reinicio invalida las sesiones en memoria;
- remember-me no crea cookies persistentes.

### 21.4. CSRF

Debe verificarse que:

- un formulario válido con token funciona;
- una operación sin token es rechazada;
- logout sin token es rechazado;
- los endpoints de negocio no están excluidos;
- Thymeleaf incorpora el token en formularios de escritura;
- la carga multipart de Evidencias de Soporte conserva la protección CSRF;
- el token CSRF no aparece en la URL.

### 21.5. Cookies

Debe verificarse en el perfil público que:

- `HttpOnly` está presente;
- `Secure` está presente;
- `SameSite=Lax` está presente;
- la cookie no contiene información del usuario;
- la cookie se elimina al cerrar sesión.

### 21.6. Límites arquitectónicos

Debe verificarse que:

- Dominio no depende de Spring Security;
- Aplicación no depende de `HttpSession`;
- los controladores no pasan `Authentication` al Dominio;
- `UserDetails` permanece dentro del adaptador;
- el usuario autenticado llega como `AuthenticatedPrincipal`;
- no existen cadenas de roles dentro de las reglas del Dominio.

---

## 22. Consecuencias positivas

- Integración directa con Spring MVC y Thymeleaf.
- Protección CSRF adecuada para formularios.
- La contraseña no se valida en cada solicitud.
- La sesión puede revocarse e invalidarse.
- El Dominio permanece independiente de Spring Security.
- No se introduce infraestructura externa.
- No se requiere una administración completa de identidades.
- La rotación se controla mediante configuración.
- El principal estable mejora la trazabilidad.
- El enfoque es suficiente para un MVP público con un solo usuario.

---

## 23. Consecuencias y costos

- El despliegue debe administrar dos valores de configuración de autenticación; el hash es sensible y el nombre de usuario no se trata como secreto.
- El hash configurado debe sincronizarse con PostgreSQL.
- Una configuración incorrecta puede impedir el arranque.
- Las sesiones se pierden al reiniciar.
- No es posible escalar horizontalmente sin revisar la persistencia de sesión.
- El usuario debe volver a iniciar sesión después de un despliegue.
- La aplicación necesita pruebas de seguridad específicas.
- El limitador en memoria se reinicia con el proceso.
- No existe recuperación automática de contraseña.
- La rotación requiere una operación de despliegue.
- El acceso depende de conservar correctamente el secreto externo.

---

## 24. Alternativas descartadas

### HTTP Basic Authentication

Se descarta porque no proporciona la experiencia de sesión, cierre e integración con formularios requerida por la aplicación Thymeleaf.

### JWT

Se descarta porque no existe una API externa ni un cliente desacoplado que justifique emisión, almacenamiento y revocación de tokens.

### OAuth 2.0 u OpenID Connect

Se descarta porque incorpora una dependencia externa y complejidad desproporcionada para un usuario preconfigurado.

### Sesiones persistidas en base de datos

Se descartan para el MVP porque existe una sola instancia y la invalidación durante un reinicio es aceptable.

La incorporación futura de múltiples instancias requerirá reconsiderar Spring Session, una base compartida o un almacén de sesiones.

### Usuario definido únicamente en memoria

Se descarta como fuente principal porque dificulta conservar identidad estable, versión de credencial y trazabilidad de aprovisionamiento dentro del módulo Identidad y Acceso.

---

## 25. Criterios de cumplimiento

La decisión se considera correctamente implementada cuando:

1. Spring Security protege todas las rutas de negocio;
2. el usuario accede mediante form login;
3. HTTP Basic está deshabilitado;
4. existe exactamente un registro con identificador `responsible-user`;
5. no existen usuarios adicionales activos o inactivos;
6. las credenciales provienen de configuración externa;
7. no existen secretos reales en Git;
8. la contraseña está almacenada mediante BCrypt con prefijo;
9. `DelegatingPasswordEncoder` valida la credencial;
10. CSRF está habilitado;
11. logout requiere `POST` y token;
12. los formularios multipart mantienen protección CSRF;
13. la sesión cambia de identificador al autenticar;
14. existe como máximo una sesión activa;
15. la sesión anterior se invalida cuando comienza una nueva;
16. `SessionRegistry` y `HttpSessionEventPublisher` mantienen el ciclo de vida de sesiones;
17. el principal técnico posee igualdad estable mediante `userId`;
18. la sesión expira por inactividad;
19. remember-me está deshabilitado;
20. la cookie pública tiene atributos seguros;
21. el principal llega a Aplicación mediante un contrato propio;
22. el Dominio no depende de Spring Security;
23. no se definen roles ni permisos configurables;
24. los intentos de login están limitados;
25. la clave de limitación utiliza una dirección IP obtenida de forma confiable;
26. una rotación requiere reinicio e invalida sesiones previas;
27. los eventos no registran secretos;
28. las pruebas cubren los escenarios de la sección 21;
29. el esquema de identidad se administra con Flyway;
30. las entidades JPA de identidad permanecen en Infraestructura;
31. no se introduce gestión de usuarios;
32. el despliegue público utiliza HTTPS.

---

## 26. Documentos relacionados

- Architecture Drivers v0.1 — Impulsores de Arquitectura.
- ADR-0001 — La validación final puede devolver un cierre validado a bloqueado.
- ADR-0002 — Estilo arquitectónico de la aplicación.
- ADR-0003 — Stack tecnológico del MVP.
- ADR-0004 — Estrategia de persistencia y control de concurrencia.
- Validation Rules v0.2 — Reglas de Validación.
- Domain Model v0.3 — Modelo de Dominio.
- State Machine v0.3 — Máquina de Estados.
- Use Cases v0.2 — Casos de Uso.
- MVP Scope v0.3 — Alcance del MVP.

---

## 27. Control de cambios

Modificar esta decisión requiere:

1. identificar el impulsor o requisito que cambió;
2. determinar si el MVP continúa con un solo usuario;
3. evaluar el impacto sobre sesiones, CSRF y trazabilidad;
4. revisar la exposición pública;
5. actualizar `security-design.md`;
6. registrar una nueva ADR cuando se incorporen múltiples usuarios, JWT, MFA o un proveedor externo;
7. actualizar las pruebas de seguridad;
8. incorporar el cambio mediante revisión.

---

## 28. Conclusión

Operational Close Validator utilizará:

- Spring Security;
- form login;
- sesión HTTP del servidor;
- una sola sesión activa;
- expiración por inactividad;
- CSRF habilitado;
- logout mediante `POST`;
- un usuario persistente con identificador estable `responsible-user`;
- credenciales aprovisionadas mediante configuración externa;
- `DelegatingPasswordEncoder` con BCrypt;
- un principal propio desacoplado de Spring Security.

La sesión se conservará en memoria dentro de la única instancia del MVP. Un reinicio, una rotación de credencial o un nuevo inicio de sesión invalidarán sesiones anteriores.

Esta estrategia satisface el alcance de un único usuario preconfigurado, protege las operaciones web y conserva la independencia del Dominio aprobada en ADR-0002.

El documento permanece como propuesta hasta completar su revisión final. `security-design.md` deberá materializar esta decisión y no constituye una condición previa para aceptarla.