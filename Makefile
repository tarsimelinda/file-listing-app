NETWORK=file-listing-net

DB_CONTAINER=file-listing-db
BACKEND_CONTAINER=file-listing-backend
FRONTEND_CONTAINER=file-listing-frontend

BACKEND_IMAGE=file-listing-backend
FRONTEND_IMAGE=file-listing-frontend

DB_NAME=filedb
DB_USER=fileuser
DB_PASSWORD=filepass

PROJECT_DIR := $(shell pwd -W 2>/dev/null || pwd)
RUN_USER := $(shell whoami 2>/dev/null || echo unknown)
RUN_UID := $(shell id -u 2>/dev/null || echo unknown)
RUN_GID := $(shell id -g 2>/dev/null || echo unknown)

.PHONY: run network db wait-db build-backend backend build-frontend frontend stop clean ps logs

run: network db wait-db build-backend backend build-frontend frontend
	@echo "Application is running:"
	@echo "Frontend: http://localhost:3000"
	@echo "Backend:  http://localhost:8080"
	@echo "Swagger:  http://localhost:8080/swagger-ui.html"

network:
	@podman network exists $(NETWORK) || podman network create $(NETWORK)

db:
	podman run -d --replace \
		--name $(DB_CONTAINER) \
		--network $(NETWORK) \
		-e POSTGRES_DB=$(DB_NAME) \
		-e POSTGRES_USER=$(DB_USER) \
		-e POSTGRES_PASSWORD=$(DB_PASSWORD) \
		-p 5433:5432 \
		postgres:16

wait-db:
	@echo "Waiting for database..."
	@for i in $$(seq 1 30); do \
		if podman exec $(DB_CONTAINER) pg_isready -U $(DB_USER) -d $(DB_NAME) >/dev/null 2>&1; then \
			echo "Database is ready"; \
			exit 0; \
		fi; \
		sleep 1; \
	done; \
	echo "Database did not become ready in time"; \
	exit 1

build-backend:
	podman build -t $(BACKEND_IMAGE) ./backend

backend:
	podman run -d --replace \
		--name $(BACKEND_CONTAINER) \
		--network $(NETWORK) \
		-p 8080:8080 \
		-v "$(PROJECT_DIR)/input:/input" \
		-e APP_RUN_USER="$(RUN_USER)" \
		-e APP_RUN_UID="$(RUN_UID)" \
		-e APP_RUN_GID="$(RUN_GID)" \
		-e SPRING_DATASOURCE_URL=jdbc:postgresql://$(DB_CONTAINER):5432/$(DB_NAME) \
		-e SPRING_DATASOURCE_USERNAME=$(DB_USER) \
		-e SPRING_DATASOURCE_PASSWORD=$(DB_PASSWORD) \
		$(BACKEND_IMAGE)

build-frontend:
	podman build -t $(FRONTEND_IMAGE) ./frontend

frontend:
	podman run -d --replace \
		--name $(FRONTEND_CONTAINER) \
		--network $(NETWORK) \
		-p 3000:80 \
		$(FRONTEND_IMAGE)

stop:
	-podman stop $(FRONTEND_CONTAINER) $(BACKEND_CONTAINER) $(DB_CONTAINER)
	-podman rm $(FRONTEND_CONTAINER) $(BACKEND_CONTAINER) $(DB_CONTAINER)

clean: stop
	-podman rmi $(FRONTEND_IMAGE) $(BACKEND_IMAGE)

ps:
	podman ps

logs:
	@echo "Backend logs:"
	@podman logs $(BACKEND_CONTAINER)
	@echo ""
	@echo "Frontend logs:"
	@podman logs $(FRONTEND_CONTAINER)
	@echo ""
	@echo "Database logs:"
	@podman logs $(DB_CONTAINER)