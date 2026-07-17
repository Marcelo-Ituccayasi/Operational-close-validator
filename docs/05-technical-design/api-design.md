# Diseño de API

**Versión:** v0.1

**Estado:** Candidata a línea base

**Estado documental:** Incorporada en el repositorio; correcciones finales y aprobación pendientes

**Fase:** 05 — Diseño técnico

**Producto:** Operational Close Validator

---

## 1. Propósito

Este documento define el contrato HTTP del MVP de Operational Close Validator.

Describe:

- rutas;
- métodos HTTP;
- parámetros de ruta y consulta;
- campos de formularios;
- vistas lógicas;
- redirecciones;
- resultados de negocio;
- errores de entrada, seguridad, concurrencia y operación;
- protección CSRF;
- contrato de carga y consulta de Evidencias de Soporte;
- trazabilidad observable desde la capa web.

El contrato está diseñado para una aplicación web renderizada en servidor mediante Spring MVC y Thymeleaf.

Este documento no define clases Java, firmas internas, entidades JPA, consultas SQL ni estructura física definitiva de paquetes.

---

## 2. Alcance

### 2.1. Incluye

Este documento decide:

- convenciones de rutas;
- nombres públicos de parámetros y campos;
- uso de `GET` y `POST`;
- patrón Post/Redirect/Get;
- vistas lógicas;
- contrato de autenticación y cierre de sesión;
- navegación de Cierres Operativos;
- registro y modificación de Eventos Operativos;
- registro, sustitución y desactivación de Evidencias de Soporte;
- registro, sustitución y desactivación de Autorizaciones;
- validación y revalidación de Eventos Operativos;
- gestión manual permitida de Alertas;
- consolidación;
- ejecución final de VR-008 y envío interno a contabilidad;
- tratamiento HTTP de resultados de negocio;
- validación sintáctica de formularios;
- tratamiento de conflictos de estado y concurrencia;
- integración del principal autenticado;
- protección CSRF;
- reglas generales del contrato multipart;
- criterios de aceptación del contrato.

### 2.2. No incluye

Este documento no decide:

- clases de controladores;
- clases de comandos, consultas o view models;
- nombres finales de paquetes;
- implementación de casos de uso;
- esquema relacional;
- configuración JPA;
- mecanismo físico definitivo de almacenamiento de Evidencias de Soporte;
- tamaño máximo definitivo de archivos;
- catálogo definitivo de tipos MIME permitidos;
- antivirus, checksum o cifrado del contenido;
- diseño visual;
- CSS o JavaScript definitivo;
- estrategia completa de seguridad HTTP;
- estrategia de pruebas;
- proveedor o topología de despliegue;
- observabilidad completa;
- endpoints JSON públicos;
- integración contable externa;
- múltiples usuarios o roles;
- reapertura de cierres enviados.

Los aspectos diferidos se resolverán en `security-design.md`, `testing-strategy.md`, `deployment-strategy.md` o mediante una decisión adicional.

---

## 3. Fuentes y restricciones heredadas

| Fuente | Restricción integrada |
|---|---|
| Use Cases v0.2 [casos de uso] | Flujos UC-001 a UC-009 y comportamiento observable |
| Validation Rules v0.2 [reglas de validación] | VR-001, VR-002, VR-003, VR-006 y VR-008 |
| State Machine v0.3 [máquina de estados] | Transiciones permitidas para cierre, evento y Alerta |
| MVP Scope v0.3 [alcance del MVP] | Un usuario, cuatro tipos de evento y flujo interno de envío |
| ADR-0001 | Una falla final de VR-008 rechaza el envío y devuelve el cierre a Bloqueado |
| ADR-0002 | Presentación depende de Aplicación; las entidades JPA no forman parte del contrato |
| ADR-0003 | Spring MVC, Thymeleaf, formularios HTML y JSON solo ante una necesidad concreta |
| ADR-0004 | Transacciones explícitas, bloqueo del cierre, `READ_COMMITTED` y ausencia de reintentos automáticos |
| ADR-0005 | Form login, sesión HTTP, CSRF y un único usuario preconfigurado |
| Architecture Overview v0.1 [visión general de arquitectura] | Una unidad desplegable, principal adaptado y límite transaccional de VR-008 |
| Data Model v0.1 [modelo de datos] | UUID, estados, revisiones, Anulaciones, consolidaciones e intentos de envío |

---

## 4. Principios del contrato HTTP

### 4.1. Interfaz principal

El MVP utiliza HTML renderizado en servidor.

El contrato inicial no expone endpoints JSON.

La incorporación futura de JSON requiere:

- un caso de uso concreto;
- un consumidor identificado;
- una revisión de autenticación, CSRF y errores;
- actualización de este documento.

### 4.2. Métodos permitidos

| Método | Uso |
|---|---|
| `GET` | Consultar, listar, mostrar detalles y renderizar formularios |
| `POST` | Crear, modificar o ejecutar una acción de negocio |
| `PUT` | No utilizado en el MVP |
| `PATCH` | No utilizado en el MVP |
| `DELETE` | No utilizado en el MVP |

Un `GET` no modifica estado de negocio, no invalida resultados y no ejecuta transiciones.

### 4.3. Post/Redirect/Get

Después de un `POST` procesado correctamente, Presentación responde con:

```text
303 See Other
```

y redirige a una vista `GET`.

Se considera procesado correctamente tanto:

- un resultado de negocio exitoso;
- como un resultado de negocio rechazado que deba persistir estado y trazabilidad.

Ejemplo: una falla de VR-008 es un rechazo de negocio confirmado. No es un fallo técnico del transporte HTTP. Después de persistir el intento, las causas y el estado Bloqueado, el sistema redirige al detalle del cierre con un mensaje flash de error.

### 4.4. Excepción al PRG

Cuando un formulario contiene errores de entrada detectados antes de ejecutar el caso de uso:

- no se ejecuta la operación;
- no se persiste estado parcial;
- se renderiza nuevamente el mismo formulario;
- la respuesta utiliza `400 Bad Request`;
- se conservan los valores aceptables ingresados;
- se muestran errores asociados a campos y, cuando corresponda, un error global.

### 4.5. Ausencia de reintentos automáticos

El servidor no reintenta automáticamente:

- conflictos de bloqueo;
- timeouts transaccionales;
- deadlocks;
- comandos fallidos;
- cargas físicas fallidas;
- envíos rechazados.

