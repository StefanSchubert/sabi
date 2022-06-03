# Docker Compose Environment

## Notice for Developers on an ARM system like MacBooks with M1 processor

You require to enable multiarch build support by registering a special builder

    docker buildx create --name mybuilder
    docker buildx use mybuilder

## Provided services for frontend development

This docker-compose provides all required backend services:

* database
* captcha-service
* sabi-service
* fakeSMTP-Mailserver

for being able to develop the sabi frontends.

## Additional monitoring services

In Addition, to profile services runtime behavior during development:

* grafana
* prometheus

for monitoring purposes.

To login into grafana use http://localhost:3000 and

    admin/sabimon

as username/password.

To check which targets are currently being scaped be prometheus, check
prometheus.yml or http://localhost:9090/targets 
