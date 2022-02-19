This docker-compose provides all required backend services:

* database
* captcha-service
* sabi-service
* fakeSMTP-Mailserver

for being able to develop the sabi frontends.

In Addition, to profile services runtime behavior during development:

* grafana
* prometheus

for monitoring purposes.

To login into grafana use http://localhost:3000 and

    admin/sabimon

as username/password.

To check which targets are currently being scaped be prometheus, check
prometheus.yml or http://localhost:9090/targets 

