# AppDistribution Backend

Ktor-based backend for distributing test Android APKs.

## Requirements

- JDK 21+
- Docker + Docker Compose

## Quick Start

### Using Docker Compose (recommended)

```bash
# Start Postgres and MinIO
docker-compose up -d

# Build and run the application
./gradlew run
```

### Using Docker

```bash
# Build the image
docker build -t appdist-backend .

# Run the container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/appdist \
  -e DATABASE_USER=appdist \
  -e DATABASE_PASSWORD=appdist_secret \
  -e MINIO_ENDPOINT=http://host.docker.internal:9000 \
  -e MINIO_ACCESS_KEY=minioadmin \
  -e MINIO_SECRET_KEY=minioadmin123 \
  -e JWT_SECRET=your-secret-here \
  appdist-backend
```

## Environment Variables

| Variable              | Description                        | Example                                          |
|-----------------------|------------------------------------|--------------------------------------------------|
| `DATABASE_URL`        | JDBC connection URL for Postgres   | `jdbc:postgresql://localhost:5432/appdist`       |
| `DATABASE_USER`       | Postgres username                  | `appdist`                                        |
| `DATABASE_PASSWORD`   | Postgres password                  | `appdist_secret`                                 |
| `MINIO_ENDPOINT`      | MinIO server URL                   | `http://localhost:9000`                          |
| `MINIO_ACCESS_KEY`    | MinIO access key                   | `minioadmin`                                     |
| `MINIO_SECRET_KEY`    | MinIO secret key                   | `minioadmin123`                                  |
| `JWT_SECRET`          | Secret used to sign JWT tokens     | `change-me-in-production`                        |

## API Examples

### Auth Flow

Workspace and the first user (ADMIN) are auto-created on the first successful OTP login. There is no separate registration step.

**1. Request OTP**

```bash
curl -X POST http://localhost:8080/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com"}'
```

Response:
```json
{"message": "OTP sent"}
```

**2. Verify OTP (auto-creates workspace + first user as ADMIN)**

```bash
curl -X POST http://localhost:8080/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "otp": "123456"}'
```

Response:
```json
{
  "token": "<jwt>",
  "workspace_id": "ws_abc123"
}
```

### Upload APK

```bash
curl -X POST http://localhost:8080/apps/{appId}/releases \
  -H "Authorization: Bearer <jwt>" \
  -F "file=@app-release.apk" \
  -F "version_name=1.0.0" \
  -F "version_code=1" \
  -F "release_notes=Initial release"
```

Response:
```json
{
  "id": "rel_xyz789",
  "version_name": "1.0.0",
  "version_code": 1,
  "created_at": "2026-03-22T10:00:00Z"
}
```

### Get Download URL

```bash
curl -X GET http://localhost:8080/apps/{appId}/releases/{releaseId}/download \
  -H "Authorization: Bearer <jwt>"
```

Response:
```json
{
  "url": "http://localhost:9000/apks/app-release.apk?X-Amz-Signature=..."
}
```
