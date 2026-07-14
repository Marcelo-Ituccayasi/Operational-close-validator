# Alcance del MVP

**Versión:** v0.3

**Estado:** Línea base aprobada

**Fase:** 04 — Alcance del producto

**Producto:** Operational Close Validator

## Propósito

Este documento define el alcance aprobado del MVP de Operational Close Validator. Establece qué problema demostrará resolver, qué capacidades deberá incluir, qué elementos quedan fuera, cuáles son sus restricciones y qué condiciones deben cumplirse para considerarlo terminado.

El documento delimita el producto. No selecciona framework, lenguaje, proveedor de nube, patrón arquitectónico, esquema físico de base de datos ni contratos concretos de API. Esas decisiones pertenecen a Technical Design [diseño técnico].

## Objetivo del MVP

Demostrar que un Evento Operativo registrado sin la Evidencia de Soporte o Autorización formal requerida, o con una inconsistencia verificable, puede detectarse antes del envío del Cierre Operativo a contabilidad.

Cuando se detecta una condición bloqueante, el sistema debe:

1. registrar el Resultado de Validación correspondiente;
2. colocar el Evento Operativo en un estado coherente con la falla;
3. generar o mantener una Alerta bloqueante;
4. impedir que el Cierre Operativo quede Validado o sea enviado;
5. permitir la corrección de la causa;
6. invalidar los resultados dependientes anteriores;
7. exigir revalidación exitosa;
8. exigir una nueva consolidación cuando corresponda;
9. ejecutar nuevamente VR-008 inmediatamente antes del envío.

## Hipótesis de producto

Una validación temprana, trazable y bloqueante de los eventos conocidos por el sistema reducirá la probabilidad de que un cierre llegue a consolidación o contabilidad con soportes faltantes, autorizaciones informales o inconsistencias de registro.

El MVP valida la viabilidad funcional de esta hipótesis mediante un flujo completo y demostrable. No pretende medir todavía el impacto estadístico real en múltiples organizaciones.

## Usuario del MVP

### Usuario responsable

El MVP incluye un único usuario responsable preconfigurado que se autentica mediante un mecanismo básico y ejecuta el flujo completo.

El usuario puede:

- registrar y corregir Eventos Operativos;
- adjuntar Evidencia de Soporte;
- registrar Autorizaciones formales;
- ejecutar validaciones;
- revisar y gestionar Alertas;
- consolidar el Cierre Operativo;
- ejecutar el control final;
- registrar el envío interno a contabilidad.

Las funciones de gerencia y contabilidad se simulan mediante datos y acciones dentro del mismo flujo. No existen como usuarios o roles independientes en el MVP.

## Contexto operativo cubierto

El MVP se limita al cierre operativo de caja y trabaja con movimientos conocidos y registrados dentro del sistema.

Tipos de Evento Operativo incluidos:

- Ingreso;
- Egreso;
- Descuento;
- Anulación.

Cada Evento Operativo pertenece a un Cierre Operativo y conserva información suficiente para aplicar las reglas correspondientes y mantener trazabilidad.

## Capacidades funcionales incluidas

### 1. Autenticación básica

El sistema debe permitir el acceso del usuario responsable preconfigurado y rechazar credenciales inválidas.

La autenticación protege las acciones del MVP. La gestión completa de identidades, recuperación de contraseña, registro público y administración de usuarios quedan fuera.

### 2. Registro de Eventos Operativos

El usuario debe poder registrar Eventos Operativos de los cuatro tipos aprobados.

Datos mínimos generales:

- tipo de evento;
- monto;
- fecha y hora de ocurrencia;
- fecha y hora de registro;
- responsable;
- motivo o descripción;
- Cierre Operativo relacionado.

Según el tipo y la regla aplicable, el evento también puede requerir Evidencia de Soporte o Autorización formal.

El producto no crea eventos a partir de movimientos físicos, bancarios o contables externos. Solo valida eventos conocidos por el sistema.

### 3. Evidencia de Soporte

El usuario debe poder adjuntar un archivo o registrar una referencia verificable asociada a un Evento Operativo.

La evidencia debe conservar, cuando corresponda:

- tipo;
- referencia o archivo;
- monto respaldado;
- fecha;
- condición de legibilidad;
- Evento Operativo relacionado.

Modificar o reemplazar evidencia relevante invalida los Resultados de Validación dependientes.

### 4. Autorización formal

El usuario debe poder registrar una Autorización formal y verificable cuando el evento la requiera.

Datos mínimos:

- Evento Operativo relacionado;
- responsable de autorizar;
- motivo;
- fecha y hora;
- referencia formal verificable.

Modificar una autorización relevante invalida los Resultados de Validación dependientes.

### 5. Validación de Eventos Operativos

