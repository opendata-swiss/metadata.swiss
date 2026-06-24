# How to setup a keycloak service-account for catalog publishers

Scenario: A catalog publisher wants to use the `hub-repo` API to create/update/delete datasets in specific catalogs (machine-to-machine API access).

See also: `opendata.swiss/metadata/scripts/publisher/dataset.sh` - a shell script that shows how to get the token from keycloak and call the `hub-repo` API with the token

> **ℹ️ Info:** This guide uses `bfs-publisher-apitest` as example for a specific client and `bfs-test` as example for a specific resource. Adapt these to your specific situation.


## Step 1 — Create a new Client in Keycloak (service-account)

Navigate to the Keycloak Admin Console and follow these steps.

### 1.1 Open Clients

- Log in to the Keycloak Admin Console.
- In the left sidebar, click **Manage realms** and select the correct **Realm**.
- In the left sidebar, click **Clients**, then click **Create client**.

### 1.2 General Settings tab

| Setting | Value |
|---|---|
| Client type | OpenID Connect |
| Client ID | e.g. `bfs-publisher-apitest` (choose a meaningful name) |
| Name | e.g. BFS_PublisherAPITest |
| Description | e.g. Service account for the BFS, used by them to test Piveau API calls |

Click **Next**.

### 1.3 Capability Config tab

| Setting | Value |
|---|---|
| Client authentication | **ON** — this enables the client secret |
| Authorization | OFF |
| Standard flow | OFF |
| Direct access grants | OFF |
| **Service accounts roles** | **ON ← This is the key setting** |

> **⚠️ Important:** "Service accounts roles" must be ON. This enables the Client Credentials grant and creates the hidden service account user automatically.

Click **Next**, then **Save**.

---

## Step 2 — Copy the Client Secret

Keycloak generates a client secret. The script of the publisher will use this to authenticate and request a token.

- In your newly created client, go to the **Credentials** tab.
- Find **Client Secret** and click the copy icon.

> **🔑 Security:** Treat the client secret like a password. Rotate it periodically via the Credentials tab (regenerate button). Your script should read it from an environment variable, not from source code.

---


## Step 3 — Grant Access to the Resource

Piveau uses Keycloak's **Authorization Services** with a **scope-based authorization model**. The authorization configuration lives on the **`piveau-hub-repo`** client (not the `bfs-publisher-apitest` you just created). The following steps involving the policy and permission happen inside the **`piveau-hub-repo`** client's **Authorization** tab.

The resource `bfs-test` (the catalog) already exists and has three scopes defined:

- `dataset:create`
- `dataset:update`
- `dataset:delete`

You need to: (1) make the service account a member of the `bfs-test` group, (2) create a Group Policy targeting that group, and (3) create a Scope-Based Permission that ties the `bfs-test` resource, the required scopes, and the new policy together.

### 3.1 Add the service account to the `bfs-test` group

When "Service accounts roles" is enabled, Keycloak creates a hidden user named `service-account-bfs-publisher-apitest`. Add this user to the existing group:

1. In the left sidebar, go to **Users**.
2. Search for `service-account-bfs-publisher-apitest` and open it.
3. Go to the **Groups** tab.
4. Click **Join Group**, select **`bfs-test`**, and confirm.

> **ℹ️ Info:** The Group Policy in the next step relies on group membership for authorization.

### 3.2 Create a Group Policy

1. In the left sidebar, go to **Clients**.
2. Open the **`piveau-hub-repo`** client.
3. Go to **Authorization** → **Policies** → **Create client policy** → **Group**.
4. Fill in:

| Field | Value |
|---|---|
| Name | Only Members of bfs-test |
| Groups | Click **Add groups**, select **`bfs-test`** |
| Logic | Positive |

5. Click **Save**.

This policy now evaluates to "true" for any user (including service accounts) that is a member of the `bfs-test` group.

### 3.3 Create a Scope-Based Permission

This step connects the resource, the required scopes, and the policy you just created.

1. Still in **Authorization** for the `piveau-hub-repo` client, go to the **Permissions** tab.
2. Click **Create permission** → **Scope-based permission**.
3. Fill in:

| Field | Value |
|---|---|
| Name | Group Access to catalog bfs-test |
| Resources | Select the existing resource **`bfs-test`** |
| Scopes | Select `dataset:create`, `dataset:update`, `dataset:delete` (or only the subset your script needs) |
| Apply policy | Select **Only Members of bfs-test** |
| Decision strategy | Unanimous  |

4. Click **Save**.

---

## Step 3a — Test the Permission with the Keycloak Evaluation Tool

Verify the permission logic directly inside Keycloak.

1. Open the **`piveau-hub-repo`** client.
2. Go to **Authorization** → **Evaluate** tab.
3. Configure the simulated request:

| Field | Value |
|---|---|
| User | `service-account-bfs-publisher-apitest` |
| Client | `piveau-hub-repo` |
| Resources | `bfs-test` |
| Scopes | Select the scope(s) you assigned in the permission, e.g. `dataset:update` |

4. Click **Evaluate**.
