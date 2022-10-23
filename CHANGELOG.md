# History of changes (since 5/2022)

## Release 1.0.2

### New Features
* SABI-116: Added more (time-view) measurment report charts 
* SABI-130: PastPlagues table includes the duration of observed plage

### Technical Maintenance
* Patchmanagement

## Release 1.0.1

### New Features
* CSS Tuning of Plague Center
* SABI-117: Fixed problem with flyway maven plugin since migration to v9.x
* SABI-124: Healtheck includes backend components also. Unfortunately Uptrends freeplan supports only one endpoint.

### Technical Maintenance
* Infrastrucure: QAed Monitoring solution which has been integrated in ansible deplyoments

## Release 1.0.0

### New Features
* SABI-115: added Plague Center

### Technical Maintenance 
* Patchmanagement

## Release 0.9.3

### New Features

* SABI-75: Added APIKey based measurement reporting for temperatures for IoT measurement devices. 

## Release 0.9.2

### Technical Maintenance 
* WebClient Module:
  * Migrated to SpringBoot 3.x
    * Java 17 (rollout requires to switch PI to ARM64 OS)
    * Primefaces 11 (Joinfaces 5)
* SDK:
  * Added support for ARM64 build-architecture (for being able to develop with apples MacBook M1 and docker)