El sistema debe determinar las reglas aplicables, ejecutarlas y registrar un Resultado de Validación por regla.

Valores aprobados del resultado:

- Satisfecha;
- Fallida;
- Pendiente, únicamente para el dominio completo cuando corresponda y no como resultado operativo normal del subconjunto MVP.

Un Evento Operativo solo puede quedar Validado cuando todos sus Resultados de Validación aplicables están vigentes y satisfechos y no existe una Alerta bloqueante activa.

### 6. Catálogo fijo de reglas

El MVP incluye únicamente las siguientes reglas:

- VR-001 — Registro trazable de movimientos autorizados;
- VR-002 — Coincidencia de monto de ingreso;
- VR-003 — Evidencia legible para egresos menores;
- VR-006 — Autorización formal de descuento o anulación;
- VR-008 — Control final antes del envío.

El catálogo es interno, fijo y versionado. El usuario no puede crear, editar, habilitar ni deshabilitar reglas.

### 7. Alertas

Una falla relevante debe poder generar o mantener una Alerta vinculada a la entidad y regla correspondientes.

Estados incluidos:

- Activa;
- Reconocida;
- En revisión;
- Resuelta;
- Descartada.

Restricciones:

- reconocer una Alerta no elimina su efecto bloqueante;
- una Alerta no puede marcarse manualmente como Resuelta;
- la resolución exige corregir la causa, revalidar la entidad afectada y conservar un Resultado de Validación vigente asociado;
- descartar exige autorización, justificación, fecha y responsable;
- descartar una Alerta no valida automáticamente el Evento Operativo ni el Cierre Operativo.

### 8. Corrección y revalidación

El usuario debe poder corregir datos, adjuntar o reemplazar evidencia y registrar la autorización faltante.

Después de una modificación relevante:

1. los Resultados de Validación dependientes quedan no vigentes;
2. un Evento Operativo previamente Validado pasa a Registrado;
3. si el Cierre Operativo estaba Validado, pasa a Bloqueado;
4. la consolidación afectada queda no vigente;
5. el evento debe revalidarse;
6. la revalidación determina su nuevo estado real;
7. una Alerta solo pasa a Resuelta después de una revalidación exitosa.

### 9. Consolidación del Cierre Operativo

El sistema debe calcular y conservar, como mínimo:

- totales por tipo de Evento Operativo;
- saldo inicial;
- saldo esperado;
- saldo real;
- diferencia;
- fecha y hora de consolidación;
- usuario responsable.

La consolidación final solo puede completarse cuando:

- todos los Eventos Operativos están Validados;
- no existen Alertas bloqueantes activas;
- todos los Resultados de Validación aplicables están vigentes y satisfechos.

Una corrección posterior invalida la consolidación cuando modifica datos de los cuales depende.

### 10. Control final y envío interno a contabilidad

El envío solo puede solicitarse desde un Cierre Operativo en estado Validado y con consolidación completa y vigente.

El sistema ejecuta VR-008 inmediatamente antes del envío y verifica:

- todos los Eventos Operativos continúan Validados;
- no existen Alertas bloqueantes activas;
- todos los Resultados de Validación aplicables están vigentes y satisfechos;
- la consolidación está completa y vigente.

Resultado exitoso:

- VR-008 queda Satisfecha;
- se registran fecha, hora y responsable;
- el cierre pasa a Enviado a contabilidad.

Resultado fallido:

- VR-008 queda Fallida;
- el envío se rechaza;
- el cierre pasa de Validado a Bloqueado;
- se conserva la causa;
- se exige corrección, revalidación y nueva consolidación.

El envío es una transición interna. El MVP no integra un sistema contable externo.

## Estados incluidos en el MVP

### Cierre Operativo

- Preparación;
- Bloqueado;
- Validado;
- Enviado a contabilidad.

`Enviado a contabilidad` es terminal dentro del MVP.

### Evento Operativo

- Registrado;
- Pendiente de soporte;
- Pendiente de autorización;
- Con observaciones;
- Validado.

### Alerta

- Activa;
- Reconocida;
- En revisión;
- Resuelta;
- Descartada.

## Flujo principal demostrable

1. El usuario se autentica.
2. Trabaja sobre un Cierre Operativo disponible.
3. Registra Eventos Operativos.
4. Adjunta evidencia o registra autorizaciones cuando corresponda.
5. Ejecuta las reglas aplicables.
6. El sistema registra resultados y genera Alertas ante fallas.
7. El cierre permanece en Preparación o pasa a Bloqueado mientras exista una condición bloqueante.
8. El usuario reconoce o revisa las Alertas.
9. Corrige las causas.
10. El sistema invalida resultados y consolidaciones dependientes.
11. El usuario revalida los eventos.
12. Las Alertas solo se resuelven después de revalidación exitosa.
13. El usuario ejecuta nuevamente la consolidación.
14. El cierre pasa a Validado cuando cumple todas las condiciones.
15. El usuario solicita el envío interno.
16. El sistema ejecuta VR-008.
17. El cierre pasa a Enviado a contabilidad o vuelve a Bloqueado.

