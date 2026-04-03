# Ansible Deployment

The deployment via ansible on single rasperry-pi's is like using a sledgehammer to crack a nut,
it is unsuitable here, but I liked the idea of having a small blueprint of it to see how it goes.

So here it is. The scripts in here are meant for deployment in my private computer center ![Computer Center](https://github.com/StefanSchubert/sabi/raw/master/sabi-server/UML/7_DeploymentView/SabiRZ.png)
so merge request involving these configurations will be denied for security reasons.
However, I publish my deployment pipeline here too, to have it all together in a single git
repository.

## Preconditions to use these scripts

* Ansible and ssh are available
* The ssh private key of the executing user has been published onto the 'pi' account.

> **Pi wakeup:** Every playbook starts with a `wait_for_connection` task (timeout: 120s).
> If a Pi is slow to respond after a reboot or from sleep, Ansible will wait patiently
> instead of failing immediately. Additionally `ansible_ssh_retries: 5` is set globally.

## Examples

_Execute something on all pis_

	ansible sabi -i hosts -u pi -a "/bin/uname -a"

or check the unattended update logs

	ansible sabi -i hosts -u pi -a "tail -n20 /var/log/unattended-upgrades/unattended-upgrades.log"


## Vault-Passwort

Die verschlüsselten Secrets in den `group_vars/*.yml` werden beim Deployment
automatisch entschlüsselt. Die Vault-Passwort-Datei liegt unter:

```
~/Documents/SABI_Config_Safe/.ansible_vault_pass_sabi
```

> Diese Datei liegt **außerhalb des Repos** und wird **nicht ins Git** eingecheckt.
> Details zum Vault-Verfahren: siehe [ANSIBLE_VAULT.md](ANSIBLE_VAULT.md)

## Deployment of a new captcha release

1) Build the new captcha release
2) `ansible-playbook -i hosts deployCaptchaService.yml`

## Deployment of a sabi backend release

1) Build the new sabi server release
2) `ansible-playbook -i hosts deploySabiService.yml --vault-password-file ~/Documents/SABI_Config_Safe/.ansible_vault_pass_sabi`

## Deployment of DB-Node supplements (pimetric metrics on Esmeralda)

> Esmeralda hosts the MariaDB. The pimetric service provides Prometheus/Grafana health metrics.
> No application deployment here – only monitoring supplements.

1) Build pimetric release (if changed)
2) `ansible-playbook -i hosts deployDBNodeSupplements.yml`

## Deployment of a sabi frontend release

1) Build the new sabi webclient release
2) `ansible-playbook -i hosts deploySabiWebclient.yml --vault-password-file ~/Documents/SABI_Config_Safe/.ansible_vault_pass_sabi`