El usuario decide si repite la acción después de revisar el estado actual.

### 4.6. Frontera de Aplicación

Los controladores:

- reciben parámetros HTTP;
- validan estructura y formato;
- adaptan el principal autenticado;
- construyen comandos o consultas de Aplicación;
- traducen resultados a vistas, redirecciones y mensajes.

Los controladores no:

- contienen reglas del Dominio;
- abren límites transaccionales de negocio;
- acceden directamente a repositorios JPA;
- exponen entidades JPA;
- deciden transiciones por sí mismos;
- calculan `balanceEffect`, consolidaciones o VR-008.

---

## 5. Convenciones públicas

### 5.1. Idioma y nombres

El contenido interno del documento y las etiquetas visibles se mantienen en español.

Los elementos técnicos públicos utilizan inglés:

- rutas;
- nombres de campos;
- parámetros;
- códigos de estado;
- códigos de tipo;
- identificadores técnicos.

Ejemplos:

```text
/closes/{closeId}
/events/{eventId}
/alerts/{alertId}
periodStart
actualBalance
SENT_TO_ACCOUNTING
```

### 5.2. Rutas

Las rutas utilizan:

- nombres de recursos en plural;
- minúsculas;
- segmentos separados por guion cuando contienen varias palabras;
- UUID en parámetros de ruta;
- verbos únicamente para comandos que no se representan como creación de un recurso independiente.

Patrón:

```text
/resources
/resources/{resourceId}
/resources/{resourceId}/action
/parent-resources/{parentId}/child-resources
```

### 5.3. Campos de formulario

Los campos utilizan `lowerCamelCase`.

Ejemplos:

```text
periodStart
currencyCode
responsibleName
reversedEventId
discardJustification
actualBalance
```

### 5.4. Códigos de catálogo

Los formularios envían los códigos persistidos en inglés.

La interfaz presenta sus etiquetas en español.

Ejemplo:

| Valor enviado | Etiqueta visible |
|---|---|
| `INCOME` | Ingreso |
| `EXPENSE` | Egreso |
| `DISCOUNT` | Descuento |
| `CANCELLATION` | Anulación |

### 5.5. UUID

Todo parámetro de entidad de negocio utiliza UUID.

Un UUID inválido produce:

```text
400 Bad Request
```

Un UUID válido que no identifica una entidad accesible produce:

```text
404 Not Found
```

### 5.6. Fechas e instantes

| Concepto | Formato HTTP |
|---|---|
| Fecha de período o documento | `yyyy-MM-dd` |
| Fecha y hora ingresada por el usuario | `yyyy-MM-dd'T'HH:mm` |
| Instante técnico | No se recibe desde formularios |

Presentación interpreta los valores de fecha y hora según la zona configurada para la aplicación y los transforma al tipo de Aplicación correspondiente.

El usuario no envía:

- `createdAt`;
- `updatedAt`;
- `evaluatedAt`;
- `completedAt`;
- `attemptedAt`;
- actor técnico.

Esos valores provienen del reloj y del principal autenticado.

### 5.7. Importes

Los importes:

- aceptan hasta cuatro decimales;
- no aceptan notación científica;
- utilizan punto como separador técnico;
- se validan contra `numeric(19,4)`;
- se convierten a un tipo decimal exacto;
- nunca utilizan `float` o `double` como contrato de Aplicación.

---

## 6. Respuestas y navegación

### 6.1. Respuestas `GET`

Una consulta exitosa responde:

```text
200 OK
Content-Type: text/html
```

### 6.2. Respuestas `POST`

| Resultado | Respuesta |
|---|---|
| Operación exitosa | `303 See Other` |
| Rechazo de negocio persistido | `303 See Other` |
| Entrada inválida sin ejecutar caso de uso | `400 Bad Request` con formulario |
| Entidad inexistente | `404 Not Found` |
| Estado incompatible sin mutación persistida | `409 Conflict` |
| Conflicto técnico de concurrencia | `409 Conflict` |
| CSRF inválido o ausente | `403 Forbidden` |
| Error técnico inesperado | `500 Internal Server Error` |

### 6.3. Mensajes flash

Las redirecciones pueden transportar una categoría lógica de mensaje:

| Categoría | Uso |
|---|---|
| `success` | Resultado de negocio exitoso |
| `error` | Rechazo de negocio persistido |
| `warning` | Estado que requiere atención sin afirmar éxito |
| `info` | Confirmación o contexto no crítico |

Los detalles internos, excepciones, SQL, rutas físicas y stack traces no se muestran al usuario.

### 6.4. Errores en parámetros de consulta

Un filtro desconocido puede ignorarse únicamente si no altera el significado de la consulta.

Un valor inválido para un filtro conocido produce `400 Bad Request` con un mensaje de entrada inválida.

### 6.5. Paginación

Las listas paginadas utilizan:

| Parámetro | Tipo | Valor inicial |
|---|---|---|
| `page` | entero no negativo | `0` |
| `size` | entero entre `1` y `100` | `20` |

La interfaz conserva filtros al navegar entre páginas.

---

## 7. Autenticación y sesión

### 7.1. Rutas

| Ruta | Método | Propósito |
|---|---|---|
| `/login` | `GET` | Mostrar el formulario |
| `/login` | `POST` | Procesar credenciales mediante Spring Security |
| `/logout` | `POST` | Invalidar la sesión |
| `/` | `GET` | Redirigir a `/dashboard` o `/login` |

### 7.2. Formulario de login

Codificación:

```text
application/x-www-form-urlencoded
```

Campos:

| Campo | Obligatorio | Descripción |
|---|---:|---|
| `username` | Sí | Nombre de inicio de sesión |
| `password` | Sí | Credencial no persistida por la aplicación |
| `_csrf` | Sí | Token CSRF |

### 7.3. Resultados de autenticación

| Resultado | Navegación |
|---|---|
| Autenticación correcta | Redirección a `/dashboard` |
| Credenciales incorrectas | Redirección a `/login?error` |
| Sesión cerrada | Redirección a `/login?logout` |
| Sesión expirada | Redirección a `/login?expired` |
| Sesión sustituida por un segundo login | Redirección a `/login?replaced` |

Los parámetros de consulta de login identifican únicamente una categoría visible; no contienen credenciales ni detalles sensibles.

### 7.4. Sesión

