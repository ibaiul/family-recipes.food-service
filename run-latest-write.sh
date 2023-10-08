#!/bin/bash
set -e

docker-compose -f docker/docker-compose-local.yml stop write-model1 write-model2
docker rm food-service-write-model1 food-service-write-model2 -f
./gradlew clean food-service-write-model:bootJar
docker build -t ibaiul/family-recipes:food-service-write-model-local --build-arg MODULE_NAME=write-model -f Dockerfile .
docker-compose -f docker/docker-compose-local.yml up -d
docker logs food-service-write-model1 -f
