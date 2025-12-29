# Grafana custom.ini - Nur Secrets (SMTP Konfiguration)

## Konzept

Die Grafana-Konfiguration wird in **zwei Schritten** aufgebaut:

1. **`custom.ini`** (deine lokale Datei mit Secrets) wird kopiert → enthält `[smtp]` und andere Secrets
2. **Ansible `lineinfile`** setzt die `[server]` Sektion → automatisch, idempotent

## Deine lokale custom.ini

📁 `/Users/bluewhale/Documents/SABI_Config_Safe/grafana/custom.ini`

Diese Datei enthält **NUR** die Sektionen mit Secrets (nicht im Git!):

### Minimale Version (nur SMTP):

```ini
#################################### SMTP / Emailing #####################
[smtp]
enabled = true
host = mail.example.com:587
user = your-email@example.com
password = """your-secret-password"""
cert_file =
key_file =
skip_verify = false
from_address = grafana@deepspace-9.bluewhale.de
from_name = Grafana DeepSpace-9
ehlo_identity = deepspace-9.bluewhale.de

[emails]
welcome_email_on_sign_up = false
templates_pattern = emails/*.html, emails/*.txt
```

### Mit zusätzlichen Secrets:

```ini
#################################### Security ############################
[security]
# Used for signing cookies and other sensitive data
# Generiere mit: openssl rand -base64 32
secret_key = SW2YcwTIb9zpOOhoPsMm

# disable gravatar profile images
disable_gravatar = false

# Set to true if you host Grafana behind HTTPS
cookie_secure = false

# Set cookie SameSite attribute
cookie_samesite = lax

#################################### SMTP / Emailing #####################
[smtp]
enabled = true
host = mail.example.com:587
user = your-email@example.com
password = """your-secret-password"""
cert_file =
key_file =
skip_verify = false
from_address = grafana@deepspace-9.bluewhale.de
from_name = Grafana DeepSpace-9
ehlo_identity = deepspace-9.bluewhale.de

[emails]
welcome_email_on_sign_up = false
templates_pattern = emails/*.html, emails/*.txt

#################################### Auth (Optional) #####################
[auth]
login_cookie_name = grafana_session
login_maximum_inactive_lifetime_duration = 7d
login_maximum_lifetime_duration = 30d

#################################### OAuth (Optional) ####################
[auth.google]
enabled = false
client_id = your-client-id
client_secret = your-client-secret
scopes = https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email
auth_url = https://accounts.google.com/o/oauth2/auth
token_url = https://accounts.google.com/o/oauth2/token
allowed_domains = bluewhale.de
allow_sign_up = true
```

## Was Ansible automatisch setzt

Die `[server]` Sektion wird **automatisch von Ansible** gesetzt (nach dem Copy):

```ini
[server]
protocol = http
domain = 
http_port = 3000
root_url = %(protocol)s://%(domain)s:%(http_port)s/grafana/
serve_from_sub_path = true
enable_gzip = true
```

**Du musst diese Settings NICHT in deine `custom.ini` schreiben!**

## Workflow

### 1. Erstelle/Aktualisiere deine custom.ini:

```bash
nano /Users/bluewhale/Documents/SABI_Config_Safe/grafana/custom.ini
```

**Inhalt (Minimal):**
```ini
[smtp]
enabled = true
host = mail.example.com:587
user = your-email@example.com
password = """your-secret-password"""
from_address = grafana@deepspace-9.bluewhale.de
from_name = Grafana DeepSpace-9

[emails]
welcome_email_on_sign_up = false
```

### 2. Ansible Playbook ausführen:

```bash
cd /Users/bluewhale/dev/Intellij-wss/github/sabi.git/devops/ansible
ansible-playbook deployMyMonitoringSolution.yml
```

**Was passiert:**

1. ✅ `custom.ini` wird nach `/etc/grafana/grafana.ini` kopiert
2. ✅ Ansible fügt die `[server]` Sektion hinzu (oder aktualisiert sie)
3. ✅ Grafana wird neugestartet

### 3. Ergebnis auf dem Server:

```bash
# Auf dem Pi prüfen:
ssh pi@deepspace-9

sudo cat /etc/grafana/grafana.ini
```

Die Datei enthält jetzt:
```ini
[smtp]
enabled = true
host = mail.example.com:587
user = your-email@example.com
password = """your-secret-password"""
...

[server]
protocol = http
domain = 
http_port = 3000
root_url = %(protocol)s://%(domain)s:%(http_port)s/grafana/
serve_from_sub_path = true
enable_gzip = true
```

## Wichtige Hinweise

### ⚠️ Die [server] Sektion NICHT in custom.ini schreiben!

**Falsch:**
```ini
# In custom.ini - NICHT TUN!
[server]
domain = deepspace-9.bluewhale.de  ← Ansible überschreibt das!
```

**Richtig:**
```ini
# In custom.ini - nur Secrets
[smtp]
password = """geheim"""
```

Ansible setzt die `[server]` Sektion automatisch.

### ✅ Welche Sektionen gehören in custom.ini?