El contrato asume:

- sesión HTTP mantenida por cookie;
- un máximo de una sesión activa para el usuario;
- expiración por inactividad de treinta minutos, externalizable;
- invalidación de sesión al reiniciar la única instancia;
- ausencia de remember-me;
- ausencia de JWT, OAuth 2.0, OpenID Connect y HTTP Basic.

### 7.5. Rutas protegidas

Requieren una sesión autenticada:

- `/dashboard`;
- `/closes/**`;
- `/events/**`;
- `/supporting-evidence/**`;
- `/authorizations/**`;
- `/alerts/**`;
- `/consolidations/**`;
- `/logout`.

Una solicitud de navegador no autenticada se redirige a `/login`.

### 7.6. Principal autenticado

Presentación adapta el principal técnico a:

```text
AuthenticatedPrincipal
- userId
- username
```

Los formularios no incluyen `userId`, `username` técnico ni actor de auditoría.

Aplicación rechaza la operación si el principal no representa al usuario estable `responsible-user`.

---

## 8. Dashboard y consultas generales

### 8.1. Dashboard

| Ruta | Método | Vista lógica |
|---|---|---|
| `/dashboard` | `GET` | `dashboard/index` |

Contexto mínimo:

- cierres en Preparación;
- cierres Bloqueados;
- cierres Validados pendientes de envío;
- cantidad de eventos no Validados;
- Alertas bloqueantes abiertas;
- acciones disponibles según estado.

El dashboard no modifica datos.

### 8.2. Lista de Cierres Operativos

| Ruta | Método | Vista lógica |
|---|---|---|
| `/closes` | `GET` | `closes/list` |

Filtros:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `state` | No | Estado del cierre |
| `periodStart` | No | Inicio mínimo |
| `periodEnd` | No | Fin máximo |
| `page` | No | Página |
| `size` | No | Tamaño |

### 8.3. Lista de Eventos Operativos

| Ruta | Método | Vista lógica |
|---|---|---|
| `/events` | `GET` | `events/list` |

Filtros:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `closeId` | No | Cierre propietario |
| `state` | No | Estado del evento |
| `eventType` | No | Tipo |
| `page` | No | Página |
| `size` | No | Tamaño |

### 8.4. Lista de Alertas

| Ruta | Método | Vista lógica |
|---|---|---|
| `/alerts` | `GET` | `alerts/list` |

Filtros:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `closeId` | No | Cierre afectado |
| `eventId` | No | Evento afectado |
| `state` | No | Estado de Alerta |
| `severity` | No | Severidad |
| `blocking` | No | Efecto bloqueante |
| `page` | No | Página |
| `size` | No | Tamaño |

---

## 9. Cierres Operativos

### 9.1. Crear un cierre

| Ruta | Método | Propósito |
|---|---|---|
| `/closes/new` | `GET` | Mostrar formulario |
| `/closes` | `POST` | Crear cierre |

Vista del formulario:

```text
closes/create
```

Campos:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `periodStart` | Sí | Fecha válida |
| `periodEnd` | Sí | Fecha válida y no anterior a `periodStart` |
| `currencyCode` | Sí | Tres letras mayúsculas |
| `initialBalance` | Sí | Decimal no negativo, máximo cuatro decimales |
| `_csrf` | Sí | Token válido |

No existe el campo `closeType`.

El MVP no introduce categorías diario, semanal o mensual. El período queda definido exclusivamente por `periodStart` y `periodEnd`.

Resultado exitoso:

```text
303 See Other
Location: /closes/{closeId}
```

Un período duplicado produce un conflicto de negocio sin crear un segundo cierre.

### 9.2. Detalle de cierre

| Ruta | Método | Vista lógica |
|---|---|---|
| `/closes/{closeId}` | `GET` | `closes/detail` |

Contexto mínimo:

- período;
- moneda;
- saldo inicial;
- estado actual;
- Eventos Operativos;
- Resultados vigentes relevantes;
- Alertas abiertas;
- consolidación vigente, cuando existe;
- último intento de envío;
- causas del último rechazo;
- historial de estados;
- acciones permitidas.

### 9.3. Acciones por estado

#### Preparación

Puede mostrar:

- registrar evento;
- validar eventos;
- gestionar evidencia y Autorización;
- consolidar cuando se cumplan precondiciones.

#### Bloqueado

Puede mostrar:

- revisar causas;
- corregir eventos;
- adjuntar o sustituir evidencia;
- registrar o sustituir Autorización;
- validar nuevamente;
- gestionar Alertas;
- consolidar nuevamente cuando se cumplan precondiciones.

#### Validado

Puede mostrar:

- detalle de consolidación;
- enviar a contabilidad.

Una modificación posterior de datos relevantes invalida resultados y consolidación, y devuelve el cierre a Bloqueado cuando corresponda.

#### Enviado a contabilidad

Solo permite consultas.

No se renderizan formularios de modificación ni comandos funcionales.

### 9.4. Ausencia de edición general del cierre

El MVP no expone una operación general para modificar:

- período;
- moneda;
- saldo inicial;

después de crear el cierre.

Una necesidad futura de corrección del encabezado del cierre requiere definir reglas de invalidación y actualizar este contrato.

---

## 10. Eventos Operativos

### 10.1. Registrar evento

| Ruta | Método | Propósito |
|---|---|---|
| `/closes/{closeId}/events/new` | `GET` | Mostrar formulario |
| `/closes/{closeId}/events` | `POST` | Registrar evento |

Vista:

```text
events/create
```

Campos:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `eventType` | Sí | `INCOME`, `EXPENSE`, `DISCOUNT` o `CANCELLATION` |
| `amount` | Sí | Decimal mayor que cero, máximo cuatro decimales |
| `reversedEventId` | Condicional | Obligatorio solo para `CANCELLATION` |
| `occurredAt` | Sí | Fecha y hora válida |
| `responsibleName` | Sí | Texto no vacío |
| `description` | Sí | Texto no vacío |
| `_csrf` | Sí | Token válido |

`balanceEffect` no forma parte del formulario.

Aplicación lo calcula:

```text
INCOME       → +amount
EXPENSE      → -amount
DISCOUNT     → -amount
CANCELLATION → inverso del evento referenciado
```

Para una Anulación:

- el evento referenciado existe;
- pertenece al mismo cierre;
- no es otra Anulación;
- no es el propio evento;
- no fue anulado previamente;
- el monto se deriva o verifica contra el evento original.

