# Changelog

Todos los cambios relevantes de Operational Close Validator se documentan en este archivo.

El formato sigue los principios de Keep a Changelog y el proyecto utiliza versionado semántico.

## [Unreleased]

Sin cambios pendientes.

## [1.0.0-mvp] - 2026-07-31

### Added

- Dockerfile multietapa para construir una imagen OCI reproducible.
- Ejecución del contenedor mediante un usuario dedicado sin privilegios.
- Docker Compose local para construir y ejecutar la aplicación desde el código fuente.
- Stack público endurecido con PostgreSQL, almacenamiento persistente y red privada.
- Publicación de imágenes OCI inmutables en GitHub Container Registry.
- Validación de la construcción de la imagen mediante GitHub Actions.
- Configuración externa para sesión, rate limiting, evidencias y zona horaria de negocio.
- Perfiles Spring separados para ejecución local y despliegue público.
- Metadatos OCI para identificar versión y commit del artefacto.
- Sistema visual compartido y responsivo mediante `app.css`.
- Rediseño integral de las vistas Thymeleaf.
- Capturas representativas del producto en `docs/screenshots`.
- Presentación actualizada del MVP en el README.
- Prueba de integración para la visualización de evidencias y autorizaciones.

### Changed

- Mejorada la presentación del login, dashboard, cierres, eventos, consolidación y errores.
- Mejorada la visualización del modo de solo lectura después del envío a contabilidad.
- Mejorada la distribución responsiva de detalles, tablas, formularios y acciones.
- Adoptado el español como idioma principal de presentación del producto.

### Fixed

- Corregida la carga de evidencias y autorizaciones en el detalle del evento operativo.
- Fijado explícitamente el esquema del historial de Flyway.
- Corregidas incompatibilidades entre las nuevas plantillas y las pruebas de integración web.

### Security

- Sistema de archivos de aplicación configurado como solo lectura en el stack público.
- Capacidades Linux innecesarias eliminadas del contenedor.
- Exposición pública limitada a la configuración prevista para proxy inverso.
- Variables sensibles mantenidas fuera del repositorio.
- Health checks y validación fail-fast preservados en el entorno desplegable.

## [0.9.0-core-mvp] - 2026-07-31

### Added

- Líneas base aprobadas de Product Discovery, Domain Analysis, System Behavior y Product Scope.
- Diseño técnico, decisiones arquitectónicas y plan de implementación IP-00 a IP-10.
- Proyecto Spring Boot con Java 25 y Maven Wrapper.
- PostgreSQL, Flyway y Testcontainers como base de persistencia y verificación.
- Reglas arquitectónicas mediante ArchUnit.
- Cobertura y gates de calidad mediante JaCoCo y Maven Verify.
- Arquitectura modular con separación entre Domain, Application, Presentation e Infrastructure.
- Autenticación mediante formulario, sesión HTTP, CSRF y cierre de sesión.
- Aprovisionamiento idempotente del usuario responsable.
- Limitación de intentos de inicio de sesión.
- Creación, listado y consulta de cierres operativos.
- Registro y edición de eventos `INCOME`, `EXPENSE`, `DISCOUNT` y `CANCELLATION`.
- Reglas de consistencia para anulaciones y revisión de eventos.
- Registro de evidencias de soporte mediante archivo o referencia.
- Registro y vigencia histórica de autorizaciones.
- Evaluación de VR-001, VR-002, VR-003 y VR-006.
- Generación y resolución de alertas bloqueantes.
- Invalidación y revalidación de resultados dependientes.
- Consolidación transaccional de cierres operativos.
- Persistencia de snapshots inmutables de eventos consolidados.
- Evaluación final VR-008.
- Envío interno único a contabilidad.
- Persistencia estructurada de intentos y causas de rechazo.
- Bloqueo pesimista y pruebas de concurrencia para evitar envíos duplicados.
- Correlation IDs y logging estructurado.
- Errores técnicos sanitizados.
- Endpoints de startup, liveness y readiness.
- Encabezados HTTP de seguridad y configuración de proxies confiables.

### Security

- Cookies de sesión endurecidas.
- HSTS, CSP, `X-Content-Type-Options`, `X-Frame-Options` y políticas adicionales.
- Validación de configuración sensible durante el arranque.
- Protección contra exposición de secretos en logs y respuestas.
- Verificación del almacenamiento de evidencias antes de aceptar tráfico.