## Persistencia y trazabilidad mínimas

El MVP debe conservar de forma persistente:

- usuario preconfigurado y datos necesarios para autenticación;
- Cierres Operativos;
- Eventos Operativos;
- Evidencias de Soporte;
- Autorizaciones;
- catálogo fijo de Reglas de Validación;
- Resultados de Validación y su vigencia;
- Alertas, estados, justificaciones y responsables;
- consolidaciones y su vigencia;
- transiciones relevantes de estado;
- fecha, hora y responsable del envío interno.

La persistencia debe sobrevivir al reinicio de la aplicación y permitir reconstruir el estado actual del cierre y la causa de cada bloqueo.

## Características mínimas del producto entregable

El MVP terminado debe ser una aplicación web ejecutable que incluya:

- interfaz de usuario para el flujo funcional aprobado;
- lógica de aplicación y dominio;
- persistencia relacional;
- autenticación básica;
- validaciones automáticas deterministas;
- gestión de Alertas y estados;
- pruebas automatizadas de las reglas y transiciones críticas;
- configuración reproducible del entorno;
- despliegue público de demostración.

La selección del stack, el diseño de componentes, la estructura de la API y la plataforma de despliegue se decidirán durante Technical Design [diseño técnico].

## Escenarios obligatorios de aceptación

### Escenario A — Evento conforme

1. El usuario registra un evento con sus datos requeridos.
2. Adjunta evidencia o autorización cuando corresponde.
3. Las reglas aplicables quedan Satisfechas.
4. El Evento Operativo pasa a Validado.

### Escenario B — Bloqueo por falta de soporte o autorización

1. El usuario registra un Evento Operativo con todos los datos mínimos, pero sin la Evidencia de Soporte o Autorización formal requerida.
2. VR-001, VR-003 o VR-006 queda Fallida, según corresponda.
3. El evento pasa a Pendiente de soporte o Pendiente de autorización.
4. Se genera una Alerta bloqueante.
5. El cierre no puede quedar Validado ni enviarse.

### Escenario C — Bloqueo por inconsistencia

1. Un Ingreso no coincide con el monto de su evidencia o existe una inconsistencia de registro o trazabilidad.
2. VR-002 o VR-001 queda Fallida.
3. El evento pasa a Con observaciones.
4. Se genera una Alerta bloqueante.

### Escenario D — Corrección y revalidación

1. El usuario corrige la causa.
2. Los resultados dependientes anteriores quedan no vigentes.
3. El evento pasa a Registrado cuando antes estaba Validado.
4. El usuario ejecuta nuevamente la validación.
5. La Alerta solo queda Resuelta cuando la revalidación es exitosa.
6. Si la corrección afectó una consolidación existente, esta queda no vigente y debe ejecutarse nuevamente.

### Escenario E — Envío exitoso

1. Todos los eventos están Validados.
2. No existen Alertas bloqueantes activas.
3. Todos los resultados aplicables están vigentes y satisfechos.
4. La consolidación está completa y vigente.
5. VR-008 queda Satisfecha inmediatamente antes del envío.
6. El cierre pasa a Enviado a contabilidad.

### Escenario F — Rechazo durante el control final

1. El cierre está Validado.
2. VR-008 detecta un evento no Validado, una Alerta bloqueante, un resultado fallido o no vigente, o una consolidación incompleta o desactualizada.
3. El envío se rechaza.
4. El cierre pasa a Bloqueado.
5. Se conserva la causa y se exige corrección, revalidación y nueva consolidación.

## Criterios de aceptación transversal

1. Ninguna acción protegida se ejecuta sin autenticación.
2. Ningún Evento Operativo queda Validado con Resultados de Validación aplicables fallidos o no vigentes.
3. Ningún Cierre Operativo queda Validado con un Evento Operativo cuyo estado no sea Validado.
4. Ningún cierre queda Validado con una Alerta bloqueante activa.
5. Toda modificación relevante invalida los resultados dependientes.
6. Una Alerta solo queda Resuelta después de una revalidación exitosa.
7. Una Alerta Descartada requiere una acción autorizada y conserva la justificación, la fecha y el responsable.
8. Una corrección que afecta la consolidación exige consolidar nuevamente.
9. VR-008 se ejecuta inmediatamente antes del envío.
10. Una falla de VR-008 rechaza el envío y coloca el cierre en Bloqueado.
11. El cierre Enviado a contabilidad no admite modificaciones dentro del MVP.
12. Los datos persistidos permiten reconstruir el estado y la causa de bloqueo.
13. Las reglas y transiciones críticas cuentan con pruebas automatizadas.
14. La aplicación puede ejecutarse desde una configuración documentada y reproducible.
15. Existe una versión desplegada para demostración pública.