Estados permitidos del cierre:

```text
PREPARATION
BLOCKED
```

Resultado exitoso:

```text
303 See Other
Location: /events/{eventId}
```

### 10.2. Detalle de evento

| Ruta | Método | Vista lógica |
|---|---|---|
| `/events/{eventId}` | `GET` | `events/detail` |

Contexto mínimo:

- cierre propietario;
- tipo y monto;
- efecto sobre saldo;
- evento revertido, cuando aplica;
- fecha de ocurrencia;
- responsable de negocio;
- descripción;
- estado;
- revisión de datos;
- Evidencias de Soporte;
- Autorizaciones;
- Resultados de Validación;
- Alertas relacionadas;
- historial de estado;
- acciones permitidas.

### 10.3. Editar evento

| Ruta | Método | Propósito |
|---|---|---|
| `/events/{eventId}/edit` | `GET` | Mostrar formulario |
| `/events/{eventId}` | `POST` | Aplicar corrección |

Vista:

```text
events/edit
```

Campos:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `eventType` | Sí | Tipo permitido |
| `amount` | Sí | Decimal mayor que cero |
| `reversedEventId` | Condicional | Solo para `CANCELLATION` |
| `occurredAt` | Sí | Fecha y hora válida |
| `responsibleName` | Sí | No vacío |
| `description` | Sí | No vacío |
| `_csrf` | Sí | Token válido |

El formulario no recibe:

- `closeId` como autoridad de pertenencia;
- `balanceEffect`;
- `dataRevision`;
- estado;
- actor;
- fechas técnicas.

La pertenencia se obtiene del evento persistido.

Una corrección relevante:

1. bloquea el cierre;
2. incrementa la revisión del evento;
3. invalida Resultados dependientes;
4. invalida la consolidación vigente;
5. cambia un evento previamente Validado a Registrado;
6. cambia un cierre previamente Validado a Bloqueado;
7. deja el evento pendiente de una nueva validación.

Si el evento corregido ya está referenciado por una Anulación, la misma operación transaccional:

8. verifica que el evento original continúe siendo no cancelatorio;
9. recalcula el monto y `balanceEffect` de la Anulación dependiente;
10. incrementa la revisión de la Anulación;
11. invalida los Resultados de Validación de ambos eventos;
12. invalida la consolidación vigente;
13. deja ambos eventos pendientes de validación cuando corresponda.

Un evento referenciado por una Anulación no puede cambiar su tipo a `CANCELLATION`. Si la modificación no permite conservar una Anulación coherente, la operación se rechaza sin cambios parciales.

Resultado exitoso:

```text
303 See Other
Location: /events/{eventId}
```

### 10.4. Validar o revalidar evento

Existe un único comando HTTP:

| Ruta | Método | Propósito |
|---|---|---|
| `/events/{eventId}/validate` | `POST` | Ejecutar las reglas aplicables |

La misma ruta sirve para:

- validación inicial;
- validación posterior a una corrección;
- revalidación requerida por evidencia o Autorización nueva.

No existe una ruta separada `/revalidate`.

Efectos:

1. bloquea el cierre;
2. recarga el evento y sus datos aplicables;
3. determina VR-001, VR-002, VR-003 y VR-006 aplicables;
4. registra un Resultado por regla;
5. determina el estado verificable del evento;
6. genera o mantiene Alertas ante fallas;
7. resuelve automáticamente Alertas compatibles únicamente cuando existe una revalidación Satisfecha y vigente;
8. no vuelve a Validado el cierre sin una nueva consolidación.

Resultados posibles del evento:

```text
VALIDATED
PENDING_SUPPORT
PENDING_AUTHORIZATION
OBSERVED
```

Resultado HTTP:

```text
303 See Other
Location: /events/{eventId}
```

Tanto el éxito como una validación Fallida son resultados de negocio procesados y trazables.

---

## 11. Evidencias de Soporte

### 11.1. Registrar evidencia

| Ruta | Método | Propósito |
|---|---|---|
| `/events/{eventId}/supporting-evidence/new` | `GET` | Mostrar formulario |
| `/events/{eventId}/supporting-evidence` | `POST` | Registrar y vincular evidencia |

Codificación:

```text
multipart/form-data
```

Campos:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `file` | Condicional | Archivo a almacenar mediante el adaptador aprobado |
| `contentReference` | Condicional | Referencia verificable aceptada por la política aprobada |
| `evidenceType` | Sí | Código permitido |
| `supportedAmount` | No | Decimal mayor que cero cuando existe |
| `evidenceDate` | Sí | Fecha válida |
| `legibilityStatus` | Sí | `UNVERIFIED`, `LEGIBLE` o `ILLEGIBLE` |
| `_csrf` | Sí | Token válido dentro del cuerpo multipart |

Debe informarse exactamente uno entre:

```text
file
contentReference
```

La política definitiva puede restringir el formulario a uno de los dos mecanismos sin cambiar las invariantes del caso de uso.

El contenido físico se procesa fuera de la transacción final que bloquea el cierre. La operación de vinculación:

1. bloquea el cierre;
2. registra metadatos y referencia abstracta;
3. invalida Resultados dependientes;
4. incrementa la revisión del evento;
5. invalida la consolidación vigente;
6. cambia el evento Validado a Registrado;
7. cambia el cierre Validado a Bloqueado.

Resultado exitoso:

```text
303 See Other
Location: /events/{eventId}
```

### 11.2. Detalle de evidencia

| Ruta | Método | Vista lógica |
|---|---|---|
| `/supporting-evidence/{evidenceId}` | `GET` | `supporting-evidence/detail` |

### 11.3. Consultar contenido

| Ruta | Método | Propósito |
|---|---|---|
| `/supporting-evidence/{evidenceId}/content` | `GET` | Obtener o visualizar el contenido autorizado |

El endpoint:

- resuelve la referencia mediante el puerto de almacenamiento;
- no expone una ruta física del servidor;
- no entrega secretos del proveedor;
- establece un `Content-Type` validado;
- aplica la política de descarga o visualización que se defina en seguridad.

Un contenido ausente o no recuperable no convierte automáticamente la evidencia en válida.

### 11.4. Sustituir evidencia

