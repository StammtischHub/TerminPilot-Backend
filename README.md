# TerminPilot-Backend

The Spring Boot backend for TerminPilot.

## Development

After cloning the repository, follow the instructions below to get started with developing the backend.

### Requirements

The following requirements must be met to be able to setup and run the backend:

1. Java 24+
2. MySQL 9.0+
3. Gradle 9.0+

### Local Database Setup

First, you need to set up a local MySQL database for development. You can do this by following the instructions below:

```bash
$ mysql -u root -p
> CREATE DATABASE TerminPilot;
> CREATE USER 'TerminPilotUser'@'localhost' IDENTIFIED BY '{DATABASE_USERNAME_PASSWORD}';
> GRANT ALL PRIVILEGES ON TerminPilot.* TO 'TerminPilotUser'@'localhost';
```

After setting up the database, you need to copy the `application-local.example.yml` file to `application-local.yml` and update the database credentials.

### Useful Commands

#### Building the Backend

```bash
$ ./gradlew build
```

#### Running the Backend

> [!TIP]  
> When you try to run the backend via your IDE, make sure to set the active profile to `local` to use the local database configuration.

```bash
$ ./gradlew bootRun --args='--spring.profiles.active=local'
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

## Making a release

> [!IMPORTANT]  
> Only Maintainers and Admins can make new releases.

If you want to make a release, you can run the `Release` workflow on `main` in GitHub Actions. This will create a new tag, upload the JAR file to the release assets and publish a new Docker image to Docker Hub with the same version as the tag.

Running the workflow will also update the `CHANGELOG.md` with the new version and features, fixes, etc.