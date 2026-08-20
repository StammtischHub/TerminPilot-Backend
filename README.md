# TerminPilot-Backend

The Spring Boot backend for TerminPilot.

## Development

After cloning the repository, follow the instructions below to get started with developing the backend.

### Requirements

The following requirements must be met to be able to set up and run the backend:

1. Java 24+
2. MySQL 9.0+ (or Docker for the containerized database setup)
3. Gradle 9.0+
4. A GitHub account with access to the [TerminPilot-API-Spec](https://github.com/StammtischHub/TerminPilot-API-Spec) package (read:packages scope)

### Environment Variables

Copy the `.env.example` file to `.env` and fill in the required values:

```bash
cp .env.example .env
```

The `.env` file is used both by the Makefile (for Docker operations) and by the Gradle build (for downloading the OpenAPI spec from GitHub Packages).

| Variable                     | Description                                                                                   |
|------------------------------|-----------------------------------------------------------------------------------------------|
| `LOCAL_DB_NAME`              | Name of your local MySQL database                                                             |
| `LOCAL_DB_USER`              | Username of your local MySQL user                                                             |
| `LOCAL_DB_USER_PASSWORD`     | Password of your local MySQL user                                                             |
| `DOCKER_MYSQL_PORT`          | Host port for the Docker MySQL container (default: `3366`)                                    |
| `DOCKER_DB_NAME`             | Database name inside the Docker container (default: `TerminPilot`)                            |
| `DOCKER_DB_USER`             | MySQL user inside the Docker container                                                        |
| `DOCKER_DB_USER_PASSWORD`    | Password for the Docker MySQL user                                                            |
| `DOCKER_DB_ROOT_PASSWORD`    | Root password for the Docker MySQL container                                                  |
| `google.oauth.client-id`     | Google Cloud Project ID for OAuth Flow                                                        |
| `google.oauth.client-secret` | Secret to log in the Google Cloud Project for OAuth Flow (Redirecting)                        |
| `google.oauth.redirect-uri`  | Google OAuth re-entry URI (http://localhost:8080/api/google/callback)                         |
| `GITHUB_ACTOR`               | Your GitHub username — required to download the API spec from GitHub Packages                 |
| `GITHUB_TOKEN`               | A GitHub Personal Access Token with `read:packages` scope — required to download the API spec |

> [!NOTE]
> `GITHUB_ACTOR` and `GITHUB_TOKEN` are required at **build time** because the OpenAPI spec is published as a GitHub Package and pulled automatically during the build.
> The `google.oauth`-vars are required aswell, otherwise the project won't build. It is important, that the `redirect-uri` is set correct in the Google-Cloud-Project!

### OpenAPI Spec & Code Generation

The backend uses the **TerminPilot-API-Spec** — a versioned OpenAPI specification published as a GitHub Package from the [TerminPilot-API-Spec repository](https://github.com/StammtischHub/TerminPilot-API-Spec).

The spec is downloaded automatically during the build and used by the [OpenAPI Generator](https://openapi-generator.tech/) (`org.openapi.generator` Gradle plugin) to generate Kotlin interfaces and model classes. Generated code is placed in `build/generated/src/main/kotlin/` and included as a source set.

```
build/
└── api-spec/
│   └── openapi.yaml          ← downloaded spec
└── generated/
    └── src/main/kotlin/
        ├── de/stammtischHub/terminPilot/api/generated/   ← generated API interfaces
        └── de/stammtischHub/terminPilot/model/generated/ ← generated model classes
```

The code generation runs automatically before `compileKotlin`, so a plain `./gradlew build` is sufficient.

### Swagger UI

The interactive API documentation (Swagger UI) is powered by [springdoc-openapi](https://springdoc.org/) and served directly by the Spring Boot application.

> [!IMPORTANT]
> Swagger UI is **disabled by default** in the base `application.yml` and only enabled for the `local` and `docker` profiles.

Once the application is running with one of those profiles, the Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

The raw OpenAPI spec (JSON) is served at:

```
http://localhost:8080/v3/api-docs
```

### Local Database Setup (Manual)

If you prefer to run a local MySQL instance directly instead of using Docker, create the database and user manually:

```bash
$ mysql -u root -p
> CREATE DATABASE TerminPilot;
> CREATE USER 'TerminPilotUser'@'localhost' IDENTIFIED BY '{YOUR_PASSWORD}';
> GRANT ALL PRIVILEGES ON TerminPilot.* TO 'TerminPilotUser'@'localhost';
```

Then set the corresponding values in your `.env`:

```env
LOCAL_DB_NAME=TerminPilot
LOCAL_DB_USER=TerminPilotUser
LOCAL_DB_USER_PASSWORD=YOUR_PASSWORD
```

The `application-local.yml` profile reads these values and connects to your local MySQL on port `3306`.

### Docker Setup (Recommended)

The repository includes a `docker-compose.yml` that starts a MySQL 9.0 container and a `Makefile` with convenient shortcuts. All Docker commands read their configuration from the `.env` file automatically.

Start the database container:

```bash
make up
```

Then run the backend against the Docker database:

```bash
make run-docker
```

#### Available Makefile Targets

| Target | Description |
|---|---|
| `make up` | Start the MySQL container (data volume is preserved) |
| `make down` | Stop the MySQL container (data volume is preserved) |
| `make down-volume` | Stop the container **and delete all data** |
| `make restart` | Restart the MySQL container |
| `make logs` | Follow the MySQL container logs |
| `make shell-user` | Open a MySQL shell as the configured user |
| `make shell-root` | Open a MySQL shell as root |
| `make backup` | Create a SQL dump in `backups/docker-mysql/` |
| `make restore FILE=<file>` | Restore a dump from a `.sql` file |
| `make status` | Show the status of all running containers |
| `make run-local` | Run the backend with the `local` profile (manual MySQL) |
| `make run-docker` | Start the Docker DB and run the backend with the `docker` profile |
| `make build` | Write dependencies to lock files and build the project |

### Spring Profiles

The backend uses Spring profiles to manage environment-specific configuration:

| Profile | Database | Swagger UI | Use case                                           |
|---|---|---|----------------------------------------------------|
| _(none / default)_ | — | ❌ | Base config only, no DB, probably not even working |
| `local` | Local MySQL on port `3306` | ✅ | Local development with manual MySQL                |
| `docker` | Docker MySQL on `DOCKER_MYSQL_PORT` | ✅ | Local development with Docker MySQL                |

### Useful Commands

#### Building the Backend

```bash
$ ./gradlew build
```

#### Running the Backend

> [!TIP]
> When running the backend via your IDE, make sure to set the active profile to `local` or `docker` to connect to a database and enable Swagger UI.

```bash
# With a local MySQL instance
$ ./gradlew bootRun --args='--spring.profiles.active=local'

# With Docker MySQL (start the container first with 'make up')
$ ./gradlew bootRun --args='--spring.profiles.active=docker'

# Alternatively, use the Makefile shortcuts
$ make run-local
$ make run-docker
```

#### Running Tests

```bash
$ ./gradlew test
```

#### Kotlin Linter

To check the code style:

```bash
$ ./gradlew ktlintCheck
```

To format the code:

```bash
$ ./gradlew ktlintFormat
```

## Making a Release

> [!IMPORTANT]
> Only Maintainers and Admins can make new releases.

If you want to make a release, you can run the `Release` workflow on `main` in GitHub Actions. This will create a new tag, upload the JAR file to the release assets and publish a new Docker image to Docker Hub with the same version as the tag.

Running the workflow will also update the `CHANGELOG.md` with the new version and features, fixes, etc.
