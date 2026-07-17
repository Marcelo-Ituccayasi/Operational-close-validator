# Estrategia de Despliegue

**Versión:** v0.1

**Estado:** Candidata a línea base

**Estado documental:** Incorporada en el repositorio; revisión final y aprobación pendientes

**Fase:** 05 — Diseño técnico

**Producto:** Operational Close Validator

---

## 1. Propósito y alcance

Este documento define cómo construir, configurar, publicar, verificar y recuperar el MVP de Operational Close Validator.

Incluye:

- artefacto desplegable;
- entornos;
- topología pública;
- configuración y secretos;
- PostgreSQL y Flyway;
- persistencia de Evidencias;
- HTTPS, proxy y health checks;
- CI/CD;
- despliegue, smoke tests, rollback y backups;
- observabilidad y riesgos operativos.

No selecciona un proveedor comercial.

Quedan fuera:

- alta disponibilidad;
- múltiples instancias activas;
- Kubernetes;
- multirregión;
- autoscaling horizontal;
- CDN;
- almacenamiento distribuido de sesiones;
- object storage para Evidencias;
- recuperación de desastres con objetivos contractuales.

Estas capacidades requieren revisar sesión, limitación del login, almacenamiento y concurrencia.

---

## 2. Restricciones heredadas

| Fuente | Restricción |
|---|---|
| ADR-0002 | Monolito modular y una sola unidad desplegable |
| ADR-0003 | Java LTS, Spring Boot, Thymeleaf, Maven y build reproducible |
| ADR-0004 | PostgreSQL, Flyway, `READ_COMMITTED` y bloqueo pesimista |
| ADR-0005 | Form login, sesión en memoria y credenciales externas |
| Data Model v0.1 | Esquema `ocv`, trazabilidad y migraciones controladas |
| API Design v0.1 | Interfaz y backend de mismo origen |
| Security Design v0.1 | HTTPS, cookie segura, proxy confiable, fail-fast y Evidencias persistentes |
| Testing Strategy v0.1 | `mvn verify`, CI obligatoria y artefactos trazables |
| MVP Scope v0.3 | Un usuario, una instancia y envío interno |

El despliegue no introduce una segunda fuente de verdad ni modifica reglas del Dominio.

---

## 3. Decisiones principales

1. La aplicación se publica como una imagen OCI inmutable.
2. Existe una sola instancia activa.
3. PostgreSQL es persistente y privado.
4. Las Evidencias se almacenan en un volumen persistente externo a la imagen.
5. TLS termina en un proxy confiable.
6. La aplicación recibe tráfico solo después de migraciones y Readiness satisfactoria.
7. Production utiliza el mismo digest probado en Staging.
8. La promoción a Production requiere aprobación manual.
9. Flyway no se revierte automáticamente.
10. Se acepta una interrupción breve durante el reemplazo de la instancia.

El MVP prioriza integridad y recuperación sobre despliegue sin interrupciones.

---

## 4. Entornos

| Entorno | Propósito | Exposición |
|---|---|---|
| Local | Desarrollo manual | Solo `localhost` |
| Test | Automatización | No pública |
| Staging | Validación previa | HTTPS restringido |
| Production | Operación pública | HTTPS público |

### 4.1. Local y Test

Local puede usar HTTP sobre `localhost`, PostgreSQL local o contenedorizado y un directorio de Evidencias no real.

Test utiliza:

- PostgreSQL Testcontainers;
- directorios temporales;
- reloj y datos controlados;
- secretos sintéticos;
- ausencia de dependencias externas.

### 4.2. Staging

Staging reproduce la topología pública con:

- la misma imagen de Production;
- PostgreSQL y volumen independientes;
- HTTPS;
- cookie `Secure`;
- proxy confiable;
- secretos diferentes.

Debe utilizarse antes de la primera publicación y ante cambios en migraciones, seguridad, Evidencias, backup o rollback.

### 4.3. Production

Production:

- no compila código;
- no usa secretos de desarrollo;
- recibe únicamente artefactos producidos por CI;
- conserva registro del digest desplegado.

---

## 5. Artefacto y runtime

### 5.1. Imagen

La imagen OCI contiene:

- ejecutable Spring Boot;
- plantillas Thymeleaf y recursos estáticos;
- runtime Java compatible;
- versión y commit.

No contiene:

- repositorio Git;
- secretos;
- `.env`;
- PostgreSQL;
- Evidencias;
- herramientas de desarrollo.

La construcción usa Maven Wrapper, build multietapa e imágenes versionadas. No se utiliza `latest`.

Antes de construir:

```powershell
./mvnw verify  # Ejecuta pruebas, integración, arquitectura y cobertura.
```

Cada imagen se identifica mediante:

```text
sha-<commit>
v<version>
digest inmutable
```

### 5.2. Runtime

El contenedor:

- ejecuta como usuario no root;
- usa filesystem raíz de solo lectura cuando sea compatible;
- escribe únicamente en temporales y en el volumen autorizado;
- usa UTC como zona técnica;
- recibe límites explícitos de CPU y memoria;
- termina de forma controlada;
- no conserva estado de negocio.

Los límites se miden en Staging y se registran por entorno.

---

## 6. Topología pública

```text
Internet
   |
HTTPS
   |
Proxy confiable
   |
HTTP privado
   |
Una instancia OCV
   |              |
PostgreSQL        Volumen de Evidencias
```

Reglas:

- la aplicación no se expone directamente;
- PostgreSQL y el volumen no son públicos;
- interfaz y backend comparten origen;
- CORS no se habilita globalmente;
- el volumen no se sirve como directorio estático;
- existe un único escritor del volumen.

---

## 7. Configuración y secretos

Perfiles:

```text
local
test
public
```

Staging y Production utilizan `public`.

| Variable | Sensible | Regla |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | No | Perfil activo |
| `OCV_ENVIRONMENT` | No | `staging` o `production` en público |
| `SPRING_DATASOURCE_URL` | Sí | PostgreSQL del entorno |
| `SPRING_DATASOURCE_USERNAME` | Sí | Usuario técnico |
| `SPRING_DATASOURCE_PASSWORD` | Sí | Secreto externo |
| `OCV_AUTH_USERNAME` | No | Sin valor por defecto |
| `OCV_AUTH_PASSWORD_HASH` | Sí | `{bcrypt}<encoded-password>` |
| `OCV_SESSION_EXPIRY_MINUTES` | No | Predeterminado `30` |
| `OCV_LOGIN_MAX_FAILURES` | No | Predeterminado `10` |
| `OCV_LOGIN_WINDOW_SECONDS` | No | Predeterminado `300` |
| `OCV_LOGIN_BLOCK_SECONDS` | No | Predeterminado `300` |
| `OCV_TRUSTED_PROXY_CIDRS` | No | Obligatoria cuando existe proxy |
| `OCV_EVIDENCE_STORAGE_PATH` | No | Ruta absoluta del volumen |
| `OCV_EVIDENCE_MAX_FILE_SIZE_BYTES` | No | Máximo 10 MiB |
| `OCV_EVIDENCE_MAX_REQUEST_SIZE_BYTES` | No | Máximo 12 MiB |
| `OCV_BUSINESS_TIME_ZONE` | No | Zona de fechas de negocio |

Los secretos:

- se inyectan desde el entorno;
- no se escriben en Git, imagen o scripts;
- no se muestran en logs;
- no se comparten entre Staging y Production.

Una configuración obligatoria ausente, inválida o insegura detiene el arranque. No existe fallback silencioso.

---

## 8. PostgreSQL y Flyway

Production utiliza PostgreSQL persistente, respaldado y accesible únicamente desde la red privada.

Un servicio administrado es preferido. Una alternativa autoadministrada debe ofrecer persistencia, cifrado, backup, restauración y monitoreo equivalentes.

El usuario técnico:

- no es superusuario;
- se limita a la base del producto;
- opera únicamente sobre el esquema `ocv`.

Las conexiones remotas públicas requieren cifrado en tránsito.

### 8.1. Arranque

1. validar conexión;
2. validar historial Flyway;
3. aplicar migraciones;
4. validar mapeo Hibernate;
5. habilitar Readiness.

Hibernate no crea ni modifica tablas.

Una migración fallida:

- impide Readiness;
- bloquea tráfico;
- marca el despliegue como fallido;
- no se ignora;
- no ejecuta downgrade automático.

### 8.2. Compatibilidad

Las migraciones deben ser aditivas y compatibles con la versión anterior cuando sea razonable.

Una migración destructiva requiere:

- backup previo;
- mantenimiento;
- prueba en Staging;
- procedimiento de restauración;
- aprobación manual.

---

## 9. Evidencias de Soporte

Production monta un volumen persistente en:

```text
OCV_EVIDENCE_STORAGE_PATH
```

El volumen:

- vive fuera de la imagen;
- sobrevive al reemplazo del contenedor;
- usa cifrado en reposo;
- tiene acceso restringido;
- dispone de backup;
- no se publica directamente.

Antes de Readiness se verifica:

- ruta absoluta;
- existencia;
- lectura y escritura;
- ubicación fuera del web root;
- creación y eliminación de un archivo temporal.

La transferencia física, namespaces `stored:` y `reference:`, SHA-256, compensación y entrega permanecen definidos en `security-design.md`.

Cambiar a object storage requiere una nueva decisión.

---

## 10. HTTPS, proxy y encabezados