| Ruta | Método | Propósito |
|---|---|---|
| `/supporting-evidence/{evidenceId}/replace` | `GET` | Mostrar formulario |
| `/supporting-evidence/{evidenceId}/replace` | `POST` | Crear sustitución y desactivar la anterior |

La sustitución utiliza los mismos campos del registro inicial.

La operación final:

- conserva la evidencia anterior como histórica;
- establece `isActive = false` y `deactivatedAt`;
- crea una nueva evidencia activa;
- invalida validaciones y consolidación;
- incrementa la revisión del evento;
- conserva trazabilidad.

### 11.5. Desactivar evidencia

| Ruta | Método | Propósito |
|---|---|---|
| `/supporting-evidence/{evidenceId}/deactivate` | `POST` | Retirar la evidencia de las validaciones actuales |

El formulario envía únicamente:

| Campo | Obligatorio |
|---|---:|
| `_csrf` | Sí |

El identificador se obtiene de la ruta. El actor y la fecha de desactivación se obtienen del principal autenticado y del reloj del servidor.

El MVP no exige una justificación textual para desactivar una Evidencia de Soporte.

No existe eliminación física mediante HTTP.

No existe:

```text
POST /supporting-evidence/{id}/delete
DELETE /supporting-evidence/{id}
```

---

## 12. Autorizaciones

### 12.1. Registrar Autorización

| Ruta | Método | Propósito |
|---|---|---|
| `/events/{eventId}/authorizations/new` | `GET` | Mostrar formulario |
| `/events/{eventId}/authorizations` | `POST` | Registrar Autorización |

Campos:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `authorizedByName` | Sí | Texto no vacío |
| `reason` | Sí | Texto no vacío |
| `authorizedAt` | Sí | Fecha y hora válida |
| `formalReference` | Sí | Referencia verificable no vacía |
| `_csrf` | Sí | Token válido |

La operación:

- bloquea el cierre;
- vincula la Autorización;
- invalida Resultados dependientes;
- incrementa la revisión del evento;
- invalida la consolidación vigente;
- cambia el evento Validado a Registrado;
- cambia el cierre Validado a Bloqueado;
- deja el evento pendiente de nueva validación.

Resultado exitoso:

```text
303 See Other
Location: /events/{eventId}
```

### 12.2. Detalle de Autorización

| Ruta | Método | Vista lógica |
|---|---|---|
| `/authorizations/{authorizationId}` | `GET` | `authorizations/detail` |

### 12.3. Sustituir Autorización

| Ruta | Método | Propósito |
|---|---|---|
| `/authorizations/{authorizationId}/replace` | `GET` | Mostrar formulario |
| `/authorizations/{authorizationId}/replace` | `POST` | Crear sustitución y desactivar la anterior |

La operación conserva ambos registros y deja uno solo activo para el flujo actual cuando esa sea la regla aplicable.

### 12.4. Desactivar Autorización

El formulario envía únicamente:

| Campo | Obligatorio |
|---|---:|
| `_csrf` | Sí |

El identificador se obtiene de la ruta. El actor y la fecha de desactivación se obtienen del principal autenticado y del reloj del servidor.

El MVP no exige una justificación textual para desactivar una Autorización.

No existe eliminación física mediante HTTP.

---

## 13. Alertas

### 13.1. Detalle de Alerta

| Ruta | Método | Vista lógica |
|---|---|---|
| `/alerts/{alertId}` | `GET` | `alerts/detail` |

Contexto:

- estado;
- severidad;
- efecto bloqueante;
- causa;
- entidad afectada;
- Resultado que la originó;
- Resultado que permitió resolverla, cuando existe;
- historial;
- acciones permitidas;
- vínculos hacia corrección, evidencia, Autorización o validación.

### 13.2. Reconocer

| Ruta | Método |
|---|---|
| `/alerts/{alertId}/acknowledge` | `POST` |

Precondición:

```text
ACTIVE
```

Resultado:

```text
ACKNOWLEDGED
```

Reconocer no elimina el efecto bloqueante ni valida la entidad.

### 13.3. Iniciar revisión

| Ruta | Método |
|---|---|
| `/alerts/{alertId}/start-review` | `POST` |

Precondiciones permitidas:

```text
ACTIVE
ACKNOWLEDGED
```

Resultado:

```text
UNDER_REVIEW
```

### 13.4. Descartar

| Ruta | Método |
|---|---|
| `/alerts/{alertId}/discard` | `POST` |

Campos:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `discardJustification` | Sí | Texto no vacío |
| `_csrf` | Sí | Token válido |

La autorización de la acción se verifica en Aplicación. No se introduce un rol técnico.

Descartar:

- registra actor, fecha y justificación;
- cambia la Alerta a `DISCARDED`;
- no cambia automáticamente el Evento a Validado;
- no elimina otras condiciones bloqueantes;
- no garantiza que el cierre pueda consolidarse.

### 13.5. Ausencia de resolución manual

No existe:

```text
POST /alerts/{alertId}/resolve
```

Una Alerta solo pasa a `RESOLVED` como efecto de una validación o revalidación exitosa que produzca un Resultado Satisfecho, vigente y compatible.

---

## 14. Consolidación

### 14.1. Formulario de consolidación

| Ruta | Método | Vista lógica |
|---|---|---|
| `/closes/{closeId}/consolidate` | `GET` | `consolidations/create` |

El formulario muestra:

- saldo inicial;
- eventos incluidos;
- totales nominales calculables;
- saldo esperado preliminar;
- Alertas bloqueantes;
- Resultados no vigentes o Fallidos;
- estado actual.

Campo ingresado por el usuario:

| Campo | Obligatorio | Regla |
|---|---:|---|
| `actualBalance` | Sí | Decimal no negativo, máximo cuatro decimales |
| `_csrf` | Sí | Token válido |

Los totales y `expectedBalance` no se aceptan como autoridad desde el navegador.

### 14.2. Ejecutar consolidación

| Ruta | Método |
|---|---|
| `/closes/{closeId}/consolidate` | `POST` |

La operación:

1. inicia la transacción;
2. bloquea el cierre;
3. rechaza un cierre Enviado;
4. recarga todos los Eventos;
5. recarga Resultados vigentes;
6. recarga Alertas bloqueantes;
7. verifica que exista al menos un Evento;
8. verifica que todos estén Validados;
9. verifica que las reglas aplicables estén Satisfechas y vigentes;
10. verifica que no haya Alertas bloqueantes abiertas;
11. invalida una consolidación vigente anterior, cuando exista;
12. calcula snapshots, totales, `expectedBalance` y `difference`;
13. crea una consolidación vigente;
14. cambia el cierre a Validado;
15. registra transición y trazabilidad;
16. confirma la transacción.

