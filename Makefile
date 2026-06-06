include .env
export

# Makefile – Shortcuts für häufige Docker-Operationen

.PHONY: up down down-volume restart logs shell-user shell-root backup restore status run-local run-docker

## MySQL starten (Volume bleibt erhalten)
up:
	docker compose up -d

## MySQL stoppen (Volume bleibt erhalten!)
down:
	docker compose down

## MySQL stoppen UND Volume löschen (alle Daten weg!)
down-volume:
	docker compose down -v

## Container neu starten (z.B. nach Konfigurationsänderung)
restart:
	docker compose restart mysql

## Logs live verfolgen
logs:
	docker compose logs -f mysql

## MySQL-Shell der DB mit User öffnen
shell-user:
	docker compose exec mysql mysql -u $(DOCKER_DB_USER) -p$(DOCKER_DB_USER_PASSWORD) $(DOCKER_DB_NAME)

## Root-Shell öffnen
shell-root:
	docker compose exec mysql mysql -u root -p$(DOCKER_DB_ROOT_PASSWORD)

## Datenbank-Backup erstellen
backup:
	mkdir -p backups/docker-mysql/
	docker compose exec mysql mysqldump -u root -p$(DOCKER_DB_ROOT_PASSWORD) $(DOCKER_DB_NAME) > backups/docker-mysql/backup_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "Backup erstellt: backup_$(shell date +%Y%m%d_%H%M%S).sql"

## Backup wiederherstellen (Verwendung: make restore FILE=backup_20240101.sql)
restore:
	docker compose exec -T mysql mysql -u root -prootpassword appdb < $(FILE)

## Status aller Container anzeigen
status:
	docker compose ps

## Spring Boot mit lokalem Docker-MySQL starten
run-local:
	./gradlew bootRun --args='--spring.profiles.active=local'

## Spring Boot mit lokalem Docker-MySQL starten
run-docker:
	docker compose up -d
	./gradlew bootRun --args='--spring.profiles.active=docker'
