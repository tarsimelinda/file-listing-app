NETWORK=file-listing-net

DB_CONTAINER=file-listing-db
BACKEND_CONTAINER_1=file-listing-backend-1
BACKEND_CONTAINER_2=file-listing-backend-2

BACKEND_IMAGE=file-listing-backend

DB_NAME=filedb
DB_USER=fileuser
DB_PASSWORD=filepass

PROJECT_DIR := $(shell pwd -W 2>/dev/null || pwd)

.PHONY: run network db wait-db build-backend backend-1 backend-2 stop clean ps logs

run: network db wait-db build-backend backend-1 backend-2
	@echo "Application is running:"
	@echo "Backend instance 1: http://localhost:8080"
	@echo "Backend instance 2: http://localhost:8081"
	@echo "Swagger instance 1: http://localhost:8080/swagger-ui.html"
	@echo "Swagger instance 2: http://localhost:8081/swagger-ui.html"

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

backend-1:
	podman run -d --replace \
		--name $(BACKEND_CONTAINER_1) \
		--network $(NETWORK) \
		--user appuser1 \
		-p 8080:8080 \
		-v "$(PROJECT_DIR)/input:/input" \
		-e SPRING_DATASOURCE_URL=jdbc:postgresql://$(DB_CONTAINER):5432/$(DB_NAME) \
		-e SPRING_DATASOURCE_USERNAME=$(DB_USER) \
		-e SPRING_DATASOURCE_PASSWORD=$(DB_PASSWORD) \
		$(BACKEND_IMAGE)

backend-2:
	podman run -d --replace \
		--name $(BACKEND_CONTAINER_2) \
		--network $(NETWORK) \
		--user appuser2 \
		-p 8081:8080 \
		-v "$(PROJECT_DIR)/input:/input" \
		-e SPRING_DATASOURCE_URL=jdbc:postgresql://$(DB_CONTAINER):5432/$(DB_NAME) \
		-e SPRING_DATASOURCE_USERNAME=$(DB_USER) \
		-e SPRING_DATASOURCE_PASSWORD=$(DB_PASSWORD) \
		$(BACKEND_IMAGE)

stop:
	-podman stop $(BACKEND_CONTAINER_1) $(BACKEND_CONTAINER_2) $(DB_CONTAINER)
	-podman rm $(BACKEND_CONTAINER_1) $(BACKEND_CONTAINER_2) $(DB_CONTAINER)

clean: stop
	-podman rmi $(BACKEND_IMAGE)

ps:
	podman ps

logs:
	@echo "Backend instance 1 logs:"
	@podman logs $(BACKEND_CONTAINER_1)
	@echo ""
	@echo "Backend instance 2 logs:"
	@podman logs $(BACKEND_CONTAINER_2)
	@echo ""
	@echo "Database logs:"
	@podman logs $(DB_CONTAINER)