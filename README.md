# Employee Time Tracker

Sistema de control horario para el negocio: kiosco de fichaje con PIN, panel de
administración con dashboard de métricas, reportes para liquidación y API REST
segura. Pensado para operar un local hoy y escalar a multi-sucursal (franquicia)
mañana.

> El código vive en el módulo Maven [`employee-time-tracker/`](employee-time-tracker/).

## Qué incluye

**Operación diaria**
- Kiosco de fichaje (`/`): el dispositivo del local se configura una vez (cuenta
  `KIOSK`) y cada empleado ficha entrada/salida/breaks con su PIN (BCrypt,
  con bloqueo de 1 minuto tras 5 intentos fallidos).
- Panel admin (`/admin`): alta/edición de empleados, PINes, quién trabaja ahora,
  corrección de jornadas (auditada) y reportes.
- Cierre automático de jornadas olvidadas (job programado) marcadas para revisión.

**Análisis de datos (pestaña "Análisis" del panel)**
- KPIs del período: horas trabajadas, costo laboral estimado, promedio por
  jornada, % de llegadas tarde y ausencias.
- Gráficos: tendencia diaria de horas y horas por empleado.
- Puntualidad por empleado (vs hora esperada de entrada, con tolerancia
  configurable) y horas extra (exceso diario y semanal).
- **Liquidación**: cuánto pagarle a cada empleado en el período (horas netas ×
  valor hora); las jornadas marcadas como **dobles** (feriados) cuentan ×2.
- **Pagos**: marcar el período como pagado lo congela (las jornadas no se
  editan hasta reabrir el pago, todo auditado) y queda en el historial;
  mensaje de desglose diario listo para **WhatsApp** por empleado.
- Alertas de jornadas auto-cerradas pendientes de corrección (bloquean el pago).
- Exportes CSV compatibles con Excel (separador `;`, UTF-8 con BOM).

**Base técnica**
- Spring Boot 3.5 / Java 21, Spring Security con JWT + refresh tokens rotativos.
- Flyway para migraciones de esquema (MySQL, PostgreSQL y H2).
- Auditoría: `created_at`/`updated_at` en todas las tablas y bitácora
  `audit_log` de ediciones/borrados manuales de jornadas.
- Actuator (`/actuator/health`), suite de ~50 tests (unitarios + integración
  end-to-end) y CI en GitHub Actions.

## Cómo correr

### Desarrollo local

Requiere Java 21 y una base MySQL o PostgreSQL.

```bash
cd employee-time-tracker
export SPRING_PROFILES_ACTIVE=dev
export DB_URL="jdbc:mysql://localhost:3306/employee_time_tracker?createDatabaseIfNotExist=true"
export DB_USERNAME=root
export DB_PASSWORD=tu_password
./mvnw spring-boot:run
```

- Kiosco: http://localhost:8080/ · Panel: http://localhost:8080/admin
- Swagger: http://localhost:8080/swagger-ui.html
- Usuarios seed (solo si no existen): `admin/admin1234` y `kiosk/kiosk1234`.
  **Cambialos en producción** vía `ADMIN_USERNAME`/`ADMIN_PASSWORD` (y kiosk).

### Tests

```bash
cd employee-time-tracker && ./mvnw test
```

Corren contra H2 en memoria con las migraciones reales de Flyway.

### Docker

```bash
cd employee-time-tracker
docker build -t employee-time-tracker .
docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... employee-time-tracker
```

## Variables de entorno

| Variable | Obligatoria en prod | Descripción |
|---|---|---|
| `DB_URL` | ✅ | JDBC URL (MySQL o PostgreSQL; Flyway detecta el motor) |
| `DB_USERNAME` / `DB_PASSWORD` | ✅ | Credenciales de la base |
| `JWT_SECRET` | ✅ | Clave HMAC de ≥ 32 caracteres |
| `SPRING_PROFILES_ACTIVE` | — | `dev` para desarrollo; sin definir se asume `prod` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | recomendado | Credenciales del admin seed |
| `JWT_EXPIRATION_MS` | — | Vida del access token (default 1 h) |
| `JWT_REFRESH_EXPIRATION_MS` | — | Vida del refresh token (default 30 días) |
| `PORT` | — | Puerto HTTP (Render lo inyecta) |
| `LATE_TOLERANCE_MINUTES` | — | Gracia antes de contar tardanza (default 10) |
| `DAILY_OVERTIME_HOURS` | — | Umbral diario de overtime (default 8) |
| `WEEKLY_HOURS_TARGET` | — | Tope semanal default (48, jornada legal AR) |
| `app.timezone` | — | Zona horaria (default America/Argentina/Buenos_Aires) |