Staging y Production usan HTTPS.

El proxy:

- mantiene certificados válidos;
- redirige o rechaza HTTP;
- reenvía al puerto privado;
- conserva el esquema original;
- no expone PostgreSQL ni el volumen.

La aplicación solo interpreta encabezados reenviados cuando la conexión directa pertenece a:

```text
OCV_TRUSTED_PROXY_CIDRS
```

Una configuración inválida impide el arranque.

La aplicación conserva responsabilidad sobre:

- HSTS;
- CSP;
- `X-Content-Type-Options`;
- `X-Frame-Options`;
- `Referrer-Policy`;
- `Permissions-Policy`;
- `Cache-Control`.

El proxy no elimina ni debilita estos encabezados.

---

## 11. Health checks y terminación

### 11.1. Startup

Startup permanece fallido mientras no finalicen:

- Flyway;
- aprovisionamiento de identidad;
- validación de configuración;
- validación del volumen.

### 11.2. Liveness

Liveness indica que el proceso funciona.

No depende de PostgreSQL para evitar reinicios continuos ante una interrupción temporal.

Puede exponerse sin información sensible.

### 11.3. Readiness

Readiness requiere:

- aplicación inicializada;
- migraciones aplicadas;
- PostgreSQL accesible;
- volumen utilizable;
- configuración pública válida.

Solo la plataforma o red interna accede a Readiness.

### 11.4. Terminación

1. retirar tráfico nuevo;
2. finalizar solicitudes en curso dentro del límite;
3. cerrar conexiones;
4. terminar proceso.

Un reinicio invalida las sesiones en memoria.

---

## 12. CI/CD y promoción

### 12.1. Pull request

Cada pull request ejecuta:

1. compilación;
2. `./mvnw verify`;
3. PostgreSQL Testcontainers;
4. pruebas de arquitectura y cobertura;
5. construcción de imagen sin promoverla.

Un gate fallido bloquea merge.

### 12.2. `main`

Después del merge:

1. repetir `./mvnw verify`;
2. construir imagen;
3. publicar referencia inmutable;
4. registrar digest;
5. desplegar Staging;
6. ejecutar smoke tests.

### 12.3. Production

La promoción:

- requiere aprobación manual;
- reutiliza exactamente el digest probado;
- no reconstruye la imagen;
- no se ejecuta desde una estación local.

---

## 13. Procedimiento de despliegue

### 13.1. Preparación

1. confirmar CI;
2. identificar commit, versión y digest;
3. revisar migraciones;
4. validar secretos;
5. confirmar backup;
6. crear backup previo cuando corresponda;
7. declarar mantenimiento cuando sea necesario.

### 13.2. Ejecución

1. retirar tráfico o activar mantenimiento;
2. detener la versión anterior;
3. iniciar la imagen nueva;
4. ejecutar Flyway y validaciones;
5. esperar Readiness;
6. ejecutar smoke tests;
7. habilitar tráfico;
8. registrar resultado.

Se acepta una interrupción breve.

### 13.3. Smoke tests

Staging verifica:

- health checks;
- HTTPS y encabezados;
- login, sesión y logout;
- cookie segura;
- PostgreSQL;
- escritura y lectura controlada de Evidencia;
- flujo operativo representativo;
- logs sin secretos.

Production verifica únicamente:

- health checks;
- HTTPS y encabezados;
- redirección anónima;
- login;
- vistas no mutantes;
- versión y logs.

Production no crea datos ficticios.

---

## 14. Rollback

La imagen anterior se conserva por digest.

### 14.1. Aplicación

Puede desplegarse la versión anterior cuando no existen cambios de esquema incompatibles.

1. retirar tráfico;
2. desplegar digest anterior;
3. esperar Readiness;
4. ejecutar smoke tests;
5. habilitar tráfico;
6. registrar causa.

### 14.2. Datos

Con una migración incompatible:

- no se revierte Flyway automáticamente;
- la aplicación permanece fuera de servicio;
- se restaura PostgreSQL;
- se restaura el volumen cuando fue afectado;
- se despliega la imagen anterior;
- se verifica integridad antes de publicar.

Una restauración destructiva requiere aprobación manual.

---

## 15. Backups y restauración

| Recurso | Frecuencia | Retención mínima |
|---|---|---:|
| PostgreSQL | Diaria | 7 días |
| Evidencias | Diaria | 7 días |
| Backup previo a migración riesgosa | Por despliegue | Hasta confirmar estabilidad |

Los backups usan cifrado y acceso restringido.

Al restaurar:

- el snapshot de Evidencias debe ser del mismo momento o posterior al backup de PostgreSQL;
- archivos sin referencia quedan huérfanos y no se sirven;
- referencias sin archivo se tratan como incidente;
- se verifican referencias `stored:` y SHA-256.