Fórmulas:

```text
expectedBalance = initialBalance + suma(balanceEffect)
difference = actualBalance - expectedBalance
```

### 14.3. Rechazo de consolidación

Cuando una precondición de negocio no se cumple:

- no se crea una consolidación vigente;
- el cierre queda o pasa a Bloqueado;
- se registran estado y trazabilidad cuando corresponda;
- se informa qué entidades requieren corrección;
- la respuesta sigue PRG.

Resultado:

```text
303 See Other
Location: /closes/{closeId}
```

con mensaje flash de error.

Un error técnico que impide persistir el resultado revierte la transacción y no simula un rechazo de negocio confirmado.

### 14.4. Detalle de consolidación

| Ruta | Método | Vista lógica |
|---|---|---|
| `/consolidations/{consolidationId}` | `GET` | `consolidations/detail` |

Contexto:

- cierre;
- moneda;
- cantidad de eventos;
- totales nominales;
- saldo inicial;
- saldo esperado;
- saldo real;
- diferencia;
- vigencia;
- fecha y responsable;
- snapshots utilizados;
- causa de invalidación, cuando exista.

---

## 15. VR-008 y envío interno a contabilidad

### 15.1. Comando

| Ruta | Método |
|---|---|
| `/closes/{closeId}/submit-to-accounting` | `POST` |

No existe un formulario que permita enviar:

- estado;
- resultado de VR-008;
- consolidación seleccionada;
- actor;
- fecha;
- causas.

La operación obtiene esos datos del estado bloqueado y recargado.

### 15.2. Protocolo

La operación:

1. inicia la transacción;
2. bloquea el cierre;
3. rechaza si ya está Enviado;
4. recarga Eventos;
5. recarga Resultados vigentes;
6. recarga Alertas bloqueantes;
7. recarga la consolidación vigente;
8. ejecuta VR-008;
9. registra el Resultado de VR-008;
10. registra el intento de envío;
11. registra causas estructuradas cuando falla;
12. persiste la transición del cierre;
13. confirma el resultado de negocio.

### 15.3. Resultado exitoso

Cuando VR-008 queda `SATISFIED`:

- existe una consolidación completa y vigente;
- todos los Eventos están Validados;
- no hay Alertas bloqueantes abiertas;
- todos los Resultados aplicables están vigentes y Satisfechos;
- se registra un intento `SUCCEEDED`;
- se registra fecha y usuario;
- el cierre pasa a `SENT_TO_ACCOUNTING`;
- no admite más mutaciones funcionales.

Respuesta:

```text
303 See Other
Location: /closes/{closeId}
```

Mensaje:

```text
Cierre enviado a contabilidad.
```

### 15.4. Resultado rechazado

Cuando VR-008 queda `FAILED`:

- se registra un intento `REJECTED`;
- se registran una o más causas estructuradas;
- no se registra envío exitoso;
- el cierre queda o pasa a `BLOCKED`;
- la consolidación evaluada se invalida cuando existe;
- se exige corrección, nueva validación y nueva consolidación.

Causas visibles iniciales:

```text
EVENT_NOT_VALIDATED
BLOCKING_ALERT
VALIDATION_RESULT_FAILED
VALIDATION_RESULT_STALE
CONSOLIDATION_MISSING
CONSOLIDATION_STALE
OTHER_CRITICAL_INCONSISTENCY
```

Respuesta:

```text
303 See Other
Location: /closes/{closeId}
```

Mensaje general:

```text
El envío fue rechazado. Revisa las causas registradas.
```

Las causas se muestran como datos estructurados en la vista; no se concatenan sin control dentro de la URL.

### 15.5. Diferencia frente a un conflicto HTTP

Una falla de VR-008 no devuelve `409` porque el sistema procesó el comando y confirmó un resultado de negocio con trazabilidad.

Se utiliza `409 Conflict` cuando la operación no pudo ejecutarse o confirmarse por:

- timeout de bloqueo;
- deadlock traducido;
- estado modificado concurrentemente fuera del resultado previsto;
- transición incompatible detectada antes de una mutación que deba persistirse.

---

## 16. Validación de formularios

### 16.1. Capas de validación

Presentación valida:

- presencia;
- formato;
- longitud;
- rango básico;
- UUID;
- fecha;
- decimal;
- código de catálogo conocido.

Aplicación y Dominio validan:

- estado permitido;
- pertenencia al cierre;
- vigencia;
- aplicabilidad de reglas;
- referencias entre eventos;
- invariantes;
- transiciones;
- autorización de comandos;
- consistencia transaccional.

### 16.2. Errores de campo

Los errores se asocian al nombre público del campo.

Ejemplos:

| Campo | Código lógico |
|---|---|
| `amount` | `amount.required` |
| `amount` | `amount.positive` |
| `periodEnd` | `periodEnd.beforePeriodStart` |
| `currencyCode` | `currencyCode.invalid` |
| `reversedEventId` | `reversedEventId.requiredForCancellation` |
| `actualBalance` | `actualBalance.nonNegative` |
| `discardJustification` | `discardJustification.required` |

El texto final puede localizarse en español sin modificar el código lógico.

### 16.3. Errores globales

Se utilizan cuando el problema no corresponde a un único campo.

Ejemplos:

- el período ya existe;
- el cierre está Enviado;
- el evento original ya fue anulado;
- el evento no pertenece al cierre esperado;
- la evidencia no pudo vincularse;
- la consolidación no cumple precondiciones;
- la transición de Alerta no está permitida.

### 16.4. Valores controlados por servidor

No se confía en campos ocultos para:

- actor;
- estado;
- cierre propietario de un evento existente;
- revisión;
- vigencia;
- `balanceEffect`;
- totales;
- saldo esperado;
- diferencia;
- resultado de validación;
- severidad calculada;
- efecto bloqueante;
- consolidación vigente;
- resultado del envío.

---

## 17. Manejo de errores

### 17.1. Entrada inválida

```text
400 Bad Request
```

Se renderiza el formulario con errores.

No se ejecuta el caso de uso.

### 17.2. No autenticado

Para navegación web:

