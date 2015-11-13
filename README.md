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

### Common

* JDK 1.8

### Client site
As you desire, the server API will be open, so that everyone might develop their own client or interface their existing product
against sabi. However to start with this project involves a

* JSF based WebClient

### Server side
* Spring 4
* REST
* JPA 2.x (Eclipselink)
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


## Architectural Notes

First of all, for this little app the architecture is overblown for sure.
However I would like to make it (almost) right at the beginning and 
liked the idea to have a small blueprint at hand with it, to ease further projects.

DAOs and ServiceLayer. A good introduction about it can be
found e.g.: [here](http://bearprogrammer.com/2012/11/12/dao-repository-and-service-digging-deeper/)

### Clients
Expected to use the REST api, but maybe I intent to offer other interfaces too.
The design need to ease these extensions in the future.

### API Endpoints
This is where the client is talking to, for the REST clients it's all kept in
de.bluewhale.sabi.rest package.
(html endpoint is planed, too)

The Endpoints are responsible to do the i18n for the clients.

### ServiceLayer
Business logic like (validations, business rules, authentication, authorisation, BI and reporting stuff) that
is shared by the different client technologies is being kept in here: de.bluewhale.sabi.services
Though in the beginning all is running on one server, this design eases infrastructure refactorings,
if we want to have the Client API frontends on multiple frontend servers, which will communicate to
the (multiple) backend servers. If this business domain is growing we might introduce a domain cut,
where each domain might run on different servers.

This service layer is thought of containing higher business logic,
meaning it orchestrates the persistent backend operations. Meaning no service on this level 
will get an @Transactional. The methods work or don't.

The ServiceLayer is working completely on TransportObjects. No Entity is allowed to be used within the
ServiceLayer.

#### Exception Handling 
 In case of don't we will introduce a common BusinessException which contains the different reasons as messages, which will be used to signal 
errors to the API-Endpoints which in turn have the responsibility to translate and deliver them back
to the client. 

#### Coming to i18n
The API Layer is responsible to do all the translations. Thinking of 
typical frontend backend machine scenario, I want to keep the traffic between those machines as small
as possible, as transfering all translations to the webserver which in turn delivers one won't do it.

Discarded Solution: Reaching a language context into the backend. This would do it. But I didn't liked 
it. I was thinking about being able on the API-Layer to decide given back a small message (suitable for
client constrains like small displays) or very detailed messages (maybe with solution suggestions).

### DAO-Layer
 
The DOAs are used as repositories, which are responsible to manage all CRUD and other persistence
operations. The intention of this layer to isolate the application from the datalayer, through
which we are allowed to do persistence refactorings (e.g. for performance sake) withing mingling
with the object models used on the client site. Or in other words, we are able to
evolve data and application layer independently. 
So all DAOs will never return an entity, but TransportObjects.
As the transport objects are part of the application model and therefore required by the server
ans maybe the API Endpoints as well, you will find them in
de.bluewhale.sabi.model of the sabi-boundary module.

Our DAO layer is being kept in de.bluewhale.sabi.dao of the sabi-server module.

