# File Listing App

A containerized full-stack application for generating and listing files from a mounted input directory.

The application can:

* generate a deep folder structure under an input directory
* recursively list unique file names by extension
* store query history in PostgreSQL
* expose REST endpoints with OpenAPI/Swagger documentation
* run the full stack with one Makefile command

## Tech stack

### Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* PostgreSQL
* Springdoc OpenAPI
* Gradle

### Frontend

* Vite
* Vanilla TypeScript
* Nginx container for serving the production build

### Infrastructure

* Podman
* PostgreSQL 16 container
* Makefile-based container orchestration
* No Docker Compose

## Project structure

```text
file-listing-app/
├── backend/
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   └── src/
├── input/
│   └── sample files, generated files are created here at runtime
├── Makefile
└── README.md
```

## Running the application

### Prerequisites

Make sure the following tools are installed and available from the terminal:

```text
podman
make
```

On Windows, the application can be started from WSL or another terminal where `make` is available.

The Podman machine must be running before starting the application.

## Start the application

From the project root:

```bash
make run
```

This command:

1. creates a Podman network
2. starts the PostgreSQL container
3. builds and starts the backend container
4. builds and starts the frontend container
5. mounts the local `input` directory into the backend container as `/input`

No separate database setup, Gradle build, `npm install`, or Docker Compose command is required.

## Stop the application

```bash
make stop
```

## Clean containers and images

```bash
make clean
```

## Check running containers

```bash
make ps
```

The application uses three runtime containers:

```text
file-listing-db
file-listing-backend
file-listing-frontend
```

## Application URLs

Frontend:

```text
http://localhost:3000
```

Backend:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI documentation:

```text
http://localhost:8080/v3/api-docs
```

PostgreSQL is exposed on host port:

```text
5433
```

Inside the Podman network, the backend connects to the database using:

```text
jdbc:postgresql://file-listing-db:5432/filedb
```

## Input directory

The local project directory:

```text
input/
```

is mounted into the backend container as:

```text
/input
```

When using the API or frontend, use `/input` as the base path.

Example:

```text
/input
```

Generated folders and files are created inside the mounted input directory, so they are visible both inside the container and on the host machine.

The backend restricts file operations to the configured input root. This prevents requests from accessing paths outside the mounted input directory.

## REST endpoints

### Generate deep file structure

```http
POST /api/generate
```

Example request:

```json
{
  "basePath": "/input",
  "depth": 10,
  "filesPerDirectory": 1,
  "extension": "txt"
}
```

This creates a deep directory structure under:

```text
/input/generated
```

Before generating a new structure, the existing `generated` directory under the selected base path is removed and recreated. This keeps the generated output clean and predictable.

Directory names are generated alphabetically:

```text
a, b, c, ..., z, aa, ab, ac, ...
```

Files are generated with numeric names:

```text
1.txt, 2.txt, 3.txt, ...
```

Example response:

```json
{
  "basePath": "/input",
  "depth": 10,
  "createdDirectories": 10,
  "createdFiles": 10,
  "deepestPath": "a/b/c/d/e/f/g/h/i/j"
}
```

### List files

```http
GET /api/list?path=/input&extension=txt
```

Example response:

```json
{
  "requestedPath": "/input",
  "extension": "txt",
  "files": [
    "1.txt",
    "2.txt",
    "a.txt",
    "c.txt"
  ],
  "count": 4
}
```

The file listing is recursive and searches through all subdirectories under the requested path.

The response contains unique file names. If the same file name appears in multiple directories, it is returned only once because the backend stores the result in a `HashSet`.

For example, if the input directory contains:

```text
/input/a.txt
/input/nested/a.txt
/input/nested/c.txt
```

the response will contain:

```text
a.txt
c.txt
```

The `count` field represents the number of unique file names returned.

If the `extension` parameter is empty or omitted, the endpoint lists all regular files regardless of extension.

Example:

```http
GET /api/list?path=/input
```

### Get query history

```http
GET /api/history
```

Example response:

```json
[
  {
    "id": 1,
    "runUser": "melinda",
    "runUid": "1000",
    "runGid": "1000",
    "requestedPath": "/input",
    "extension": "txt",
    "requestedAt": "2026-07-13T10:30:00",
    "resultCount": 5,
    "status": "SUCCESS"
  }
]
```

The history stores:

* the user who started the backend container
* requested path
* requested extension
* request timestamp
* result count
* status

The status can be:

* `SUCCESS` for successful file listing requests
* `GENERATED` for successful file generation requests
* `FAILED` for failed generation or listing requests

## Database

The PostgreSQL database is created automatically when the database container starts.

The database configuration is:

```text
database: filedb
user: fileuser
password: filepass
```

The application tables are created and updated automatically by Hibernate based on the JPA entities.

No persistent database volume is used. The stored history is intended for local demo and runtime usage, and it can be reset when the database container is removed.

## OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

The raw OpenAPI documentation is available at:

```text
http://localhost:8080/v3/api-docs
```

The OpenAPI documentation is generated automatically from the Spring Boot REST controllers by Springdoc OpenAPI.

## Error handling

Invalid requests return a structured JSON error response.

Example:

```json
{
  "message": "Path does not exist or cannot be accessed: /wrong",
  "status": 400,
  "error": "Bad Request",
  "timestamp": "2026-07-13T10:30:00"
}
```

Failed list and generate requests are also stored in the query history with `FAILED` status.

## Notes about generated files

The `POST /api/generate` endpoint writes into the mounted `/input` directory. Because of this, the backend container must mount the input directory with write permission.

The Makefile handles this automatically.

Generated files are runtime output. They do not need to be committed to the repository.

## Development notes

For local backend development, the backend can also be started with Gradle:

```bash
cd backend
./gradlew bootRun
```

For local frontend development, the frontend can be started with Vite:

```bash
cd frontend
npm install
npm run dev
```

The development frontend runs on:

```text
http://localhost:5173
```

The production frontend container runs on:

```text
http://localhost:3000
```

The frontend API URL can be configured with:

```text
VITE_API_BASE_URL=http://localhost:8080
```

## One-command startup

The main entry point for running the full application is:

```bash
make run
```

This starts all required containers and makes the application available at:

```text
http://localhost:3000
```

The “one click” requirement is implemented as a one-command startup using the Makefile.