## Fuera del alcance del MVP

### Usuarios y organización

- múltiples usuarios;
- roles reales separados de caja, gerencia y contabilidad;
- administración de usuarios y permisos;
- múltiples sucursales o empresas;
- aprobación remota por parte de gerencia.

### Procesos operativos

- gestión completa de facturas de proveedores;
- conciliación bancaria;
- conciliación con POS o pasarelas de pago;
- gestión de inventario;
- detección automática de movimientos físicos o externos nunca registrados;
- cierre Provisional;
- reapertura, rectificación o anulación de un cierre Enviado a contabilidad.

### Configuración e integraciones

- creación o edición de reglas por el usuario;
- motor dinámico de reglas;
- integración real con ERP, POS, bancos, facturación electrónica o sistemas contables;
- envío real de asientos o documentos a contabilidad;
- notificaciones externas o flujos de aprobación remotos.

### Analítica avanzada

- predicción de fallas;
- detección estadística de anomalías;
- indicadores corporativos multiempresa;
- automatización basada en modelos de inteligencia artificial.

Estos elementos requieren decisiones posteriores y no deben incorporarse de forma implícita durante la implementación.

## Limitaciones conocidas

1. El producto no puede detectar directamente un movimiento real que nunca fue registrado ni recibido mediante una integración.
2. La validación se limita a los datos y evidencias disponibles en el sistema.
3. El usuario único simula responsabilidades que en un producto posterior podrían corresponder a roles diferentes.
4. El envío a contabilidad es un registro interno, no una entrega técnica a un sistema externo.
5. El MVP demuestra comportamiento funcional; la medición de impacto real requiere pilotaje posterior.

## Supuestos

- existe al menos un Cierre Operativo disponible para ejecutar el flujo;
- el usuario conoce los movimientos que debe registrar;
- los archivos o referencias de evidencia pueden almacenarse y recuperarse;
- los montos usan una moneda definida por la configuración técnica;
- las reglas aprobadas son suficientes para demostrar el valor central;
- no existen dependencias externas obligatorias para completar el flujo MVP.

## Matriz de trazabilidad del alcance

- Autenticación básica → UC-001.
- Registro de eventos → UC-002.
- Evidencia de Soporte → UC-003; VR-001, VR-002 y VR-003.
- Autorización formal → UC-004; VR-001 y VR-006.
- Validación de eventos → UC-005; VR-001, VR-002, VR-003 y VR-006.
- Gestión de Alertas → UC-006; estados de Alerta.
- Corrección y revalidación → UC-007; invalidación de resultados y resolución de Alertas.
- Consolidación → UC-008; transiciones Preparación o Bloqueado → Validado.
- Envío interno → UC-009; VR-008 y FM-008.
- Rechazo final → transición Validado → Bloqueado; ADR-0001.
- Limitación de eventos no registrados → FM-001 y declaración de límites del dominio.

## Definición de terminado

El MVP se considera terminado cuando:

1. todas las capacidades incluidas están implementadas;
2. los escenarios obligatorios de aceptación funcionan de extremo a extremo;
3. los estados e invariantes coinciden con las líneas base aprobadas;
4. las reglas VR-001, VR-002, VR-003, VR-006 y VR-008 están implementadas y probadas;
5. los datos relevantes persisten y conservan trazabilidad;
6. el flujo bloqueado, la corrección, la revalidación y la nueva consolidación están demostrados;
7. el envío interno exitoso y su rechazo por VR-008 están demostrados;
8. la aplicación puede configurarse y ejecutarse de forma reproducible;
9. existe documentación mínima de uso y ejecución;
10. existe un despliegue público de demostración;
11. no se incorporaron elementos fuera de alcance sin una decisión formal.

## Control de cambios

El alcance v0.3 permanece congelado después de su aprobación en GitHub. Cualquier nueva capacidad, regla, actor, integración o estado requiere:

1. registrar la necesidad;
2. evaluar el impacto sobre problema, dominio, comportamiento y arquitectura;
3. aprobar formalmente la decisión;
4. actualizar la versión de los documentos afectados;
5. incorporar el cambio mediante un commit y revisión independientes.

## Conclusión

El MVP no intenta reemplazar un ERP, POS, sistema contable, plataforma bancaria ni sistema de inventario. Su responsabilidad es validar de forma temprana y trazable los Eventos Operativos registrados, bloquear cierres inconsistentes y permitir que únicamente un Cierre Operativo validado y nuevamente verificado sea puesto a disposición de contabilidad.