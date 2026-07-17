# Diseño de Seguridad

**Versión:** v0.1

**Estado:** Línea base aprobada

**Fase:** 05 — Diseño técnico

**Producto:** Operational Close Validator

---

## 1. Propósito y alcance

Este documento materializa los controles de seguridad del MVP para:

- autenticación, autorización, sesión y CSRF;
- HTTPS, cookies y encabezados HTTP;
- credenciales, secretos y arranque seguro;
- limitación del login y proxy confiable;
- validación, errores y logging;
- carga, almacenamiento y entrega de Evidencias de Soporte;
- pruebas y riesgos aceptados.

No reabre ADR-0005 ni introduce múltiples usuarios, roles, MFA, JWT, OAuth 2.0, OpenID Connect, SAML, LDAP o administración de credenciales desde la aplicación.

Quedan fuera:

- proveedor de despliegue o secretos;
- topología final de red;
- WAF y mitigación volumétrica de DDoS;
- retención definitiva de logs y copias de seguridad;
- cifrado de campos a nivel de aplicación;
- sesiones distribuidas.

---

## 2. Restricciones heredadas

| Fuente | Restricción |
|---|---|
| ADR-0002 | El Dominio no depende de Spring Security |
| ADR-0003 | Spring Boot, Spring MVC y Thymeleaf |
| ADR-0004 | PostgreSQL y una sola unidad desplegable |
| ADR-0005 | Form login, sesión HTTP, un usuario, CSRF y una sesión activa |
| `api-design.md` | HTML renderizado, comandos por `POST`, PRG y multipart |
| `data-model.md` | `responsible-user`, `security_event` y referencia abstracta de Evidencias |

La autorización del MVP significa únicamente:

```text
usuario responsable autenticado
```

Aplicación recibe `AuthenticatedPrincipal` y valida:

```text
userId = responsible-user
```

El Dominio no recibe `Authentication`, `SecurityContext`, `UserDetails`, `HttpSession` ni cookies. Para trazabilidad recibe datos de actor ya adaptados.

---

## 3. Activos, confianza y amenazas

### 3.1. Activos

| Activo | Criticidad |
|---|---|
| Contraseña, hash, sesión y secretos | Crítica |
| Datos operativos, VR-008 y Evidencias | Alta |
| Eventos de seguridad y trazabilidad | Alta |
| Token CSRF | Alta |

### 3.2. Límites de confianza

El navegador es no confiable. No se confía en campos ocultos, nombres de archivo, MIME declarado, actor, estado, vigencia, revisiones ni encabezados reenviados de una conexión no confiable.

La aplicación confía únicamente en configuración validada, principal adaptado, PostgreSQL, almacenamiento configurado y proxies incluidos explícitamente en la lista de confianza.

### 3.3. Amenazas y controles

| Amenaza | Control principal |
|---|---|
| Robo o adivinación de credenciales | HTTPS, BCrypt y limitador de login |
| Enumeración de usuario | Mensaje uniforme |
| Fijación o secuestro de sesión | Cambio de ID, cookie segura y HTTPS |
| CSRF | Token en todo `POST` |
| XSS y clickjacking | Escape de salida, CSP y `DENY` |
| Inyección | Validación por capas y consultas parametrizadas |
| Path traversal y MIME confusion | Claves opacas, detección y `nosniff` |
| Archivo activo o malicioso | Lista cerrada y entrega controlada |
| Alteración del contenido almacenado | SHA-256 verificado al entregar |
| Exposición por errores o logs | Sanitización |
| IP falsificada | Proxy explícitamente confiable |
| DDoS volumétrico | Riesgo delegado a Infraestructura |

---

## 4. Autenticación y autorización

### 4.1. Mecanismo

Se utiliza Spring Security con form login y sesión HTTP. Se deshabilitan HTTP Basic, remember-me, JWT y proveedores externos.

| Ruta | Método | Acceso |
|---|---|---|
| `/login` | `GET` | Público |
| `/login` | `POST` | Público, con CSRF |
| `/logout` | `POST` | Autenticado, con CSRF |
| rutas de negocio | cualquier método | Autenticadas |

También pueden ser públicos recursos mínimos de login, páginas de error sanitizadas y un liveness sin datos sensibles. `/logout` no es público.

### 4.2. Usuario preconfigurado

Existe exactamente un registro en `identity_user` con:

```text
user_id = responsible-user
```

Variables obligatorias, sin valores por defecto:

```text
OCV_AUTH_USERNAME
OCV_AUTH_PASSWORD_HASH
```

El hash utiliza:

```text
{bcrypt}<encoded-password>
```

