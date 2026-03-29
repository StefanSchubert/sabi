# Ansible Vault – Secrets-Management für SABI

## Was ist Ansible Vault?

Ansible Vault ist ein eingebautes Verschlüsselungsfeature von Ansible.
Es ermöglicht, **Secrets verschlüsselt ins Git einzuchecken** – ohne dass sie
lesbar im Repository stehen. Ansible entschlüsselt die Werte automatisch
beim Ausführen eines Playbooks, sofern du das Vault-Passwort angibst.

**Das Problem, das es löst:**
```
Open-Source-Repo → Secrets dürfen nicht committet werden
→ Produktionsconfig wird händisch nachgepflegt
→ Deployment überschreibt Produktionsconfig
→ Frustration
```

**Die Lösung mit Vault:**
```
Secret wird mit Vault-Passwort verschlüsselt
→ Chiffrat wird committet (sieht aus wie base64-Blob)
→ Ansible entschlüsselt beim Deploy automatisch
→ Kein händisches Nachpflegen mehr
```

---

## Secrets-Inventar für SABI

Übersicht aller Secrets und ihr aktueller Verwaltungsstatus:

| Secret | Service | Datei | Status |
|--------|---------|-------|--------|
| `GOOGLE_OIDC_CLIENT_ID` | sabi-webclient (FE) | `group_vars/sabiFrontend.yml` | ⚠️ Placeholder – **noch nicht vaulted** |
| `GOOGLE_OIDC_CLIENT_SECRET` | sabi-webclient (FE) | `group_vars/sabiFrontend.yml` | ⚠️ Placeholder – **noch nicht vaulted** |
| `GOOGLE_OIDC_CLIENT_ID` | sabi-service (BE) | `group_vars/sabiBackend.yml` | ⚠️ Placeholder – **noch nicht vaulted** |
| DB-Password (`sabi123`) | sabi-service (BE) | `sabi-server/application.yml` + ext. Config | ⚠️ Extern manuell gepflegt |
| SMTP-Password | sabi-service (BE) | extern auf Server | ⚠️ Extern manuell gepflegt |
| JWT-Signing-Secret | sabi-service (BE) | extern auf Server | ⚠️ Extern manuell gepflegt |
| Grafana SMTP-Password | Grafana | `~/Documents/SABI_Config_Safe/grafana/custom.ini` | ✅ Lokal isoliert (nicht im Git) |
| Grafana Secret Key | Grafana | `~/Documents/SABI_Config_Safe/grafana/custom.ini` | ✅ Lokal isoliert (nicht im Git) |

> **Hinweis Grafana:** Für Grafana existiert bereits ein separater Mechanismus
> (`GRAFANA_CUSTOM_INI.md`). Die `custom.ini` liegt außerhalb des Repos.
> Vault wäre hier optional, aber nicht zwingend – das bestehende Verfahren ist ausreichend.

---

## Wie das Vault-Verfahren in SABI funktioniert

### Das Muster (am Beispiel der OIDC-Secrets)

**1. Schritt – Secret verschlüsseln (einmalig, lokal):**
```bash
cd /Users/bluewhale/dev/Intellij-wss/github/sabi.git/devops/ansible
ansible-vault encrypt_string 'dein-echtes-client-secret' --name 'sabi_google_oidc_client_secret'
```

Ausgabe (Beispiel):
```yaml
sabi_google_oidc_client_secret: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          38653364623335353561373532386662646234633430653663393339376562336633386264393664
          6635363233623035363437386636343237393362343934330a333638373135326565663166363737
          ...
```

**2. Schritt – Chiffrat in `group_vars` eintragen:**
Das `!vault |`-Block ersetzt den `"CHANGEME_USE_ANSIBLE_VAULT"`-Placeholder in
`group_vars/sabiFrontend.yml` bzw. `group_vars/sabiBackend.yml`.

**3. Schritt – Deployen mit Vault-Passwort:**
```bash
# Interaktiv (Passwort-Prompt):
ansible-playbook deploySabiWebclient.yml --ask-vault-pass

# Oder mit Passwort-Datei (bequemer):
ansible-playbook deploySabiWebclient.yml --vault-password-file ~/.vault_pass_sabi
```

