# BigoBooks Auth Service

Microservicio de autenticacion de BigoBooks. Expone registro y login de
usuarios y emite el token JWT (HS256) que comparten los demas servicios.

- Aplicacion: `auth-service`
- Puerto: `8084`
- Eureka: `http://localhost:8761/eureka/`
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- Gateway: `http://localhost:8080/auth/**` y `http://localhost:8080/docs/auth`

## Endpoints

| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/auth/register` | Registra un usuario con el rol por defecto (`READER`) |
| POST | `/auth/login` | Autentica y devuelve `token`, `tokenType`, `expiresIn` y `user` |

El login ademas deja el token en la cookie `token` (path `/`).

## Pruebas con curl

Los siguientes ejemplos usan el gateway en `localhost:8080`. Ejecuta primero
Eureka, auth y el gateway, y usa el mismo correo y contraseña en ambas llamadas.

### Registro

```bash
curl -i -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Ana",
    "lastName": "Pérez",
    "email": "ana@example.com",
    "password": "TuClaveSegura123!",
    "city": "Madrid"
  }'
```

Una creación correcta devuelve `201 Created`.

### Login

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "ana@example.com",
    "password": "TuClaveSegura123!"
  }'
```

Un login correcto devuelve `200 OK`, incluye el JWT en la respuesta y lo envía
también en la cookie `token`.

## Requisitos

- Java 21
- Maven (se incluye el wrapper `mvnw` / `mvnw.cmd`)
- Acceso a la base PostgreSQL compartida
- JAR del common: `lib/bigobooks-0.0.1-SNAPSHOT.jar`

## Preparacion local

1. Copiar el ejemplo de credenciales y completar los valores:

   ```powershell
   Copy-Item src/main/resources/config/private.properties.example src/main/resources/config/private.properties
   ```

   Claves: `db.url`, `db.username`, `db.password`, `jwt.secret`, `jwt.expiration-minutes`.

2. Colocar el JAR del common en `lib/`:

   ```powershell
   # Opcion A: copiar el jar ya compilado
   Copy-Item ..\Libreria-MS-Rentas\lib\bigobooks-0.0.1-SNAPSHOT.jar lib\

   # Opcion B: generarlo desde el repo del common
   cd ..\Libreria-Common
   .\mvnw.cmd -B clean install -DskipTests
   Copy-Item target\bigobooks-0.0.1-SNAPSHOT.jar ..\Libreria-MS-Auth\lib\
   ```

   El JAR no se versiona (`lib/*.jar` esta en `.gitignore`).

## Build y ejecucion

```powershell
.\mvnw.cmd -B clean package -DskipTests
java -jar target\auth-service-0.0.1-SNAPSHOT-exec.jar
```

El plugin de Spring Boot genera un JAR ejecutable con clasificador `exec`
(incluye las dependencias de scope `system`, es decir el common).

## Tests

```powershell
.\mvnw.cmd -B test
```

Los tests de contexto completo se ejecutan solo si se define la propiedad de
sistema `auth.test.full`:

```powershell
.\mvnw.cmd -B test -Dauth.test.full=true
```

## Base de datos

Este servicio **no migra el esquema**: usa las tablas compartidas
`user_account`, `role` y `user_wishlist`, y
corre con `spring.jpa.hibernate.ddl-auto=none` y Liquibase deshabilitado.

`jwt.secret` debe ser el mismo que usan los demas servicios para que el token
sea interoperable.
