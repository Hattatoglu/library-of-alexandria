maven:
	mvn clean install -DSkipTests

server-postgres:
	docker compose -f docker/server/postgres/server-postgres-docker-compose.yaml -p libofalex up -d

server-redis:
	docker compose -f docker/server/redis/server-redis-docker-compose.yaml -p libofalex up -d

server-insight:
	docker compose -f docker/server/redis/server-insight-docker-compose.yaml -p libofalex up -d

server-grafana:
	docker compose -f docker/server/server-grafana/server-grafana-docker-compose.yaml -p libofalex up -d

server-prometheus:
	docker compose -f docker/server/server-prometheus/server-prometheus-docker-compose.yaml -p libofalex up -d

server-adminer:
	docker compose -f docker/server/postgres/server-adminer-docker-compose.yaml -p libofalex up -d

build-service-auth: maven
	docker build -f docker/service/service-auth/Dockerfile -t service-auth:latest .

service-auth:
	docker compose -f docker/service/service-auth/service-auth-docker-compose.yaml -p libofalex up -d

build-server-gateway: maven
	docker build -f docker/server/server-gateway/Dockerfile -t server-gateway:latest .

server-gateway:
	docker compose -f docker/server/server-gateway/server-gateway-docker-compose.yaml -p libofalex up -d

server-kafka:
	docker compose -f docker/server/server-kafka/server-kafka-docker-compose.yaml -p libofalex up -d

server-kafka-ui:
	docker compose -f docker/server/server-kafka/server-kafka-ui-docker-compose.yaml -p libofalex up -d

build-service-catalog: maven
	docker build -f docker/service/service-catalog/Dockerfile -t service-catalog:latest .

service-catalog:
	docker compose -f docker/service/service-catalog/service-catalog-docker-compose.yaml -p libofalex up -d