La configuración externa es la fuente de verdad. En el arranque se:

1. exige cero o un registro;
2. rechaza cualquier `user_id` distinto de `responsible-user`;
3. crea el registro si la tabla está vacía;
4. sincroniza username y hash;
5. incrementa la versión de credencial al rotar;
6. registra aprovisionamiento o rotación;
7. detiene el arranque ante incompatibilidad.

### 4.3. Contraseña y rotación

Se utiliza `DelegatingPasswordEncoder` con BCrypt para credenciales nuevas. El factor de trabajo se mide en el entorno objetivo, busca una verificación cercana a un segundo y no es menor que el valor predeterminado del framework.

La aplicación nunca almacena texto plano, recibe hashes desde una pantalla ni escribe contraseñas o hashes completos en logs.

La rotación requiere generar un nuevo hash, actualizar el secreto, reiniciar o desplegar, sincronizar la credencial e invalidar las sesiones en memoria.

### 4.4. Normalización

El username se recorta, convierte a minúsculas y limita a la longitud permitida.

Todo fallo visible utiliza:

```text
Credenciales inválidas.
```

---

## 5. Sesión, cookies, CSRF y CORS

### 5.1. Sesión

| Propiedad | Decisión |
|---|---|
| Creación | `IF_REQUIRED` |
| Almacenamiento | Memoria del proceso |
| Máximo | Una sesión por usuario |
| Reemplazo | La nueva invalida la anterior |
| Inactividad | 30 minutos, externalizable |
| Fijación | Cambiar ID al autenticar |
| remember-me | Deshabilitado |
| Reinicio | Invalida todas las sesiones |

El control de concurrencia de sesión utiliza `SessionRegistry` y `HttpSessionEventPublisher`. El principal técnico define `equals` y `hashCode` de forma estable a partir de `userId`.

La sesión no contiene Cierres, Eventos, Alertas, Resultados, consolidaciones ni decisiones de VR-008.

### 5.2. Cookie

| Propiedad | Decisión |
|---|---|
| Nombre | `JSESSIONID` |
| `HttpOnly` | Sí |
| `Secure` | Obligatorio en perfiles públicos |
| `SameSite` | `Lax` |
| Path | `/` |
| Domain | No configurado |
| Persistencia | Cookie de sesión |

`Secure` puede deshabilitarse únicamente en desarrollo local sobre `localhost`. No se guardan credenciales ni tokens en local storage o session storage.

### 5.3. Logout

`POST /logout` requiere CSRF, invalida sesión, elimina `JSESSIONID`, limpia el contexto, registra el evento y redirige a `/login?logout`. No existe logout por `GET`.

### 5.4. CSRF y CORS

Todo `POST`, incluido login, logout y multipart, requiere CSRF. El token se envía en el cuerpo o en un encabezado futuro aprobado; nunca en la URL ni en logs.

No existe cookie `_csrf`, variable `OCV_SECRET_CSRF` ni exclusión de negocio.

Interfaz y backend usan el mismo origen. CORS no se habilita globalmente.

---

## 6. HTTPS y encabezados HTTP

HTTPS es obligatorio en pruebas públicas y producción. HTTP solo se permite en `localhost` durante desarrollo.

TLS puede terminar en la aplicación o en un proxy confiable. Las solicitudes HTTP públicas se redirigen o rechazan antes de ejecutar negocio. No se fijan puertos en este documento.

En HTTPS público se envía:

```text
Strict-Transport-Security: max-age=31536000
```

`includeSubDomains` queda deshabilitado salvo que todos los subdominios estén controlados y usen HTTPS.

Encabezados mínimos:

