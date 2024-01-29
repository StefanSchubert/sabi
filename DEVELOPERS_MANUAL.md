# Developers Manual

The following helps new developers to setup the required local build environment.

## Technology Stack

### Common

* Java 21
* openAPI3 (Swagger) via springdoc-v2
* ARM-Platform (raspberryPis)
* IPv6 DynDNS (at least for the start)

### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product against sabi. However to start with this project involves a

* JSF2.3 based WebClient
* Primefaces 13.x
* Spring-Boot-3

Why JSF and not some current modern framework like angular or VUE? In fact, it took me quite a while to came to a decision here.
The two main reasons for me are:

(Please read also: https://github.com/StefanSchubert/sabi/wiki/09.-Design-Decisions#92-frontend-technology - as I changed my mind a bit on this in between)

* I don't believe that the traffic will be that big, that the server (though a rasperryPi at the beginnig) can't handle it, so that I need to shift the resources (session, logic) rather to the client side.
* Though since 2014-2017 it became rather quiet around the mature JSF technology (at least according the web barometers), I see today more JSF based applications in business life, that needs maintenance than writing new angular ones. So I decided to improve my skills in JSF again.

### Server side
* Spring-Boot 3 Application
* REST
* JPA 2.x (Eclipselink instead of Hibernate)
* jUnit
* MariaDB 10.x

----

## Architectural Notes

Before starting to contribute on this project, make sure that you have read the architectural notes in the [wiki](https://github.com/StefanSchubert/sabi/wiki) which
are based upon arc42 templates.

### Current Code-Quality State

[![CodeQL](https://github.com/StefanSchubert/sabi/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/StefanSchubert/sabi/actions/workflows/codeql-analysis.yml)
[![Java CI with Maven](https://github.com/StefanSchubert/sabi/actions/workflows/maven.yml/badge.svg)](https://github.com/StefanSchubert/sabi/actions/workflows/maven.yml)

----

## Setting up the development environment

With a look at [Building-Block View](https://github.com/StefanSchubert/sabi/wiki/05.-Building-Block-View) from the arc42 documentation you see that sabi is not just a single app but consists of using several modules that are configured to work together. Your environment might look different, depending on which module you intent to work on, but the good news is that we already rely on docker here, which makes the setup for you so much easier.

### Preconditions

* You have a JDK21 and current Maven installed
* You have docker installed on your machine, and you know docker usage fairly well.
* As we are using the org.owasp dependency check you will required to register yourself at https://nvd.nist.gov/
  to get an API-KEY, which you can store in the properties section of your local settings.xml like this:

        <properties>
            <nvd.api.key>YOUR_API_KEY</nvd.api.key>
        </properties>

#### Prepare your local docker environment

Do some maven builds in the following order:
1. mvn package on captcha-light
2. mvn install on sabi-boundary
3. mvn package on sabi-server

Next you run this script:

  devops/sabi_docker_sdk/copyjars.sh

Which makes sure that the docker container gets the required jar module assets. 

### DEV-Environment for a frontend engineer working on sabi-webclient module

That's fairly easy. Go to devops/sabi_docker_sdk have a look into the docker-compose.yml.
You may need to do some port tweaking in case it collides with your personal environment, but all you need to do is a

`docker-compose up -d`

and there you are. Give it 2 min for the services to come up and then your you can access 

* the swagger-APIs here:
  * Captcha: http://localhost:8081/captcha/swagger-ui.html
  * Sabi-Service: http://localhost:8080/sabi/swagger-ui.html
  
* the Faked SMTP Server to Catch all Registration-Workflow Emails:
  * http://localhost:5080
  
* the database (though you won't need it) via your favorite DB-Browser here:
  * User: sabiapp / Password: sabi123  
  * jdbc:mariadb://localhost:3306/sabi
  
Now you can pick up your favourite IDE (as the module is maven based), and
launch 
`sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/SpringPrimeFacesApplication.java`

After that you can access the current's frontend stage at:

  http://localhost:8088/index.xhtml

You may login with the following test user:

    sabi@bluewhale.de/clibanarius

or register a new one. 

### Dev-Environment for a backend engineer to work on sabi-server.

#### Preparations

Do all the steps above which are required as frontend-engineer.

Understand when working on sabi-boundary than you need to do a maven install 
so that maven builds of sabi-server and sabi-webclient can fetch your changes. 

#### Database-Evolution

Schema evolution follows the flyway approach, as all changes will be rolled out to production only through flyway.
Meaning pre-existing scripts in

`sabi-database/src/main/resources/db/migration/version*`

are immutable to you, you require to add a new one for any changes.

##### Add the following profile to your maven settings.xml

To ease the work with flyway you shoud add the following snippet to your maven profile or settings.xml:

```
   <profiles>
    <profile>
        <!-- Used for local environment development to setup the sabi database -->
        <id>db_local_secrets_sabi</id>
        <activation>
            <activeByDefault>false</activeByDefault>
        </activation>
        <properties>
                <mariadb.schema>sabi</mariadb.schema>
                <mariadb.server.name>localhost</mariadb.server.name>
                <mariadb.server.port>3306</mariadb.server.port>
                <mariadb.server.app.username>sabiapp</mariadb.server.app.username>
                <mariadb.server.app.password>sabi123</mariadb.server.app.password>
        </properties>
    </profile>
   </profiles>
```

##### Used maven goals on module sabi_database

| Maven command | Purpose  |
| ------------- |-------------| 
| mvn clean install -P configure_flyway db_local_secrets_sabi | Setup/Reinstall the database schema | 
| mvn flyway:migrate -P configure_flyway db_local_secrets_sabi | Apply Schema changes      | 
| mvn flyway:validate -P configure_flyway db_local_secrets_sabi | Validate schema      | 
| mvn flyway:repair -P configure_flyway db_local_secrets_sabi | Repair flyway metadata       | 


#### Working on sabi-server

Nothing very special here, except two things you need to know:

##### Junit testing.

Introducing new functionalities require addition of junit test. 

As we are using eclipselink you must add a specific javaagent, when running your tests. See section prepare your IDE below for it:


##### Preparing your productive and IDE environment

Because of eclipselink we are using weaving at runtime which requires the following vm option:

```
-javaagent:/PATH_TO_YOUR_MAVEN_REPOSITORY/org/springframework/spring-instrument/6.1.2/spring-instrument-6.1.2.jar
```

You will need the agent for the springboot application run-config in your IDE as well as VM parameter for you 
test runner config. **Please verify** that you use the correct version as derived from pom.xml dependency tree, as you may require
to adopt the javaagent string above to suite to your version.
You may also need to adopt your setting if that version in the pom changes because of patch management.


