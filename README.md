# AppDistribution Backend

Ktor-based REST backend for distributing test Android APKs to internal testers, with OTP-based authentication, role-based access control, and MinIO-backed binary storage.

## Prerequisites

- JDK 17
- Docker and Docker Compose

## Run with Docker Compose

Copy the example environment file and set local secrets:

```bash
cp .env.example .env
```

Then start the full stack from the backend repo root:

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`.

## Run Locally with Gradle

Start the infrastructure dependencies first:

```bash
docker compose up -d postgres minio
```

Then run the application:

```bash
./gradlew run
```

Or build a fat JAR and run it:

```bash
./gradlew shadowJar
java -jar build/libs/*-all.jar
```

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL for Postgres | `jdbc:postgresql://localhost:5433/appdist` |
| `DATABASE_USER` | Postgres username | `appdist` |
| `DATABASE_PASSWORD` | Postgres password | required via `.env` |
| `MINIO_ENDPOINT` | MinIO server URL | `http://localhost:9000` |
| `MINIO_PUBLIC_ENDPOINT` | Public MinIO URL for clients | `http://10.0.2.2:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | required via `.env` |
| `MINIO_BUCKET` | MinIO bucket for APK binaries | `appdist-builds` |
| `JWT_SECRET` | Secret used to sign JWT tokens | required via `.env`, minimum 32 chars |
| `JWT_ISSUER` | JWT issuer claim | `appdist` |
| `JWT_AUDIENCE` | JWT audience claim | `appdist-client` |
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service-account JSON | unset, push notifications disabled |
| `PORT` | HTTP port | `8080` |

## Security Notes

- The repository does not ship real passwords, JWT secrets, or Firebase service-account credentials.
- `application.conf` contains placeholders and the app will fail fast if you do not provide real secret values.
- Keep `.env` private and never commit it to the public repository.

## API Endpoint Groups

| Group | Routes |
|---|---|
| Auth | `POST /auth/request-otp`, `POST /auth/verify-otp`, `POST /auth/refresh`, `POST /auth/logout` |
| Workspaces | `GET /workspaces/me` |
| Members | `GET /workspaces/{id}/members`, `PATCH /workspaces/{id}/members/{userId}`, `DELETE /workspaces/{id}/members/{userId}` |
| Apps | `GET /apps`, `POST /apps`, `GET /apps/{id}`, `PATCH /apps/{id}`, `DELETE /apps/{id}` |
| Releases | `GET /apps/{id}/releases`, `POST /apps/{id}/releases`, `GET /apps/{id}/releases/{rid}`, `DELETE /apps/{id}/releases/{rid}`, `GET /apps/{id}/releases/{rid}/download` |
| Invitations | `POST /invitations`, `GET /invitations/{token}`, `POST /invitations/{token}/accept` |
