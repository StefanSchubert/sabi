# Bluewhale's Captcha light REST Service

Captcha light aims to provide software developers with a simple captcha mechanism to start with while bootstrapping a new
product idea. To do so, this approach provides captcha as a RESTful microservice component based upon spring boot, which you can
include within your product environment, and you may adapt to your needs if required.

## Chosen approach

Most of the CAPTCHA services out there rely on pattern recognition to tear humans from machines apart.
Some other relies on mathematics skills. However, they all might be difficult for some machines, but to 
be honest, they all look like the TV winnings games for the dumb audience. I liked the idea of challenging not
only the machine, but us humans a little as well. So to answer the challenge questions you may need to know some
context, a machine might not be able to combine the right context with the question. It might be that you missing the context to answer 
a challenge, but as of other captchas out there you may request a different captcha - I'm pretty sure there is one you are able to answer.   

So when talking about simple CAPTCHA service, I'm referring to setting it up and integrate the API ;-) and not the challenges itself. 


## Why you would use Captcha light, or captcha mechanisms in general?

* If you are going to deploy some self developed service and want to protect it against 
   the denial-of-service attack vector of automated registrations.
* If you want to protect your users from password-forgotten workflow emails, 
  triggered by a bot. (You may lose users, if you don't care about it).
* You won't rely on some offered captcha services out there, and think of offering this 
  functionality as part of your product, so that the service is within your control 
  (by means that you are able to exchange the captcha question set even according to 
  your problem oder service context).
* It's free and simple to integrate.



## Why would you want exchange captcha light?

* This captcha service is simple. A hacker will be able to write brute force algorithms
  to enable bots to scope with it.
* Your service or product has grown out of child-hood, and you require a serious solution, 
  as you may have become more attractive be hackers.
* captcha service in general (even professional solutions) are getting weaker as protective
   measures as KI is getting more and more powerful. That's why all the big-player are switching
   to two-factor authorizations, and you are planning to do so, too.


## Technical documentation

### Stack: Server Side

* JDK 22
* openAPI3 
* Spring-Boot-Application
* REST
* jUnit

### Stack: Client side

...this is up to you. This component does not include a client but you may use swagger
to test the service.


### Build Configuration
Adopt resources/application.properties to your need.

### Build via maven
mvn clean install captcha-service


### API Specification

The REST API has been documented using the open API approach (Swagger). Therefore, if you are 
a software engineer you find the API after start of captcha 
by pointing the browser to captcha/swagger-ui.html

Just start this component (it's a spring boot application)

