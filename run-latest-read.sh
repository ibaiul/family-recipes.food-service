#!/bin/bash
set -e

docker-compose -f docker/docker-compose-local.yml stop read-model
docker rm food-service-read-model -f
./gradlew clean food-service-read-model:bootJar
docker build -t ibaiul/family-recipes:food-service-read-model-local --build-arg MODULE_NAME=read-model -f Dockerfile .
docker-compose -f docker/docker-compose-local.yml up -d
docker logs food-service-read-model -f