- se redirige a `/login`;
- no se expone un cuerpo JSON;
- se conserva únicamente una URL de destino segura cuando la política lo permita.

### 17.3. CSRF

Token ausente o inválido:

```text
403 Forbidden
```

No se ejecuta el caso de uso.

### 17.4. Recurso inexistente

```text
404 Not Found
```

La vista no revela si un identificador pertenece a otro usuario porque el MVP solo contiene un usuario.

### 17.5. Conflicto de negocio no persistido

```text
409 Conflict
```

Ejemplos:

- comando incompatible con el estado actual;
- segundo evento intentando anular el mismo evento;
- intento de modificar un cierre Enviado;
- referencia válida en formato pero incompatible con el agregado.

La vista muestra el estado actual y una acción de recuperación.

### 17.6. Conflicto de concurrencia

```text
409 Conflict
```

El mensaje indica:

- que la operación no se confirmó;
- que no hubo reintento automático;
- que el usuario debe recargar el estado y decidir si reintenta.

### 17.7. Error técnico

```text
500 Internal Server Error
```

La respuesta:

- muestra un mensaje genérico;
- no presenta stack trace;
- no afirma que la operación se completó;
- no convierte el error en una Alerta de negocio;
- no simula un estado Bloqueado salvo que ese estado haya sido confirmado antes del fallo.

---

## 18. CSRF

### 18.1. Regla general

Todo `POST` requiere un token CSRF válido.

Incluye:

- login;
- logout;
- formularios normales;
- comandos de estado;
- multipart;
- descarte de Alertas;
- validación;
- consolidación;
- envío.

### 18.2. Transporte

El token se envía:

- en el cuerpo del formulario;
- o en el encabezado aprobado para una solicitud JavaScript futura.

Nunca se envía:

- en la URL;
- en parámetros de consulta;
- dentro de mensajes flash.

### 18.3. Thymeleaf

Los formularios renderizados por Thymeleaf integran el token conforme a la configuración de Spring Security.

El contrato no depende de que el usuario manipule el valor directamente.

---

## 19. Carga de archivos

### 19.1. Contrato multipart

Los endpoints de evidencia utilizan `multipart/form-data`.

El nombre técnico del campo es:

```text
file
```

### 19.2. Decisiones diferidas

Permanecen pendientes para el diseño de seguridad:

- tamaño máximo;
- extensiones aceptadas;
- tipos MIME aceptados;
- detección del tipo real;
- checksum;
- antivirus;
- cifrado;
- nombre seguro;
- política de descarga;
- retención física.

Este documento no fija JPEG, PNG o PDF como catálogo definitivo.

### 19.3. Consistencia

La transferencia física no mantiene abierto el bloqueo pesimista del cierre.

La vinculación final de la referencia abstracta sí se ejecuta dentro de la transacción que protege las invariantes del evento y del cierre.

Si el almacenamiento físico termina pero la vinculación falla, el adaptador debe aplicar el protocolo de compensación definido posteriormente sin declarar la Evidencia como activa.

---

## 20. Concurrencia

### 20.1. Operaciones protegidas

Toda operación mutable vinculada a un cierre bloquea primero `operational_close`.

Incluye:

- registrar o editar evento;
- registrar, sustituir o desactivar evidencia;
- registrar, sustituir o desactivar Autorización;
- validar evento;
- gestionar Alerta bloqueante;
- consolidar;
- enviar;
- registrar transiciones.

### 20.2. Parámetros HTTP

El navegador no controla:

- nivel de aislamiento;
- lock timeout;
- reintentos;
- orden de bloqueo.

Esos valores pertenecen a Infraestructura y configuración.

### 20.3. Comportamiento observable

Cuando el bloqueo se adquiere:

- la operación recarga el estado dentro de la transacción;
- no decide con datos enviados como snapshot por el navegador.

Cuando el bloqueo no puede adquirirse:

- la operación revierte;
- responde `409 Conflict`;
- no reintenta;
- no deja registros parciales;
- el usuario recarga la vista antes de repetir.

### 20.4. Doble envío

El contrato no utiliza idempotency keys en el MVP.

La seguridad contra doble envío se apoya en:

- bloqueo del cierre;
- estado terminal;
- restricción de un único envío exitoso;
- PRG para reducir reenvíos del navegador.

---

## 21. Trazabilidad

### 21.1. Trazabilidad de negocio

Las operaciones relevantes conservan:

- actor obtenido de `AuthenticatedPrincipal`;
- fecha y hora obtenidas del reloj;
- entidad afectada;
- transición;
- causa;
- Resultado de Validación;
- justificación cuando corresponde;
- intento de envío;
- resultado exitoso o rechazado.

### 21.2. Eventos de seguridad

Los eventos de autenticación y sesión se registran según ADR-0005.

No contienen:

- contraseña;
- hash completo;
- cookie;
- token CSRF;
- identificador completo de sesión.

### 21.3. Solicitudes de lectura

El contrato no exige persistir una fila de auditoría por cada `GET`.

El logging técnico de ruta, método, latencia y estado HTTP pertenece a observabilidad y despliegue, no al modelo funcional.

### 21.4. Datos del actor

Los formularios no aceptan actor como fuente confiable.

El actor se obtiene después de autenticar la solicitud.

---

## 22. Matriz resumida de rutas