**Was dann passiert:**
```
group_vars/sabiFrontend.yml (verschlüsselt)
    ↓ Ansible entschlüsselt
deploySabiWebclient.yml schreibt /etc/sabi/sabi-frontend.env
    GOOGLE_OIDC_CLIENT_ID=<entschlüsselter Wert>
    GOOGLE_OIDC_CLIENT_SECRET=<entschlüsselter Wert>
    ↓
sabiFE.service liest EnvironmentFile=/etc/sabi/sabi-frontend.env
    ↓
Spring Boot liest ${GOOGLE_OIDC_CLIENT_ID}
```

---

## Schritt-für-Schritt: Vault einrichten

### 1. Vault-Passwort-Datei anlegen (einmalig)

```bash
# Passwort-Datei anlegen (außerhalb des Repos!)
echo 'dein-vault-passwort' > ~/.vault_pass_sabi
chmod 600 ~/.vault_pass_sabi
```

> ⚠️ Diese Datei **niemals** ins Git einchecken.
> Empfehlung: In `~/.gitignore_global` oder `.gitignore` des Repos absichern.

### 2. OIDC-Secrets vaulten (Frontend)

```bash
cd /Users/bluewhale/dev/Intellij-wss/github/sabi.git/devops/ansible

# Client-ID verschlüsseln:
ansible-vault encrypt_string 'DEINE_ECHTE_GOOGLE_CLIENT_ID' \
  --name 'sabi_google_oidc_client_id' \
  --vault-password-file ~/.vault_pass_sabi

# Client-Secret verschlüsseln:
ansible-vault encrypt_string 'DEIN_ECHTES_GOOGLE_CLIENT_SECRET' \
  --name 'sabi_google_oidc_client_secret' \
  --vault-password-file ~/.vault_pass_sabi
```

Die Ausgabe in `group_vars/sabiFrontend.yml` eintragen:

```yaml
# group_vars/sabiFrontend.yml
ansible_user: pi
ansible_become: yes
ansible_python_interpreter: /usr/bin/python3

sabi_google_oidc_client_id: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          <hier den verschlüsselten Block einfügen>

sabi_google_oidc_client_secret: !vault |
          $ANSIBLE_VAULT;1.1;AES256
          <hier den verschlüsselten Block einfügen>
```

### 3. OIDC-Client-ID vaulten (Backend)

```bash
ansible-vault encrypt_string 'DEINE_ECHTE_GOOGLE_CLIENT_ID' \
  --name 'sabi_google_oidc_client_id' \
  --vault-password-file ~/.vault_pass_sabi
```

In `group_vars/sabiBackend.yml` eintragen.

### 4. Deployment ausführen

```bash
# Frontend deployen:
ansible-playbook deploySabiWebclient.yml \
  --vault-password-file ~/.vault_pass_sabi

# Backend deployen:
ansible-playbook deploySabiService.yml \
  --vault-password-file ~/.vault_pass_sabi

# Alles auf einmal (wenn alle Playbooks angepasst):
ansible-playbook deploySabiWebclient.yml deploySabiService.yml \
  --vault-password-file ~/.vault_pass_sabi
```

---

## Migrationspfad: Was noch auf Vault umgestellt werden sollte

### Phase 1 – OIDC-Secrets (jetzt umsetzen)

Die Placeholder in den `group_vars` durch echte Vault-Werte ersetzen,
wie oben beschrieben.

### Phase 2 – Spring Boot Applikations-Secrets ✅ umgesetzt

`application.yml` nutzt jetzt `${ENV_VAR:dev-default}` für alle Secrets.
Docker-lokal greift weiterhin der Dev-Default, Produktion bekommt die
echten Werte per EnvironmentFile aus Ansible Vault.

| Variable | Ansible-Var | Datei |
|----------|-------------|-------|
| `SABI_DB_PASSWORD` | `sabi_db_password` | `group_vars/sabiMiddleware.yml` |
| `SABI_JWT_SECRET` | `sabi_jwt_secret` | `group_vars/sabiMiddleware.yml` |
| `SABI_SMTP_HOST` | `sabi_smtp_host` | `group_vars/sabiMiddleware.yml` |
| `SABI_SMTP_PASSWORD` | `sabi_smtp_password` | `group_vars/sabiMiddleware.yml` |

> Alle Placeholder in `group_vars/sabiMiddleware.yml` müssen noch durch
> echte Vault-Werte ersetzt werden (siehe Schritt-für-Schritt-Anleitung oben).

---

## Sonderfall Grafana

Grafana nutzt **keinen Vault**, sondern einen separaten Isolations-Mechanismus:

- Die `custom.ini` mit SMTP-Credentials liegt lokal unter
  `~/Documents/SABI_Config_Safe/grafana/custom.ini` (nicht im Git)
