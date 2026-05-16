# History of changes (since 5/2022)

## Release 1.3.5 (since 1.2.9)

### Technical Maintenance

* Common Patch-Management (JSpringBoot 4.0.6 / Java 25 / Primefaces 15 / Dependencies)
* Agentic coding support through elaborated instructions for the AI agent, which allows to generate code snippets for the SABI project. This is a major improvement in terms of development speed and efficiency, as it allows developers to quickly generate code snippets for common tasks and functionalities, without having to write them from scratch.
* Introduced Ansible-Vaults to safley commit productive credentials to the repository, which is a major improvement in terms of security and best practices, as it allows to securely store and manage sensitive information such as API keys, database credentials, etc. without exposing them in the codebase.
* Ansible playbook enhancements for SABI project, which allows to automate the deployment and management of the SABI application, including tasks such as provisioning infrastructure, configuring servers, deploying code, etc. This is a major improvement in terms of efficiency and scalability, as it allows to quickly and easily deploy and manage the SABI application in a consistent and repeatable manner.

### Feature
* Ability to record fish inhabitants in tanks
* Ability to record events in tanks (e.g. water change, filter change, etc.)
* Support for AI based consulting by JSON download through their user profile
* Ability tp provide a public "homepage" for each tank, which can be shared with friends and family (without login)
* Ability to record coral inhabitants in tanks
* Introduced OIDC ability for authentication and authorization, which allows to use external identity providers (e.g. Keycloak, Auth0, etc.) and also supports SSO (Single Sign-On) across multiple applications. This is a major improvement in terms of security and user experience, as it allows users to authenticate once and access multiple applications without having to log in again.
* Introduction of the SABI Project Badge
* Aquarium metadata enhancement (ecosystem-type, net volume, etc.)
* UI Design overhaul with Primefaces 15, which allows to use the latest features and improvements of the Primefaces library, such as new components, improved performance, better accessibility, etc. This is a major improvement in terms of user experience and visual appeal, as it allows to create a more modern and responsive UI for the SABI application.
* General UI/UX improvements (e.g. better mobile support, more intuitive navigation, etc.)
* UI support for dark mode

### Fixes

* Datepicker-Rendering in Safari (iOS) fixed
* CI-Pipeline configuration

## Release 1.2.9

### Technical Maintenance

* Common Patch-Management (JSpringBoot 4.0.3 / Java 25 / Primefaces 15 / Dependencies)

### Feature
* Preparation fpr sabi-150: usageterms
* Inactive marked tanks no longer apear in the tank selection of the measurement view

## Release 1.2.8

### Technical Maintenance

* Common Patch-Management (JSpringBoot 3.4.5 / Java 22 / Primefaces 14 / Dependencies)

## Release 1.2.7

### Feature
* SABI-17: i18n of measurement units, parameter and plagues.

## Release 1.2.6

### Enhancements
* SABI-68: Spanish ressource bundles added
** THX to deepl I added French, Italian as well

### Technical Maintenance

* SABI-128 Additional TLS on Backend component (required by aquarium-IoT project)

## Release 1.2.5

### Bugfixes
* HTTP.500 in some cases for returing users with timedout session (sabi-113)

## Release 1.2.4

### Bugfixes
* i18n usage for Measurement threshold info in Measurement-View

## Release 1.2.3

### New Features
* SABI-146: Support to add freshwater tanks as well

## Release 1.2.2

### Technical Maintenance
* Common Patch-Management (Java 21 / Primefaces 13 / Dependencies)
* Mapping Layer (BE module), replaces by using Mapstruts

## Release 1.2.1
### Bugfixes
* i18n Language detection

### Technical Maintenance
* Patch-Management

## Release 1.2.0

### New Features
* SABI-64: Add open stats to prometheus registry
* SABI-85: Added a Reminder Service

### Technical Maintenance
* Migration to Spring-Boot-3
* Patch-Management (openapidoc v2 / Primefaces 12)

## Release 1.0.2

### New Features
* SABI-116: Added more (time-view) measurement report charts 
* SABI-130: PastPlagues table includes the duration of observed plage

### Technical Maintenance
* Patch-Management

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
* Patch-Management

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
