# User Identity OIDC Replication Server

[![Actions Status](https://github.com/gridsuite/user-identity-oidc-replication-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/user-identity-oidc-replication-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Auser-identity-oidc-replication-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Auser-identity-oidc-replication-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **user-identity-oidc-replication-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **storing a local replica of user identity information coming from the OIDC provider**.

Users in the platform are identified only by their OIDC `sub` (subject) identifier. This service bridges the gap between the identity provider and the rest of the backend by persisting human-readable information (first name, last name) extracted from the OIDC ID token claims. Other services can then resolve a `sub` to a display name without ever contacting the identity provider directly. This is particularly useful when the identity provider does not expose an API to query other users' information.

It provides the following capabilities:

- **Store and update user identity data** (OIDC ID token claims) pushed by the gateway on every authenticated request.
- **Retrieve user identity** (first name, last name) by `sub`, individually or in batch.


Note that this service may be replaced in some deployment contexts (for example, by fetching first name and last name from an enterprise LDAP directory).


---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator)
- PostgreSQL
- Liquibase
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus

---

## How It Works

On every authenticated request, the **gateway** validates the incoming JWT and asynchronously forwards the full ID token claims to this service via `PUT /v1/users/identities/{sub}` in a fire-and-forget manner — the gateway does not wait for a response and a failure has no impact on the original request. The stored claims are parsed to extract `firstName` and `lastName` fields. This means the identity data is automatically kept up to date on every user interaction, with no manual provisioning required.

> **Note:** The identity data originates from each user's own ID token (the token issued to them at login). It is then stored centrally so that other services can access any user's display name, regardless of whether they have ever interacted with that other user.

This behaviour is **opt-in** and can be disabled in the gateway configuration. When disabled, this service receives no data and the rest of the platform works without it.
┌─────────────────────────────────────────────┐
│               gateway                       │
│  TokenValidatorGlobalPreFilter              │
│    validates JWT                            │
│    → PUT /v1/users/identities/{sub} ───────►│  user-identity-oidc-replication-server
│      (async, best-effort)                   │         (upsert idtoken claims)
└─────────────────────────────────────────────┘

user-admin-server  ──► GET /v1/users/identities?subs=...  (enrich user listings with names)
```

---

## Development Scripts

Build Docker image:

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/main/resources/db/changelog/db.changelog-master.yml`.

---