> ⚠️ Nota de seguridad: versiones viejas de este repo tenían una contraseña de
> desarrollo commiteada en `application.properties`. Ya no está en el código,
> pero sigue en el historial de git: **rotá esa contraseña** en tu MySQL local.

## Esquema y migraciones

El esquema lo administra **Flyway** (`src/main/resources/db/migration/{vendor}`)
y Hibernate solo lo valida (`ddl-auto=validate`). Las bases creadas antes de
Flyway se adoptan solas gracias a `baseline-on-migrate` (se marcan en V1 y se
aplican V2+). Para cambiar el esquema: agregar `V<n>__descripcion.sql` en las
tres carpetas (`mysql`, `postgresql`, `h2`) — nunca editar una migración ya
aplicada.

## Seguridad

- **Roles**: `ADMIN` (gestión y reportes), `EMPLOYEE` (fichaje propio vía
  `/me`), `KIOSK` (dispositivo compartido del local).
- **Login** `POST /api/auth/login` → access token (JWT, 1 h) + refresh token
  opaco (30 días, rotativo: cada uso lo revoca y emite uno nuevo). En la base
  solo se guarda el hash SHA-256 del refresh token.
- **Renovación** `POST /api/auth/refresh`; el kiosco y el panel renuevan solos.
- Alta de usuarios (`/api/auth/register`) restringida a ADMIN.

## API (resumen)

Documentación completa en Swagger (`/swagger-ui.html`).

| Grupo | Endpoints clave |
|---|---|
| Auth | `POST /api/auth/login`, `/refresh`, `/register` |
| Empleados (ADMIN) | ABM completo `/api/employees` (DELETE solo sin historial; sino desactivar), `PUT /{id}/pin`, activar/desactivar, worked-hours |
| Kiosco (KIOSK) | `/api/kiosk/employees`, `verify`, `clock-in/out`, `break/start|end`, `worked-hours` |
| Fichaje propio (EMPLOYEE) | `/api/time-entries/me/*`, `/api/breaks/me/*` |
| Jornadas (ADMIN) | ABM: `POST /api/time-entries` (alta manual de jornada olvidada, sin solapamientos), `PUT/DELETE /{id}`, `PATCH /{id}/paid-double` (feriado ×2) — todo queda en `audit_log` |
| Reportes (ADMIN) | `weekly-report(+/csv)`, `entries` y `employees/{id}/entries` (paginados) |
| Analytics (ADMIN) | `summary(+/csv)`, `payroll(+/csv)` (liquidación), `payroll/{id}/message` (WhatsApp), `pending-fixes`, `punctuality`, `overtime(+/csv)`, `absences`, `trends`, `audit-log` |
| Pagos (ADMIN) | `POST /api/payments` (cierra el período), `GET` (historial), `DELETE /{id}` (reabre) |

Convenciones: listas vacías devuelven `200 []` (no 404); los listados grandes
se paginan (`?page=&size=`, máx. 500); errores con cuerpo JSON uniforme.

## Roadmap: de un local a franquicia

La base ya está preparada (migraciones, auditoría, tests, CI, métricas). La
próxima etapa para operar múltiples locales es **multi-sucursal**:

1. Entidad `Location` (sucursal) con FK en `Employee` y en las cuentas `KIOSK`;
   cada kiosco pertenece a un local y solo lista a sus empleados.
2. Scoping de reportes y analytics por sucursal + vista consolidada del dueño
   (comparativa de horas, costo y ausentismo entre locales), y un rol
   `FRANCHISEE` que solo ve su local.

Con eso, cada franquiciado opera su local con el mismo sistema y la marca ve
todo el negocio en un solo dashboard.

## Stack

Java 21 · Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) ·
Flyway · MySQL/PostgreSQL (H2 en tests) · JJWT · Lombok · springdoc-openapi ·
Chart.js (vendorizado) · GitHub Actions.
