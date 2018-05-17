# Seawater Aquarium Business Intelligence (sabi)

...aims to gain insights from aquarium hobbyist for aquarium hobbyist according seawater measures.

## Vision I

In seawater forums, wikis, books we got advice on the regular values (max,min) of the important mineral levels and so on.
Some things we fully understand, while on others we have just a lot of guesses about the impact, but often it stays a guess, as the complete system is very complex.
I was wondering, if we will be able to gain some more insights if we start to share our measurement data and placing some business intelligence style reporting on top of it.
This should enable us to to answer some questions like:

* How often do all measure the KH-Value, when not using the Balling method?
* Is there a thing in common when Alveoproa dies (are there similar PO4 levels)?

There must be quite a lot of interesting questions, especially in the field of aquaristic forensics.

So this is the project to build a platform, which helps to answer them.

## Vision 2.0 (in the next 10 years)

In the first stage of the project (see Vision I) we enabled the community to collect and share data.
We then added BI concepts on top for being able to explore the data, mining for new
insights driven by human curiosity. 

The next step adds KI concepts to sabi. Imagine the following scenario: 
You have a cyano bacterium plague in your tank and you are planning to add a new fish
or fiddling around with increasing the carbonate level. Sabis KI might advise you that your
 plan will probably prolong the plague.
  
For being able to do so, sabis KI will compare your tank parameters and recent history 
with the data of other users tanks who have done something similar to anticipate the outcome.
 
The KI challenge here is, that if the human provided data on a given problem context is bad for
 some reasons (e.g. missing of relevant parameters, inaccurate timelines and so on), then
 the KI starts to provide advises that will lead to false treatments. So if users starts to reports
 that advices were not successful the KI must revise the original training set and do some
  recalibration by its own.


## Release Planning

### First Release

Being able to collect the basic values and to display them in a rather static reporting manner.

### Second Release

Offering some query mechanism to do some analysis. And maybe a set of some standard reports. If possible we might acquire support from one of the big BI vendors.

### Third Release

I have some siblings in my nano reef tank and need to do some gardening. But where to with the siblings? Where ar all the other aquarists and is there someone nearby? They are organized in standard internet forums, but what if there are someone near but not located in the same forum I use (more or less frequently). If they all could be motivated using sabi it should be possible to introduce them to each other for nearby support purposes.

## Technology Stack

### Common

* JDK 1.8
* openAPI (Swagger)
 
### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product
against sabi. However to start with this project involves a

* JSF based WebClient

### Server side
* Spring-Boot-Application
* REST
* JPA 2.x (Eclipselink instead of Hibernate)
* jUnit
* MariaDB 10.x

----

## Setting up the development environment

### Database

* Install a local MariaDB (latest version should do it)
* Create a DB called sabi and and a user sabiapp with permissions for localhost.
```
    CREATE DATABASE sabi;
    GRANT ALL ON sabi.* TO sabiapp@localhost IDENTIFIED by 'sabi123';
    FLUSH PRIVILEGES;
```
* Use the password as specified by the database module pom.

## Add a the following profile to your maven settings.xml
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

## Used maven goals on module sabi_database

| Maven command | Purpose  |
| ------------- |-------------| 
| mvn clean install -P configure_flyway db_local_secrets_sabi | Setup/Reinstall the database schema | 
| mvn flyway:migrate -P configure_flyway db_local_secrets_sabi | Apply Schema changes      | 
| mvn flyway:validate -P configure_flyway db_local_secrets_sabi | Validate schema      | 
| mvn flyway:repair -P configure_flyway db_local_secrets_sabi | Repair flyway metadata       | 


## Preparing your productive and IDE environment

Customize your own server properties and do not checkin for security reasons!
Because of eclipselink we are using weaving at runtime which required the following vm 
option:

```
-javaagent:/PATH_TO_YOUR_MAVEN_REPOSITORY/org/springframework/spring-instrument/4.3.13.RELEASE/spring-instrument-4.3.13.RELEASE.jar
```

You will need the agent for the springboot application run-config in your IDE
as well as VM parameter for you test runner config.

## Architectural Notes

Before starting to contribute on this project, make sure that you have read the architectural notes in the [wiki](https://github.com/StefanSchubert/sabi/wiki) which
are based upon arc42 templates. 

---

### REST-API Doc
As we are using swagger, you will find the API doc after starting the application here:
* http://localhost:8080/sabi/swagger-ui.html


### Testing of a successfully deployed backend:
 
I use postman to test the login REST API (or the REST client from IntelliJ)


 
