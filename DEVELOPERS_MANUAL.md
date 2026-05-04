# Developers Manual

The following helps new developers to set up the required local build environment.

## Technology Stack

### Common

* Java 25
* openAPI3 (Swagger) via springdoc-v2
* ARM-Platform (raspberryPis)
* IPv6 DynDNS (at least for the start)

### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product against sabi. 
However, to start with this project involves a

* JSF2.3 based WebClient
* Primefaces 15.x
* Spring-Boot-4

Why JSF and not some current modern framework like angular or VUE? In fact, it took me quite a while to came to a decision here.
The two main reasons for me are:

(Please read also: https://github.com/StefanSchubert/sabi/wiki/09.-Design-Decisions#92-frontend-technology - as I changed my mind a bit on this in between)

* I don't believe that the traffic will be that big, that the server (though a raspberryPi at the beginning) can't handle it, so that I need to shift the resources (session, logic) rather to the client side.
* Though since 2014-2017 it became rather quiet around the mature JSF technology (at least according the web barometers), I see today more JSF based applications in business life, that needs maintenance than writing new angular ones. So I decided to improve my skills in JSF again.

### Server side
* Spring-Boot 4 Application
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

* You have a JDK25 and current Maven installed
* You have docker installed on your machine, and you know docker usage fairly well.
* As we are using the org.owasp dependency check you will require to register yourself at https://nvd.nist.gov/
  to get an API-KEY, which you can store in the properties section of your local settings.xml like this:

        <properties>
            <nvd.api.key>YOUR_API_KEY</nvd.api.key>
        </properties>

* If you work with IntelliJ: Install the "Jakarta Server Faces (JSF)" Plugin

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

#### JSF/Facelets — No Hot-Reload

**XHTML changes always require a full server restart.** There is no hot-reload for Facelets
in JoinFaces/Spring Boot:

- Even with `joinfaces.jsf.project-stage: development`, compiled Facelets views are **cached** at
  startup and never reloaded from the classpath.
- Copying changed XHTML files into `target/classes/` has **no effect** — the embedded Tomcat reads
  from the classpath that was scanned at boot time.
- **Symptom of a stale view**: auto-generated JSF component IDs (e.g. `j_idt30`) instead of
  explicitly assigned IDs (e.g. `tankSelector`), or AJAX partial-updates silently failing because
  the target component ID doesn't exist in the DOM.

**Rule: After every XHTML change → restart the server → only then test.**

#### Spring Security + JSF: Forward vs. Redirect

**Never use `successForwardUrl()` in combination with JSF.**

A servlet forward after successful authentication carries the login form's
`jakarta.faces.ViewState` POST parameter into the target page. JSF then tries to apply the login
page's saved state onto the target page's component tree, which leads to an
`ArrayIndexOutOfBoundsException` in `UIComponentBase.restoreState`.

**Always use `defaultSuccessUrl(url, true)`** — this issues an HTTP 302 redirect so that the
browser makes a fresh GET request and JSF builds a new view without any state conflict.

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

To ease the work with flyway you should add the following snippet to your maven profile or settings.xml:

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

##### JUnit testing

Introducing new functionalities requires the addition of JUnit tests.

###### EclipseLink javaagent

Because we use EclipseLink (not Hibernate), JPA weaving requires a Spring instrument javaagent
at runtime. The pom.xml already configures the Maven Surefire plugin to pass this agent
automatically when you run `mvn test` — so **`mvn test` works out-of-the-box without any manual
configuration**.

However, when running tests directly from your **IDE** (IntelliJ run config or test runner), you
must add the javaagent as a VM option yourself:

```
-javaagent:${M2_REPO}/org/springframework/spring-instrument/${spring-framework.version}/spring-instrument-${spring-framework.version}.jar
```

Replace `${M2_REPO}` with your local Maven repository path (e.g. `~/.m2/repository`) and
`${spring-framework.version}` with the actual version derived from the Spring Boot parent POM.

**How to find the current version:**
```bash
cd sabi-server && mvn help:evaluate -Dexpression=spring-framework.version -q -DforceStdout
```
> As of Spring Boot 4.0.5 this resolves to **`7.0.6`**, giving:
> ```
> -javaagent:/Users/YOU/.m2/repository/org/springframework/spring-instrument/7.0.6/spring-instrument-7.0.6.jar
> ```

You need this agent both for the Spring Boot application run-config and for the IDE test runner
config. Always verify the version after updating the Spring Boot parent in `pom.xml`.

###### Test Users and FK Constraints in Repository Tests

**NEVER use hardcoded user IDs** in repository integration tests (e.g. `proposerUserId = 42`).

Sabi's integration tests run against a real MariaDB Testcontainer that starts empty. Flyway
migrations populate schema and reference data, but they do **not** create application users.
Any `INSERT` that references a non-existent `users.id` via a foreign key will fail with
`SQLIntegrityConstraintViolationException`.

**Rule: Always create the required users programmatically in `@BeforeEach` via `UserRepository`.**

```java
@Autowired
private UserRepository userRepository;

@Autowired  // needed to bypass EclipseLink L1 cache
@PersistenceContext(unitName = "sabi")
private EntityManager em;

private Long userId1;

@BeforeEach
public void setUp() {
    userId1 = createUser("mytest1@test.de", "mytestuser1");
    em.flush();
}

private Long createUser(String email, String alias) {
    UserEntity u = new UserEntity();
    u.setEmail(email);
    u.setAlias(alias);
    // … set other mandatory fields (language, validated = true, etc.) …
    return userRepository.saveAndFlush(u).getId();
}
```

###### EclipseLink L1 Cache in Repository Tests

EclipseLink maintains an **identity map (L1 cache)** per `EntityManager`. Unlike Hibernate,
`saveAndFlush()` writes to the DB but JPQL queries may still read from the cache. Symptoms:
- A freshly saved entity is not found by a subsequent `findBy` query
- Query results look stale (return old field values)

**Fix: call `em.clear()` after every `saveAndFlush()` in tests** to evict the identity map and
force the next query to hit the database:

```java
private void saveAndClear(SomeEntity entity) {
    someRepository.saveAndFlush(entity);
    em.clear();
}
```

##### Preparing your productive and IDE environment

Because of EclipseLink we are using weaving at runtime which requires the javaagent described
above in *EclipseLink javaagent*. For the Spring Boot application run-config use:

```
-javaagent:/PATH_TO_YOUR_MAVEN_REPOSITORY/org/springframework/spring-instrument/CURRENT_VERSION/spring-instrument-CURRENT_VERSION.jar
```

Determine `CURRENT_VERSION` as shown above. The version tracks the `spring-framework.version`
property managed by the Spring Boot parent — it changes with every Spring Boot upgrade.