**Gehören in `custom.ini` (Secrets):**
- `[smtp]` - Mail-Server-Credentials
- `[security]` - Secret Key
- `[auth.*]` - OAuth Client Secrets
- `[database]` - DB-Password (falls nicht SQLite)
- Custom-Settings, die nicht von Ansible verwaltet werden

**Werden von Ansible verwaltet:**
- `[server]` - Reverse Proxy Settings
- Alles was mit Domain/URL/Subpath zu tun hat

### 🔄 Updates der custom.ini

Wenn du deine lokale `custom.ini` änderst:

```bash
# 1. Lokal bearbeiten
nano /Users/bluewhale/Documents/SABI_Config_Safe/grafana/custom.ini

# 2. Playbook ausführen
ansible-playbook deployMyMonitoringSolution.yml

# Die [server] Settings bleiben erhalten!
```

### 🔐 Secret Key generieren

Falls du einen neuen Secret Key brauchst:

```bash
openssl rand -base64 32
```

Oder mit Python:
```python
import secrets
print(secrets.token_urlsafe(32))
```

## Template für custom.ini

Hier ist ein vollständiges Template für deine `custom.ini`:

```ini
#################################### Grafana Custom Configuration ########
# Datei: /Users/bluewhale/Documents/SABI_Config_Safe/grafana/custom.ini
# Diese Datei enthält NUR Secrets und wird NICHT ins Git eingecheckt!
# Die [server] Sektion wird automatisch von Ansible gesetzt.
###########################################################################

#################################### Security ############################
[security]
# Secret key for signing cookies
# Generate with: openssl rand -base64 32
secret_key = CHANGE_ME_TO_A_RANDOM_STRING

# Disable gravatar
disable_gravatar = false

# Cookie settings (für HTTPS über Nginx)
cookie_secure = false
cookie_samesite = lax

# Data source proxy whitelist (für Prometheus)
data_source_proxy_whitelist = localhost:9090 [::1]:9090

#################################### SMTP / Emailing #####################
[smtp]
enabled = true
host = mail.example.com:587
user = your-email@example.com

# Wrap password with """ if it contains # or ;
password = """your-mail-password"""

# TLS Settings
cert_file =
key_file =
skip_verify = false

# From address
from_address = grafana@deepspace-9.bluewhale.de
from_name = Grafana Monitoring

# EHLO identity
ehlo_identity = deepspace-9.bluewhale.de

[emails]
welcome_email_on_sign_up = false
templates_pattern = emails/*.html, emails/*.txt

#################################### Auth ################################
[auth]
login_cookie_name = grafana_session
login_maximum_inactive_lifetime_duration = 7d
login_maximum_lifetime_duration = 30d

#################################### Database ############################
# (Nur bei Bedarf - Standard ist SQLite)
[database]
type = sqlite3
path = grafana.db

#################################### Weitere Custom-Settings #############
# Füge hier weitere Sektionen mit Secrets hinzu...
```

## Testen

Nach dem Deployment:

```bash
# 1. Grafana Status prüfen
sudo systemctl status grafana-server

# 2. Konfiguration prüfen
sudo grep -A5 "^\[server\]" /etc/grafana/grafana.ini
sudo grep -A5 "^\[smtp\]" /etc/grafana/grafana.ini

# 3. Lokal testen
curl -s "http://localhost:3000/grafana/" | head -n 20

# 4. Von MacBook testen
curl -s "http://deepspace-9.local:3000/grafana/" | head -n 20
```

## Troubleshooting

### Grafana startet nicht nach Deployment

```bash
# Logs prüfen
sudo journalctl -u grafana-server -n 100 --no-pager

# Häufige Fehler:
# - Syntax-Fehler in custom.ini
# - Fehlende Quotes um Passwörter
# - Doppelte Sektionen
```

### [server] Settings fehlen

```bash
# Prüfen ob lineinfile ausgeführt wurde
sudo grep "^protocol = http" /etc/grafana/grafana.ini
sudo grep "^domain = $" /etc/grafana/grafana.ini

# Falls nicht: Playbook erneut ausführen
ansible-playbook deployMyMonitoringSolution.yml
```

### SMTP funktioniert nicht

```bash
# Test-Mail senden (in Grafana UI)
# Configuration → Server Admin → Send Test Email

# Logs prüfen
sudo journalctl -u grafana-server | grep -i smtp
```

## Zusammenfassung

| Was | Wo | Von wem verwaltet |
|-----|----|--------------------|
| **SMTP Secrets** | `custom.ini` | Du (lokal) |
| **Security Secrets** | `custom.ini` | Du (lokal) |
| **[server] Sektion** | `grafana.ini` | Ansible |
| **Reverse Proxy Settings** | `grafana.ini` | Ansible |

**Workflow:**
1. Du pflegst nur `custom.ini` mit Secrets
2. Ansible kümmert sich um die `[server]` Sektion
3. Beide werden zu einer funktionierenden `grafana.ini` kombiniert

**Vorteile:**
- ✅ Secrets bleiben lokal (nicht im Git)
- ✅ Server-Settings werden automatisch gesetzt
- ✅ Idempotent (mehrfach ausführbar)
- ✅ Einfach zu warten

**Perfekt!** 🎉

