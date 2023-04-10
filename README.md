# Welcome to Open Science at the SABI-Project - Seawater Aquarium Business Intelligence

This is a semi-scientific (or open-science) project that aims to gain insights
 from aquarium hobbyist for aquarium hobbyist according seawater ecosystems.

[![CodeQL](https://github.com/StefanSchubert/sabi/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/StefanSchubert/sabi/actions/workflows/codeql-analysis.yml)
[![Java CI with Maven](https://github.com/StefanSchubert/sabi/actions/workflows/maven.yml/badge.svg)](https://github.com/StefanSchubert/sabi/actions/workflows/maven.yml)

### Notice on the red code analytic status: 
* Code-QL due to everlasting timeouts by github to downloading CVEs databases.
* Maven build: Due to currently including milestone releases of micrometer.

## Latest project news:
| Date          | News                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 09th Apr 2023 | Hotfix Release (i18n issues)                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 18th Dez 2022 | Feature Release (Reminder Service) / Technical Migration to Spring-Boot 3  / Marks Sabi-Version 1.2.0                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 7th Okt 2022  | Released Plague-Center. All required base workflows have been implemented. This marks version 1.0.0 of sabi.                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| 22nd Jul 2022 | Article about SABI has been published in the journal "KORALLE", Issue Nr. 136. 😊 Available on start of August. Many Thanks goes to Daniel Knop!                                                                                                                                                                                                                                                                                                                                                                                                            |
| 26th Feb 2022 | Offical Softlaunch Day of the project. Starting with a twitter announcement. Going to spread the word in selected forums in the next days, hoping to get some Beta-Testers, Fellow-Coders and to collect valuable feedback.                                                                                                                                                                                                                                                                                                                                 |
| 27th Mai 2021 | I managed to replace the self-signed TLS cert with a let's encrypt based one. This gets us rid of the browsers insecure warning.                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 16th Mai 2021 | **Sneak Preview** available on https://sabi-project.net (Notice: Only available if you have an **IPV6 Internet-Connection** (your mobile with wlan switched off should do it, if your home has only the half internet available). As of the zero budget start we have a self signed TLS resulting in a browsers insecure warning. Still some bugs of course (see issue list), and features left till release 1.0 but it's already usable. In case you decide to create an account...it's already the production environment ;-) i.e. your data will be kept. |

## Vision I

In seawater forums, wikis, books we got advice on the regular values (max,min) of the important mineral levels and so on.
Some things we fully understand, while on others we have just a lot of guesses about the impact, but often it stays a guess, as the complete system is very complex.
I was wondering if we will be able to gain some more insights if we start to share our measurement data and placing some business intelligence style reporting on top of it.
This should enable us to answer some questions like:

* How often do all measure the CaCO3 level when not using the Balling method? Is there a probability of getting a cyano bacteria plague when measuring too less?
* Is there a thing in common when Alveoproa dies (are there similar PO4 levels)?

There must be quite a lot of interesting questions, especially in the field of aquaristic forensics.

So this is the project to build a platform, which helps to answer them.

## Vision 2.0 (in the next 10 years)

In the first stage of the project (see Vision I) we enabled the community to collect and share data.
We then added BI concepts on top for being able to explore the data, mining for new
insights driven by human curiosity. 

The next step adds AI concepts to sabi. Imagine the following scenario: 
You have a cyano bacterium plague in your tank and you are planning to add a new fish
or fiddling around with increasing the carbonate level. Sabis KI might advise you that your
 plan will probably prolong the plague.
  
For being able to do so, sabis AI will compare your tank parameters and recent history 
with the data of other users tanks who have done something similar to anticipate the outcome.
 
The AI challenge here is, that if the human provided data on a given problem context is bad for
 some reasons (e.g. missing of relevant parameters, inaccurate timelines and so on), then
 the AI starts to provide advises that will lead to false treatments. So if users starts to reports
 that advices were not successful the AI must revise the original training set and do some
  recalibration by its own.


## Project Planning

### Stage I

Being able to collect the basic values and to display them in a rather static reporting manner.

### Stage II

Offering some query mechanism to do some analysis. And maybe a set of some standard reports. If possible we might acquire support from one of the big BI vendors.

### Stage III

Document insights gained through this project. If possible try to make forecasts (i.e. take care of that measurement level, if not raised it is likely that ... happen)

### Possible NON-Scientific extensions

I have some siblings in my nano reef tank and need to do some gardening. But where to with the siblings? Where are all the other aquarists and is there someone nearby? They are organized in standard internet forums, but what if there are someone near but not located in the same forum I use (more or less frequently). If they all could be motivated using sabi it should be possible to introduce them to each other for nearby support purposes.

## Release Planning

Just have a look at the [Milestones](https://github.com/StefanSchubert/sabi/milestones?direction=asc&sort=due_date&state=open) from the Issue Board

## Technology Stack

### Common

* Java 17
* openAPI3 (Swagger) via springdoc-v2
* ARM-Platform (raspberryPis)
* IPv6 DynDNS (at least for the start)

### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product against sabi. However to start with this project involves a

* JSF2.3 based WebClient
* Primefaces 12
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

## Setting up the development environment

With a look at [Building-Block View](https://github.com/StefanSchubert/sabi/wiki/05.-Building-Block-View) from the arc42 documentation you see that sabi is not just a single app but consists of using several modules that are configured to work together. Your environment might look different, depending on which module you intent to work on, but the good news is that we already rely on docker here, which makes the setup for you so much easier.

### Preconditions

* You have a JDK17 and current Maven installed
* You have docker installed on your machine, and you know docker usage fairly well.

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

Introducing new functionalities require addition of junit test. Please integrate them such way that they will be picked up along with execution of

`sabi-server/src/test/java/de/bluewhale/sabi/MasterTestSuite.java`

Notice this test class will be also executed via githubs action.
Naturally we expect that changes won't break anything.

As we are using eclipselink you must add a specific javaagent, when running your tests. See section prepare your IDE below for it:


##### Preparing your productive and IDE environment

Because of eclipselink we are using weaving at runtime which requires the following vm option:

```
-javaagent:/PATH_TO_YOUR_MAVEN_REPOSITORY/org/springframework/spring-instrument/6.0.2/spring-instrument-6.0.2.jar
```

You will need the agent for the springboot application run-config in your IDE as well as VM parameter for you 
test runner config. **Please verify** that you use the correct version as derived from pom.xml dependency tree, as you may require
to adopt the javaagent string above to suite to your version.
You may also need to adopt your setting if that version in the pom changes because of patch management.

## Architectural Notes

Before starting to contribute on this project, make sure that you have read the architectural notes in the [wiki](https://github.com/StefanSchubert/sabi/wiki) which
are based upon arc42 templates. 
