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

## Release Planning

### First Release

Being able to collect the basic values and to display them in a rather static reporting manner.

### Second Release

Offering some query mechanism to do some analysis. And maybe a set of some standard reports. If possible we might acquire support from one of the big BI vendors.

### Third Release

I have some siblings in my nano reef tank and need to do some gardening. But where to with the siblings? Where ar all the other aquarists and is there someone nearby? They are organized in standard internet forums, but what if there are someone near but not located in the same forum I use (more or less frequently). If they all could be motivated using sabi it should be possible to introduce them to each other for nearby support purposes.

## Technology Stack

* JDK 1.8

### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product
against sabi. However to start with this project involves a

* JSF based WebClient

### Server side
* Spring 4
* REST
* JPA 2.x
* Tomcat 7 (JDK8)
* MariaDB 10.x

----

## Setting up the development environment

### Database

* Install a local MariaDB (latest version should do it)
* Create a DB called sabi and and a user sabi app with permissions for localhost.
* Use the password as specified by the database module pom.

### Setting up the tomact 7

* Install the 7.x version
* That's it ;-) ... Almost ... I encountered some problems with the deployment of the artefact through intelliJ
  Make and Build had problems to accept the target bytecode release 1.8. As intermediate solution I deleted those
  before launch action and choosed the maven package instead. Though even this should be obsolete in an
  exploded deployment manner and having compiled it all before.


## Used maven goals

### Reinstall the database schema
mvn clean install -P db_setup sabi_database


