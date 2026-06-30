maven:
	mvn clean install -DSkipTests

server-postgres:
	docker compose -f docker/server/postgres/server-postgres-docker-compose.yaml -p libofalex up -d

server-adminer:
	docker compose -f docker/server/postgres/server-adminer-docker-compose.yaml -p libofalex up -d

build-service-auth: maven
	docker build -f docker/service/service-auth/Dockerfile -t service-auth:latest .

service-auth:
	docker compose -f docker/service/service-auth/service-auth-docker-compose.yaml -p libofalex up -d