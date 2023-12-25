# Ansible Deployment

The deployment via ansible on single rasperry-pi's is like using a sledgehammer to crack a nut,
it is unsuitable here but I liked the idea of having a small blueprint of it to see how it goes.

So here it is. The scripts in here are meant for deployment in my private computer center ![Computer Center](https://github.com/StefanSchubert/sabi/raw/master/sabi-server/UML/7_DeploymentView/SabiRZ.png)
so merge request involving these configurations will be denied for security reasons.
However I publish my deployment pipeline here too, to have it all together in a single git
repository.

## Preconditions to use these scripts

* Ansible and ssh are available
* The ssh private key of the executing user has been published onto the 'pi' account.

## Examples

_Execute something on all pis_

	ansible sabi -i hosts -u pi -a "/bin/uname -a"

or check the unattended update logs

	ansible sabi -i hosts -u pi -a "tail -n20 /var/log/unattended-upgrades/unattended-upgrades.log"


## Deployment of a new captcha release

1) Build the new captcha release
2) ansible-playbook -i hosts deployCaptchaService.yml

## Deployment of a sabi backend release

1) Build the new sabi server release
2) ansible-playbook -i hosts deploySabiService.yml

## Deployment of a sabi frontend release

1) Build the new sabi webclient release
2) ansible-playbook -i hosts deploySabiWebclient.yml