```text
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

CSP mínima:

```text
default-src 'self';
object-src 'none';
base-uri 'self';
frame-ancestors 'none';
form-action 'self';
script-src 'self';
style-src 'self';
img-src 'self';
```

No se usa `unsafe-eval`. Los scripts o estilos inline requieren nonce o hash y revisión explícita.

Login, páginas autenticadas y contenido de Evidencias utilizan:

```text
Cache-Control: no-store
```

---

## 7. Limitación del login y proxy confiable

### 7.1. Limitador

Parámetros iniciales:

```text
10 fallos en 5 minutos
bloqueo temporal de 5 minutos
```

Clave:

```text
IP confiable + username normalizado
```

El contador es en memoria, limitado en tamaño y se reinicia con el proceso. Un login exitoso lo limpia. Los intentos durante el bloqueo no extienden indefinidamente el período. No se bloquea persistentemente el único usuario.

### 7.2. Proxy

Configuración opcional:

```text
OCV_TRUSTED_PROXY_CIDRS
```

Cuando está vacía se ignoran `Forwarded` y `X-Forwarded-*`, y se usa `getRemoteAddr()`.

Cuando está configurada, la conexión directa debe provenir de un CIDR permitido antes de interpretar encabezados reenviados. Un valor inválido detiene el arranque.

---

## 8. Validación, salida y errores

Presentación valida presencia, longitud, formato, UUID, fecha, decimal, catálogo y tamaño de solicitud.

Aplicación valida identidad, autorización, estado, pertenencia, vigencia, referencias y consistencia del comando.

Dominio valida reglas, invariantes, transiciones y VR-001, VR-002, VR-003, VR-006 y VR-008.

Thymeleaf escapa la salida. No se usa `th:utext` con datos no confiables.

El contrato HTTP de errores permanece en `api-design.md`. Las respuestas no incluyen stack traces, SQL, nombres de tablas, rutas físicas, secretos, cookies, tokens, IDs completos de sesión ni detalles internos de Spring Security.

Una solicitud no autenticada redirige a `/login`; CSRF inválido produce `403`; un fallo técnico no crea Alertas de negocio falsas.

---

## 9. Evidencias de Soporte

### 9.1. Formas admitidas

El formulario informa exactamente uno entre:

```text
file
contentReference
```

Aplicación transforma la entrada en una referencia con namespace explícito:

```text
stored:evidence/{evidenceId}/{sha256}.{extension}
reference:<opaque-business-reference>
```

Reglas:

- `stored:` identifica contenido administrado por el puerto de almacenamiento;
- `reference:` identifica una referencia textual sin contenido binario administrado por la aplicación;
- el valor completo debe respetar el límite de 500 caracteres de `content_reference`;
- una referencia `reference:` nunca se envía al adaptador de almacenamiento;
- una referencia `reference:` nunca genera solicitudes HTTP, acceso al sistema de archivos ni redirecciones;
- el valor se conserva como texto y se escapa al presentarlo.

### 9.2. Archivos permitidos

| Propiedad | Valor |
|---|---|
| Máximo por archivo | 10 MiB |
| Máximo multipart | 12 MiB |
| Extensiones | `.pdf`, `.png`, `.jpg`, `.jpeg` |
| MIME | `application/pdf`, `image/png`, `image/jpeg` |

El límite puede reducirse por configuración; aumentarlo requiere revisión. Se rechazan archivos vacíos.

### 9.3. Detección e integridad inicial

No se confía en extensión ni MIME declarado. El adaptador usa Apache Tika solo para identificación, sin extracción de texto.

Debe existir coherencia entre extensión, firma detectada y catálogo permitido. PNG y JPEG deben poder decodificarse. Un tipo desconocido o inconsistente se rechaza antes de crear una Evidencia activa.

No se permiten SVG, HTML, XML, JavaScript, ejecutables, Office ni comprimidos.

Después de validar tamaño, tipo y decodificación, el adaptador calcula SHA-256 sobre el contenido. La extensión final deriva exclusivamente del MIME detectado.

### 9.4. Nombre, referencia y almacenamiento

El nombre original no se usa como ruta ni forma parte del modelo aprobado.

Para un archivo, `content_reference` conserva una clave opaca relativa con el digest esperado:

```text
stored:evidence/{evidenceId}/{sha256}.{extension}
```

Para una referencia textual conserva:

```text
reference:<opaque-business-reference>
```

Para el adaptador local, `OCV_EVIDENCE_STORAGE_PATH` apunta a un directorio absoluto, fuera del classpath y web root, con acceso limitado a la cuenta de la aplicación.

Toda resolución:

1. elimina el prefijo `stored:`;
2. combina la base con la clave relativa;
3. normaliza la ruta;
4. verifica que permanezca bajo la base;
5. rechaza enlaces simbólicos no autorizados;
6. rechaza cualquier salida del directorio.

### 9.5. Transferencia, transacción y compensación

La transferencia y validación física ocurren fuera de la transacción que bloquea el Cierre.

Después de almacenar el objeto, Aplicación inicia la transacción, bloquea el Cierre, vincula la referencia, invalida resultados y consolidación, y confirma o revierte.

Si revierte, intenta eliminar el objeto. Si la compensación falla, registra un evento sanitizado y la limpieza queda manual. La Evidencia no se declara activa.

Desactivar o sustituir una Evidencia no elimina contenido histórico.

### 9.6. Entrega

`GET /supporting-evidence/{evidenceId}/content` requiere autenticación y resuelve el contenido mediante la referencia persistida; nunca acepta una ruta del navegador.

Una referencia `reference:` no posee contenido binario. Su endpoint `/content` responde:

```text
404 Not Found
```

La referencia textual se muestra escapada en la vista de detalle.

Para una referencia `stored:`, el adaptador:

1. resuelve la clave bajo el directorio permitido;
2. lee el contenido;
3. recalcula SHA-256;
4. compara el digest con el incluido en `content_reference`;
5. obtiene el `Content-Type` desde la extensión segura incluida en `content_reference`;
6. verifica que la extensión corresponda al catálogo permitido;
7. entrega el contenido únicamente si todas las verificaciones son satisfactorias.

Una discrepancia de digest:

- impide entregar el contenido;
- produce `500 Internal Server Error`;
- registra un evento operativo sanitizado;
- no modifica automáticamente la Evidencia;
- no genera una Alerta de negocio.

| Tipo | `Content-Disposition` |
|---|---|
| PNG o JPEG | `inline` |
| PDF | `attachment` |

Nombre generado:

```text
evidence-{evidenceId}.{extension}
```

La respuesta usa `nosniff` y `no-store`.

Archivo ausente produce `404`. Metadatos incoherentes, digest inválido o tipo no permitido producen `500`. Nunca se responde `200` con un tipo genérico desconocido.

El contenido de una Evidencia activa o desactivada puede consultarse para trazabilidad. `is_active` controla su participación en validaciones actuales, no su disponibilidad histórica.

### 9.7. Riesgo de malware

Los archivos no se sirven desde recursos estáticos. Los PDF se entregan como descarga.

El MVP no incorpora antivirus ni sandbox. El riesgo se reduce mediante formatos cerrados, límites, detección, SHA-256, autenticación y exclusión de SVG, HTML, Office y comprimidos.

---

## 10. Secretos, datos y arranque seguro

Los secretos se suministran mediante configuración externa o proveedor de secretos; nunca por Git, imágenes públicas, argumentos visibles, logs o páginas de diagnóstico.

`OCV_AUTH_USERNAME` no es secreto, pero es obligatorio. El hash se trata como sensible.

La aplicación no cifra campos. En entornos públicos, PostgreSQL, Evidencias y copias de seguridad deben usar cifrado en reposo de Infraestructura y cuentas técnicas con privilegios mínimos.

La aplicación no arranca cuando:

- faltan username o hash;
- el hash no comienza con `{bcrypt}` o el valor BCrypt codificado es inválido;
- `identity_user` contiene registros incompatibles;
- timeout o limitador son inválidos;
- la configuración de proxy es inválida;
- un perfil público permite cookie sin `Secure`;
- el almacenamiento requerido no existe, no es seguro o no es legible/escribible.

No existe fallback silencioso.

---

## 11. Eventos y logs de seguridad

Se persisten, cuando corresponda:

```text
USER_PROVISIONED
CREDENTIAL_ROTATED
LOGIN_SUCCEEDED
LOGIN_FAILED
LOGIN_RATE_LIMITED
SESSION_REPLACED
LOGOUT
SESSION_EXPIRED
CONFIGURATION_FAILED
```

También se registran operativamente CSRF rechazado, acceso denegado, archivo huérfano y contenido ausente o incoherente.

Campos permitidos: tipo, instante, `userId` cuando existe, username normalizado y limitado, IP confiable, plantilla de ruta, resultado y causa sanitizada.

Campos prohibidos: contraseña, hash completo, cuerpo de login, cookie, token CSRF, ID completo de sesión, `Authorization`, secretos, contenido de Evidencias, rutas físicas y query strings sensibles.

Los eventos persistidos no dependen del nivel de logging técnico. `DEBUG` permanece deshabilitado en producción.

---

## 12. Configuración mínima

| Variable | Obligatoria | Regla |
|---|---:|---|
| `OCV_AUTH_USERNAME` | Sí | Sin valor por defecto |
| `OCV_AUTH_PASSWORD_HASH` | Sí | Formato `{bcrypt}<encoded-password>` |
| `OCV_SESSION_EXPIRY_MINUTES` | No | Predeterminado `30`; finito |
| `OCV_LOGIN_MAX_FAILURES` | No | Predeterminado `10` |
| `OCV_LOGIN_WINDOW_SECONDS` | No | Predeterminado `300` |
| `OCV_LOGIN_BLOCK_SECONDS` | No | Predeterminado `300` |
| `OCV_TRUSTED_PROXY_CIDRS` | No | Vacío significa no confiar en proxy |
| `OCV_EVIDENCE_STORAGE_PATH` | Sí para almacenamiento local | Directorio seguro |
| `OCV_EVIDENCE_MAX_FILE_SIZE_BYTES` | No | Predeterminado `10485760`; no puede superar 10 MiB |
| `OCV_EVIDENCE_MAX_REQUEST_SIZE_BYTES` | No | Predeterminado `12582912`; no puede superar 12 MiB |

`deployment-strategy.md` definirá cómo se suministran estos valores.

---

## 13. Pruebas obligatorias

Deben cubrir:

- aprovisionamiento, incompatibilidad y rotación del único usuario;
- login válido e inválido con mensaje uniforme;
- HTTP Basic y roles técnicos ausentes;
- rutas públicas mínimas y rutas de negocio protegidas;
- cambio de ID, timeout, sesión única y logout;
- `SessionRegistry`, `HttpSessionEventPublisher` y principal estable por `userId`;
- `JSESSIONID` con `HttpOnly`, `Secure` y `SameSite=Lax`;
- CSRF en todo `POST`, incluido multipart;
- CSP, HSTS, `nosniff`, `DENY`, `no-store` y CORS deshabilitado;
- límite de login, bloqueo y proxy confiable;
- archivo vacío, tamaño individual y tamaño multipart;
- extensión, MIME, firma, decodificación, path traversal, symlink y nombre malicioso;
- referencia `stored:` y referencia `reference:`;
- ausencia de solicitudes salientes desde `contentReference`;
- alteración del contenido y discrepancia SHA-256;
- entrega autenticada y `Content-Disposition` correcto;
- Evidencia desactivada disponible para consulta histórica;
- referencia textual sin acceso al endpoint binario;
- rollback y compensación de archivo;
- errores y logs sin secretos.

---

## 14. Riesgos aceptados

| Riesgo | Condición |
|---|---|
| Una sola instancia | Sin alta disponibilidad |
| Sesión y limitador en memoria | Se reinician con el proceso |
| Sin MFA o recuperación | Rotación operativa |
| Sin antivirus | Lista cerrada y entrega controlada |
| Sin cifrado de campos | Cifrado de Infraestructura |
| Limpieza manual de huérfanos | Volumen inicial bajo |
| Sin defensa DDoS en la aplicación | Delegada a Infraestructura |
| PDF potencialmente activo | Entrega como `attachment` |
| Referencias externas | No se descargan desde el servidor |

Estos riesgos no permiten deshabilitar HTTPS, CSRF, limitación del login, validación de archivos ni controles de sesión.

---

## 15. Criterios de aceptación

El diseño puede aprobarse cuando:

1. materializa ADR-0005 sin contradecirlo;
2. conserva exactamente un usuario estable y falla ante identidad incompatible;
3. utiliza `DelegatingPasswordEncoder` y `{bcrypt}`;
4. no introduce roles técnicos ni tipos de Spring Security en Dominio;
5. usa una sesión activa, timeout finito y cambio de ID;
6. usa `JSESSIONID`, `HttpOnly`, `Secure` público, `SameSite=Lax` y `Domain` no configurado;
7. exige CSRF en todo `POST` y no configura una semilla propia;
8. mantiene CORS deshabilitado y HTTPS obligatorio en público;
9. define CSP y encabezados mínimos;
10. limita login por IP confiable y username normalizado;
11. no confía en encabezados reenviados sin proxy permitido;
12. valida Evidencias mediante lista cerrada, claves opacas y SHA-256;
13. distingue referencias `stored:` y `reference:`;
14. no usa nombres originales como rutas ni descarga `contentReference`;
15. mantiene transferencia física fuera de la transacción del Cierre;
16. compensa archivos después de rollback;
17. no expone contenido desconocido con `200`;
18. permite consulta histórica sin reactivar la Evidencia;
19. no registra secretos;
20. falla ante configuración insegura;
21. prueba cada control crítico;
22. permanece coherente con `api-design.md` y `data-model.md`.

---

## 16. Documentos relacionados

- ADR-0002 — Estilo arquitectónico.
- ADR-0003 — Stack tecnológico.
- ADR-0004 — Persistencia y concurrencia.
- ADR-0005 — Autenticación y sesión.
- Architecture Overview v0.1.
- Data Model v0.1.
- API Design v0.1.
- Use Cases v0.2.
- Validation Rules v0.2.
- State Machine v0.3.
- MVP Scope v0.3.

---

## 17. Conclusión

El MVP usa una superficie web de mismo origen protegida por form login, sesión HTTP, CSRF, HTTPS público y controles explícitos de credenciales, cookies, login y Evidencias.

La candidata mantiene una identidad estable sin roles, conserva Aplicación y Dominio desacoplados de Spring Security, falla ante configuración insegura y documenta las limitaciones de una sola instancia sin ampliar el alcance aprobado.