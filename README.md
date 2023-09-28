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

## Next steps
#### Tech
- Move application property secrets from Helm secrets (SOPS) to secrets manager in AWS
- Migrate user journey AT to Cucumber 
- Remove Postgres TOAST OID type
  - https://developer.axoniq.io/w/axonframework-and-postgresql-without-toast
  - https://trifork.nl/blog/axon-postgresql-without-toast/
- Publish DTO and DomainEvents into eus.ibai.family-recipes:food-service-contract artifact so that the Android client or other JVM clients can include them as a dependency
