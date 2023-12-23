# Welcome to Open Science at the SABI-Project - Seawater Aquarium Business Intelligence

This is a semi-scientific (or open-science) project that aims to gain insights from aquarium hobbyist for aquarium
hobbyist according seawater ecosystems.

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

## Climate-Friendly (#greenwashing?)
I declare the Sabi Project climate-friendly because of:
* You as a user, who is **not** demanding, that the Sabi Service is up and running 24/7 with
 99% availability, accepting minor service outages, thus enabling the low-energy platform for the project.
* The decision to use raspberry pis as operation platform, which have a very low energy profile  (Have a look at my pis here: https://github.com/StefanSchubert/sabi/wiki/07.-Deployment-View )
  The alternative would be running in a public cloud, which would allow 99% availability but surly a much bigger CO2 footprint (as well as bigger costs).
* The private cloud at my homesite is powered by a green electricity tariff of my power supply provider.
  I in future I will generate my own electricity through solarcells on the roof.

## Project Planning

### Stage I

Being able to collect the basic values and to display them in a rather static reporting manner. (**Reached**)

**Next:** Transistion to Stage II. Precondition: at least 50 registered users.  

### Stage II

Offering some query mechanism to do some analysis. And maybe a set of some standard reports. If possible we might acquire support from one of the big BI vendors.

### Stage III

Document insights gained through this project. If possible try to make forecasts (i.e. take care of that measurement level, if not raised it is likely that ... happen)

### Possible NON-Scientific extensions

Solution for:
* I have some siblings in my nano reef tank and need to do some gardening. But where to with the siblings? Where are all the other aquarists and is there someone nearby? They are organized in standard internet forums, but what if there are someone near but not located in the same forum I use (more or less frequently). If they all could be motivated using sabi it should be possible to introduce them to each other for nearby support purposes.

## Release Planning

Just have a look at the [Milestones](https://github.com/StefanSchubert/sabi/milestones?direction=asc&sort=due_date&state=open) from the Issue Board

## Project history
| Date          | News                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 05th Nov 2023 | Service Release (Patchmanagement - e.g. Java 21)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 09th Apr 2023 | Hotfix Release (i18n issues)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 18th Dez 2022 | Feature Release (Reminder Service) / Technical Migration to Spring-Boot 3  / Marks Sabi-Version 1.2.0                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 7th Okt 2022  | Released Plague-Center. All required base workflows have been implemented. This marks version 1.0.0 of sabi.                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 22nd Jul 2022 | Article about SABI has been published in the journal "KORALLE", Issue Nr. 136. 😊 Available on start of August. Many Thanks goes to Daniel Knop!                                                                                                                                                                                                                                                                                                                                                                                                             |
| 26th Feb 2022 | Official Softlaunch Day of the project. Starting with a twitter announcement. Going to spread the word in selected forums in the next days, hoping to get some Beta-Testers, Fellow-Coders and to collect valuable feedback.                                                                                                                                                                                                                                                                                                                                  |
| 27th Mai 2021 | I managed to replace the self-signed TLS cert with a let's encrypt based one. This gets us rid of the browsers insecure warning.                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 16th Mai 2021 | **Sneak Preview** available on https://sabi-project.net (Notice: Only available if you have an **IPV6 Internet-Connection** (your mobile with wlan switched off should do it, if your home has only the half internet available). As of the zero budget start we have a self signed TLS resulting in a browsers insecure warning. Still some bugs of course (see issue list), and features left till release 1.0 but it's already usable. In case you decide to create an account...it's already the production environment ;-) i.e. your data will be kept. |

## For Developers

Please have a look at:
* [CONTRIBUTING.md](CONTRIBUTING.md)
* [DEVELOPERS_MANUAL.md](DEVELOPERS_MANUAL.md)