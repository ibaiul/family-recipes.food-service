[![CI](https://github.com/ibaiul/family-recipes.food-service/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/ibaiul/family-recipes.food-service/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=family-recipes.food-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=family-recipes.food-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=family-recipes.food-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=family-recipes.food-service)
[![Known Vulnerabilities](https://snyk.io/test/github/ibaiul/family-recipes.food-service/badge.svg)](https://snyk.io/test/github/ibaiul/family-recipes.food-service)

# FOOD SERVICE

Service in charge of managing and querying food entities, part of the Family Recipes application.

## Architecture

![Architecture](img/family-recipes-architecture.png "Architecture")

### Microservices
- Family service
  - Manage family units, members and permissions
  - Authenticate users
- Food service
  - Manage recipes, ingredients and ingredient properties
  - Query food entities
- Meal service
  - Record meals eaten by family members
  - Display meals eaten, stats, reminders

### Running locally

```bash
# Build read model
./gradlew clean food-service-read-model:bootJar
docker build -t ibaiul/family-recipes:food-service-read-model-local --build-arg MODULE_NAME=read-model -f Dockerfile .
# Build write model
./gradlew clean food-service-write-model:bootJar
docker build -t ibaiul/family-recipes:food-service-write-model-local --build-arg MODULE_NAME=write-model -f Dockerfile .
# Generate certificates for Zookeeper server and clients
./generate-certs.sh
# Run services
docker-compose -f docker/docker-compose-local.yml up -d
```
