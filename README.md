# AppDistribution Backend

Ktor-based REST backend for distributing test Android APKs to internal testers, with OTP-based authentication, role-based access control, and MinIO-backed binary storage.

## Prerequisites

- JDK 17
- Docker and Docker Compose (for containerised runs)

## Run with Docker Compose

Start all services (Postgres, MinIO, and the backend) from the worktree root:

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`.

## Run Locally with Gradle

Start the infrastructure dependencies first:

```bash
# From the worktree root
docker compose up -d postgres minio
```

Then run the application:

```bash
./gradlew :backend:run
```

Or build a fat JAR and run it:

```bash
./gradlew :backend:shadowJar
java -jar backend/build/libs/*-all.jar
```

## Environment Variables

| Variable                     | Description                          | Default                                    |
|------------------------------|--------------------------------------|--------------------------------------------|
| `DATABASE_URL`               | JDBC URL for Postgres                | `jdbc:postgresql://localhost:5432/appdist` |
| `DATABASE_USER`              | Postgres username                    | `appdist`                                  |
| `DATABASE_PASSWORD`          | Postgres password                    | `SET_DATABASE_PASSWORD_IN_ENV`                           |
| `STORAGE_ENDPOINT`           | MinIO server URL                     | `http://localhost:9000`                    |
| `STORAGE_ACCESS_KEY`         | MinIO access key                     | `minioadmin`                               |
| `STORAGE_SECRET_KEY`         | MinIO secret key                     | `SET_PRIVATE_VALUE_IN_ENV`                            |
| `STORAGE_BUCKET`             | MinIO bucket for APK binaries        | `appdist-builds`                           |
| `JWT_SECRET`                 | Secret used to sign JWT tokens       | `SET_JWT_SECRET_IN_ENV`                  |
| `JWT_ISSUER`                 | JWT issuer claim                     | `appdist`                                  |
| `JWT_AUDIENCE`               | JWT audience claim                   | `appdist-client`                           |
| `FIREBASE_CREDENTIALS_PATH`  | Path to Firebase service-account JSON| *(unset — push notifications disabled)*   |
| `PORT`                       | HTTP port                            | `8080`                                     |

## API Endpoint Groups

| Group        | Routes                                                        |
|--------------|---------------------------------------------------------------|
| Auth         | `POST /auth/request-otp`, `POST /auth/verify-otp`, `POST /auth/refresh`, `POST /auth/logout` |
| Workspaces   | `GET /workspaces/me`                                          |
| Members      | `GET /workspaces/{id}/members`, `PATCH /workspaces/{id}/members/{userId}`, `DELETE /workspaces/{id}/members/{userId}` |
| Apps         | `GET /apps`, `POST /apps`, `GET /apps/{id}`, `PATCH /apps/{id}`, `DELETE /apps/{id}` |
| Releases     | `GET /apps/{id}/releases`, `POST /apps/{id}/releases`, `GET /apps/{id}/releases/{rid}`, `DELETE /apps/{id}/releases/{rid}`, `GET /apps/{id}/releases/{rid}/download` |
| Invitations  | `POST /invitations`, `GET /invitations/{token}`, `POST /invitations/{token}/accept` |