| Recurso o acción | Método | Ruta |
|---|---|---|
| Login | `GET` | `/login` |
| Login | `POST` | `/login` |
| Logout | `POST` | `/logout` |
| Dashboard | `GET` | `/dashboard` |
| Listar cierres | `GET` | `/closes` |
| Crear cierre | `GET` | `/closes/new` |
| Crear cierre | `POST` | `/closes` |
| Ver cierre | `GET` | `/closes/{closeId}` |
| Crear evento | `GET` | `/closes/{closeId}/events/new` |
| Crear evento | `POST` | `/closes/{closeId}/events` |
| Listar eventos | `GET` | `/events` |
| Ver evento | `GET` | `/events/{eventId}` |
| Editar evento | `GET` | `/events/{eventId}/edit` |
| Editar evento | `POST` | `/events/{eventId}` |
| Validar evento | `POST` | `/events/{eventId}/validate` |
| Registrar evidencia | `GET` | `/events/{eventId}/supporting-evidence/new` |
| Registrar evidencia | `POST` | `/events/{eventId}/supporting-evidence` |
| Ver evidencia | `GET` | `/supporting-evidence/{evidenceId}` |
| Ver contenido | `GET` | `/supporting-evidence/{evidenceId}/content` |
| Sustituir evidencia | `GET` | `/supporting-evidence/{evidenceId}/replace` |
| Sustituir evidencia | `POST` | `/supporting-evidence/{evidenceId}/replace` |
| Desactivar evidencia | `POST` | `/supporting-evidence/{evidenceId}/deactivate` |
| Registrar Autorización | `GET` | `/events/{eventId}/authorizations/new` |
| Registrar Autorización | `POST` | `/events/{eventId}/authorizations` |
| Ver Autorización | `GET` | `/authorizations/{authorizationId}` |
| Sustituir Autorización | `GET` | `/authorizations/{authorizationId}/replace` |
| Sustituir Autorización | `POST` | `/authorizations/{authorizationId}/replace` |
| Desactivar Autorización | `POST` | `/authorizations/{authorizationId}/deactivate` |
| Listar Alertas | `GET` | `/alerts` |
| Ver Alerta | `GET` | `/alerts/{alertId}` |
| Reconocer Alerta | `POST` | `/alerts/{alertId}/acknowledge` |
| Iniciar revisión | `POST` | `/alerts/{alertId}/start-review` |
| Descartar Alerta | `POST` | `/alerts/{alertId}/discard` |
| Mostrar consolidación | `GET` | `/closes/{closeId}/consolidate` |
| Consolidar | `POST` | `/closes/{closeId}/consolidate` |
| Ver consolidación | `GET` | `/consolidations/{consolidationId}` |
| Enviar a contabilidad | `POST` | `/closes/{closeId}/submit-to-accounting` |

---

## 23. Rutas excluidas

El MVP no expone:

```text
DELETE /...
PUT /...
PATCH /...
POST /alerts/{alertId}/resolve
POST /closes/{closeId}/reopen
POST /closes/{closeId}/provisional
POST /closes/{closeId}/approve-by-manager
POST /accounting/receive
/api/**
```

Tampoco expone endpoints para:

- administrar reglas;
- administrar usuarios;
- administrar roles;
- cambiar permisos;
- editar un cierre Enviado;
- eliminar registros históricos;
- forzar estados;
- marcar manualmente una validación como Satisfecha;
- registrar movimientos físicos nunca observados por el usuario.

---

## 24. Criterios de aceptación

El contrato puede aprobarse como línea base cuando:

1. utiliza HTML renderizado en servidor como interfaz principal;
2. no introduce endpoints JSON sin consumidor concreto;
3. utiliza `GET` únicamente para lectura;
4. utiliza `POST` para toda mutación;
5. aplica `303 See Other` después de comandos procesados;
6. distingue entrada inválida, conflicto, rechazo persistido y fallo técnico;
7. utiliza rutas y campos técnicos coherentes en inglés;
8. utiliza UUID para entidades de negocio;
9. crea cierres mediante período, moneda y saldo inicial;
10. no introduce un tipo diario, semanal o mensual;
11. registra los cuatro tipos de Evento del MVP;
12. no recibe `balanceEffect` desde el navegador;
13. exige `reversedEventId` para Anulaciones;
14. utiliza un único comando para validar y revalidar;
15. no permite resolver manualmente una Alerta;
16. permite reconocer, revisar y descartar con las restricciones aprobadas;
17. no elimina Evidencias ni Autorizaciones;
18. representa sustitución y desactivación con trazabilidad;
19. solicita `actualBalance` para consolidar;
20. calcula totales, saldo esperado y diferencia en el servidor;
21. trata una falla de consolidación como resultado de negocio trazable;
22. ejecuta VR-008 inmediatamente antes del envío;
23. confirma tanto el éxito como el rechazo de VR-008;
24. no devuelve `409` por una falla de VR-008 confirmada;
25. utiliza `409` para conflictos que impiden confirmar la operación;
26. invalida Resultados y consolidaciones después de cambios relevantes;
27. bloquea primero el Cierre Operativo en toda escritura relevante;
28. no reintenta automáticamente;
29. exige CSRF en todo `POST`, incluido multipart, login y logout;
30. no coloca tokens CSRF en URL;
31. no confía en actor, estado, vigencia o totales enviados por el navegador;
32. mantiene la sesión y el principal conforme a ADR-0005;
33. no persiste auditoría funcional por cada consulta `GET`;
34. no fija todavía formatos ni tamaños definitivos de Evidencias;
35. no amplía el alcance funcional del MVP;
36. se incorpora al repositorio mediante revisión del diff.

---

## 25. Documentos relacionados

- Use Cases v0.2 — Casos de Uso.
- Validation Rules v0.2 — Reglas de Validación.
- State Machine v0.3 — Máquina de Estados.
- MVP Scope v0.3 — Alcance del MVP.
- Architecture Drivers v0.1 — Impulsores de Arquitectura.
- Architecture Overview v0.1 — Visión General de Arquitectura.
- Data Model v0.1 — Modelo de Datos.
- ADR-0001 — La validación final puede devolver un cierre validado a bloqueado.
- ADR-0002 — Estilo arquitectónico de la aplicación.
- ADR-0003 — Stack tecnológico del MVP.
- ADR-0004 — Estrategia de persistencia y control de concurrencia.
- ADR-0005 — Estrategia de autenticación y sesión.

---

## 26. Conclusión

Operational Close Validator expone un contrato HTTP orientado a formularios HTML y vistas renderizadas en servidor.

El contrato:

- mantiene Presentación separada de Dominio e Infraestructura;
- utiliza el principal autenticado como fuente del actor;
- evita confiar en estado o datos derivados enviados por el navegador;
- representa las correcciones mediante invalidación, nueva validación y nueva consolidación;
- conserva las Alertas como resultados gestionables pero no resolubles manualmente;
- procesa VR-008 y el envío dentro del mismo límite transaccional;
- confirma los rechazos de negocio que deben conservar trazabilidad;
- reserva los errores HTTP de conflicto para operaciones que no pudieron confirmarse;
- protege las mutaciones con CSRF y bloqueo del cierre;
- no introduce JSON, roles, eliminación funcional, reapertura ni integración externa.

Este diseño proporciona el contrato necesario para diseñar seguridad, pruebas e implementación sin ampliar el alcance aprobado del MVP.