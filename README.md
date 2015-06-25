# sabi

Seawater Aquarium Business Intelligence (sabi) aims to gain insights from aquarium hobbyist for aquarium hobbyist according seawater measures.

## Vision

In seawater forums, wikis, books we got advice on the regular values (max,min) of the important mineral levels and so on.
Some things we fully understand, while on others we have just a lot of guesses about the impact, but often it stays a guess, as the complete system is very complex.
I was wondering, if we will be able to gain some more insights if we start to share our measurement data and placing some business intelligence style reporting on top of it.
This should enable us to to answer some questions like:

* How often do all measure the KH-Value, when not using the Balling method?
* Is there a thing in common when Alveoproa dies (are there similar PO4 levels)?

There must be quite a lot of interesting questions, especially in the field of aquaristic forensics.

So this is the project to build a platform, which helps to answer them.

## Technology Stack

### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product
against sabi. However to start with this project involves a

* JSF based WebClient

### Server side
* JEE
* REST
* JPA
* Glassfish
* MariaDB

----

## Setting up the development environment

### Database

* Install a local MariaDB (latest version should do it)
* Create a DB called sabi and and a user sabi app with permissions for localhost.
* Use the password as specified by the database module pom.

### Glasfish
 * Install a glassfish4
 * Browse to the Admin GUI localhost:4848
 * Add a Resources/JDBC/JDBC Connection Pools: "Sabi_DBPool"
   * Resource Type: javax.sql.DataSourc
   * Datasource Classname: org.mariadb.jdbc.MySQLDataSource
   * Properties:
   * User: sabiapp
   * UserName: sabiapp
   * Port 3306
   * Servername: localhost
   * Databasename: sabi
   * URL: jdbc:mysql://localhost:3306/sabi
   * Password: sabiapp

  * Add a Resources/JDBC/JDBC/Resources
  * JNDI-Name: sabi
  * Poolname: "Sabi DBPool"

## IDE
* add sabi-exploded war as deployable to the glassfish


## Used maven goals

### Reinstall the database schema
mvn clean install -P db_setup sabi_database