- Ansible kopiert diese Datei auf den Server (`deployMyMonitoringSolution.yml`)
- Details: siehe `GRAFANA_CUSTOM_INI.md`

Dieser Ansatz ist **ausreichend** für Grafana, da die Secrets nie das lokale
Dateisystem verlassen und nicht im Repo landen. Eine Migration auf Vault
wäre optional, aber nicht prioritär.

---

## Vault-Passwort sicher aufbewahren

Das Vault-Passwort ist das **Master-Secret** – wer es hat, kann alle
verschlüsselten Werte entschlüsseln.

### Zur Sicherheit von AES-256

Ansible Vault verwendet AES-256 – das klingt weniger als ein GPG-Schlüssel
mit 4096 Bit, ist aber kein Sicherheitsproblem. Der Vergleich ist
Äpfel mit Birnen:

- **RSA/GPG** ist *asymmetrisch* und durch Primfaktorzerlegung mathematisch
  angreifbar → braucht große Schlüssel (4096 Bit) für ausreichende Sicherheit
- **AES** ist *symmetrisch* und hat keinen solchen mathematischen Angriffspunkt
  → 256 Bit bedeuten 2²⁵⁶ mögliche Schlüssel, was praktisch unknackbar ist

Kryptografische Äquivalenz nach NIST SP 800-57:
```
AES-256  ≈  RSA-15360 Bit  ≈  ECC-512 Bit
```

AES-256 ist stärker als RSA-4096 und von NIST für Top-Secret-Daten
(US-Regierung, NSA Suite B) bis weit über 2100 hinaus empfohlen.

**Die einzige echte Schwachstelle ist die Passwortstärke:**
```
Ansible Vault intern:
  Passwort → PBKDF2(SHA256, 10.000 Iterationen) → AES-256-CTR Key
                  ↑
              HIER liegt das Risiko – nicht im Algorithmus
```

> Ein schwaches Vault-Passwort (`sabi123`, `password`) hebelt AES-256 aus.
> Ein kryptografisch zufälliges Passwort macht Brute-Force praktisch unmöglich.

### Empfehlungen zur Passwortverwaltung

```bash
# EMPFOHLEN: Kryptografisch zufälliges Passwort erzeugen (256 Bit Entropie):
openssl rand -base64 32 > ~/.vault_pass_sabi
chmod 600 ~/.vault_pass_sabi

# Option 2: In einem Passwort-Manager (z.B. KeePass, Bitwarden)
# → Zufälliges Passwort generieren lassen, vor dem Deploy in Datei kopieren

# Option 3: macOS Keychain (sicherste Variante für den Alltag)
security add-generic-password -a sabi-vault -s ansible-vault -w "$(openssl rand -base64 32)"
# Abrufen beim Deploy:
ansible-playbook deploy.yml \
  --vault-password-file <(security find-generic-password -a sabi-vault -s ansible-vault -w)
```

---

## Cheat Sheet

```bash
# Secret verschlüsseln:
ansible-vault encrypt_string 'SECRET' --name 'var_name' \
  --vault-password-file ~/.vault_pass_sabi

# Verschlüsselten Wert prüfen/entschlüsseln:
echo '$ANSIBLE_VAULT;1.1;AES256...' | ansible-vault decrypt \
  --vault-password-file ~/.vault_pass_sabi

# Playbook mit Vault ausführen:
ansible-playbook playbook.yml --vault-password-file ~/.vault_pass_sabi

# Vault-Passwort interaktiv eingeben (ohne Datei):
ansible-playbook playbook.yml --ask-vault-pass

# Prüfen ob Vault-Wert korrekt in vars aufgelöst wird (debug, Ausgabe maskiert):
ansible -i hosts sabiFrontend -m debug \
  -a "var=sabi_google_oidc_client_id" \
  --vault-password-file ~/.vault_pass_sabi
```

---

## Zusammenfassung

| Was | Verfahren |
|-----|-----------|
| OIDC Client-ID / Secret | Ansible Vault in `group_vars/*.yml` |
| DB-Password, SMTP, JWT-Secret | ⚠️ Noch manuell – Migration geplant (Phase 2) |
| Grafana SMTP / Secret Key | Lokale `custom.ini` außerhalb des Repos |
| SSH-Key für Ansible | `~/.ssh/` – nie im Git |
| Vault-Passwort selbst | Passwort-Manager / `~/.vault_pass_sabi` (chmod 600) |