Antes de la primera publicación se ejecuta una restauración aislada.

La prueba se repite cuando cambia el proveedor, volumen, procedimiento o formato de referencia.

---

## 16. Logs y monitoreo

La aplicación escribe en `stdout` y `stderr`; no administra archivos de log internos.

Campos técnicos permitidos:

```text
timestamp UTC
level
logger
environment
correlationId
event type
sanitized result
```

Se mantienen las prohibiciones de datos sensibles establecidas en `security-design.md`.

La plataforma monitorea:

- disponibilidad;
- PostgreSQL;
- CPU y memoria;
- capacidad del volumen;
- respuestas `5xx`;
- fallos de arranque;
- fallos de backup.

No se requiere APM externo en el MVP.

---

## 17. Gestión de fallos

| Falla | Resultado |
|---|---|
| PostgreSQL no disponible | Readiness falla |
| Volumen no utilizable | Arranque o Readiness falla |
| Migración inválida | Despliegue bloqueado |
| Configuración insegura | Fail-fast |
| Certificado inválido | Servicio no publicado |
| Smoke test fallido | No habilitar tráfico o rollback |
| Backup fallido | Bloquear migración destructiva |
| Volumen sin capacidad suficiente | La carga falla de forma segura, se intenta la compensación y no se crea una Evidencia activa |

Los errores técnicos no generan Alertas de negocio.

---

## 18. Límites y evolución

Dentro del MVP:

- existe una instancia;
- sesiones y limitador están en memoria;
- el volumen tiene un escritor;
- Flyway se ejecuta desde una instancia;
- solo se permite escalamiento vertical.

Múltiples instancias requieren resolver:

- sesiones compartidas;
- limitador compartido;
- almacenamiento compartido;
- coordinación de migraciones;
- concurrencia entre nodos;
- balanceo y observabilidad.

Ese cambio requiere una ADR nueva o sustitución explícita de las decisiones actuales.

---

## 19. Riesgos aceptados

| Riesgo | Tratamiento |
|---|---|
| Interrupción breve | Aceptada para una instancia |
| Pérdida de sesión al reiniciar | Nuevo login |
| Punto único de fallo | Backups, health checks y rollback |
| Limitador reiniciado | Riesgo aceptado |
| Volumen persistente local | Una instancia y backup |
| Backup DB/archivos no atómico | Inmutabilidad y verificación |
| Rollback de migración destructiva | Backup y mantenimiento |
| Sin multirregión | Fuera del MVP |
| Promoción manual | Reduce errores |
| Proveedor no fijado | Capacidades mínimas obligatorias |

Estos riesgos no permiten omitir HTTPS, Flyway, persistencia, backups, smoke tests ni fail-fast.

---

## 20. Criterios de aceptación

La estrategia puede aprobarse cuando:

1. conserva una unidad desplegable y una instancia;
2. define una imagen OCI inmutable;
3. excluye secretos de Git y de la imagen;
4. separa Local, Test, Staging y Production;
5. reutiliza el mismo digest entre Staging y Production;
6. exige aprobación manual para Production;
7. mantiene PostgreSQL privado y respaldado;
8. usa Flyway como único mecanismo de migración;
9. impide tráfico antes de Readiness;
10. mantiene Evidencias en volumen persistente;
11. exige HTTPS y proxy confiable;
12. distingue Startup, Liveness y Readiness;
13. define CI, despliegue y smoke tests;
14. conserva una versión anterior para rollback;
15. no revierte Flyway automáticamente;
16. define backups diarios y restauración probada;
17. documenta pérdida de sesiones;
18. no introduce alta disponibilidad ni múltiples instancias;
19. permanece coherente con las líneas base aprobadas.

---

## 21. Documentos relacionados

- MVP Scope v0.3 — Alcance del MVP.
- ADR-0002 — Estilo arquitectónico.
- ADR-0003 — Stack tecnológico.
- ADR-0004 — Persistencia y concurrencia.
- ADR-0005 — Autenticación y sesión.
- Architecture Overview v0.1 — Visión General de Arquitectura.
- Data Model v0.1 — Modelo de Datos.
- API Design v0.1 — Diseño de API.
- Security Design v0.1 — Diseño de Seguridad.
- Testing Strategy v0.1 — Estrategia de Pruebas.

---

## 22. Conclusión

Operational Close Validator se despliega como una imagen inmutable, una instancia de aplicación, PostgreSQL persistente y un volumen persistente de Evidencias.

La estrategia prioriza integridad y recuperación mediante migraciones verificadas, HTTPS, configuración externa, health checks, backups, smoke tests y rollback controlado. La alta disponibilidad y el despliegue horizontal quedan fuera del MVP.