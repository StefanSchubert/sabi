# Docker Compose Environment

## Notice for Developers on an ARM system like MacBooks with M1 processor

### Using the x86 arch layout 

#### Enable Cross platform building

You require to enable multiarch build support by registering a special builder

    docker buildx create --name mybuilder
    docker buildx use mybuilder

#### Using x86 images on your mac

Forget docker desktop (at leat at current state 6/2022), use colima instead

    brew install colima

and use such a script to start colima
cat bin/launchColimaDockerEngine.sh

    #!/usr/bin/env sh
    colima start --arch x86_64 --cpu 4 --memory 8 --runtime docker --profile x86
    echo "You my switch context with 'docker context list' and 'docker context use ..'"
    echo "Distingish socket for IntelliJ Docker Run Config: 'colima status x86'"
    echo "End colima VM with: 'colima stop x86'"

### Using the ARM Architecture

Continue using docker desktop is just fine, however you need to use the adopted 
Docker and compose files:

    docker compose --file=docker-compose-arm.yml [up -d | start | stop | down ]

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
