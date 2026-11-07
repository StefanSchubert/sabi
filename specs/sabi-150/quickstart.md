# Quickstart: Google OIDC Setup (sabi-150)

**Phase**: 1 — Design  
**Date**: 2026-03-26  
**Audience**: Developer setting up sabi-150 locally or deploying to production

---

## Step 1 — Google Cloud Console: App Registration

### 1.1 Create a new project (or use existing)

1. Go to [https://console.cloud.google.com/](https://console.cloud.google.com/)
2. Select or create a project (e.g. `sabi-oidc`)

### 1.2 Configure OAuth Consent Screen

1. Navigate to **APIs & Services → OAuth consent screen**
2. Choose **External** (for users outside your Google Workspace)
3. Fill in:
   - **App name**: `Sabi`
   - **User support email**: your email
   - **Developer contact**: your email
   - **Homepage URL**: `https://<your-strato-dyndns-hostname>`
   - **Privacy policy URL**: `https://<your-strato-dyndns-hostname>/privacy` (or existing DSGVO page)
   - **Terms of service URL**: `https://<your-strato-dyndns-hostname>/terms`
4. **Scopes**: Add `openid`, `email`, `profile`
5. **Test users**: Add your own Google account for testing before going public

### 1.3 Create OAuth 2.0 Credentials

1. Navigate to **APIs & Services → Credentials → Create Credentials → OAuth client ID**
2. Application type: **Web application**
3. Name: `sabi-webclient`
4. **Authorized JavaScript origins**: `https://<your-strato-dyndns-hostname>`
5. **Authorized redirect URIs**:
   - Production: `https://<your-strato-dyndns-hostname>/login/oauth2/code/google`
   - Development: `http://localhost:8082/login/oauth2/code/google` *(adjust port as needed)*
6. Click **Create** → note the `Client ID` and `Client Secret`

> ⚠️ **Never commit** Client ID or Client Secret to the repository (Constraint C-2).

---

## Step 2 — Local Development Setup

### 2.1 Set environment variables

```bash
export GOOGLE_OIDC_CLIENT_ID=<your-client-id-from-console>
export GOOGLE_OIDC_CLIENT_SECRET=<your-client-secret-from-console>
```

Add these to your shell profile (`~/.zshrc`) or IDE run configuration — **not** to any tracked file.

### 2.2 Configure `sabi-webclient/src/main/resources/application.properties`

The properties file should reference environment variables (never hardcoded values):

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_OIDC_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_OIDC_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.google.com
```

### 2.3 Run locally

```bash
# Terminal 1: sabi-server (port 8080)
cd sabi-server
./mvnw spring-boot:run

# Terminal 2: sabi-webclient (port 8082 — adjust if different)
cd sabi-webclient
./mvnw spring-boot:run
```

Open `http://localhost:8082` → click **Login with Google** → complete Google OAuth flow → should land on dashboard.

### 2.4 Verify the OIDC flow in the log

Expected log output on successful login:
```
INFO  OIDC_LOGIN_SUCCESS provider=GOOGLE userId=<id> provisioned=true/false
```

---

## Step 3 — Production Deployment

### 3.1 Ansible secrets

Add to the appropriate Ansible `group_vars` encrypted vault file:

```yaml
sabi_google_oidc_client_id: "<client-id>"
sabi_google_oidc_client_secret: "<client-secret>"
```

### 3.2 Ansible template for `application.properties`

In the webclient's `application.properties.j2` template:

```properties
spring.security.oauth2.client.registration.google.client-id={{ sabi_google_oidc_client_id }}
spring.security.oauth2.client.registration.google.client-secret={{ sabi_google_oidc_client_secret }}
```

### 3.3 Deploy

```bash
cd devops/ansible
ansible-playbook deploySabiWebclient.yml -i hosts --ask-vault-pass
```

---

## Step 4 — IPv6 / DS-Lite Validation (OQ-1)

After deploying to production:

1. From an external IPv4 network, open `https://<strato-dyndns-hostname>`
2. Click **Login with Google**
3. Complete the Google login
4. Verify redirect back to Sabi succeeds

If the callback fails (Google cannot reach the server):
- Check that the redirect URI in Google Cloud Console matches exactly (including trailing slash / no trailing slash)
- Verify the server is listening on `::` (all IPv6 interfaces), not just `::1`
- Check nginx config: `listen [::]:443 ssl` must be present

---

## Step 5 — Verification Checklist

After setup, verify the following before marking sabi-150 as done:

- [ ] `POST /api/auth/oidc/google` with a valid Google ID token returns HTTP 200 + Sabi JWT
- [ ] New user auto-provisioning creates exactly one `users` row and one `oidc_provider_link` row
- [ ] Existing user email-match links without creating a duplicate account
- [ ] Locked account returns HTTP 423
- [ ] Invalid ID token (tampered signature) returns HTTP 401
- [ ] Client secret is NOT in any tracked file (`git grep GOOGLE_OIDC` must return 0 results)
- [ ] OIDC events appear in audit log (no PII in plain text)
- [ ] Standard password login still works after OIDC changes (regression)
- [ ] IPv6 callback validated from external network (OQ-1)

---

*Quickstart complete. Next: speckit.tasks to generate the implementation task list.*

