# UPI-Mesh-Settle

Spring Boot backend simulating offline UPI payments over a Bluetooth-style
mesh — hybrid RSA/AES-GCM encryption, atomic idempotent settlement,
optimistic locking for concurrent duplicate delivery.

*Not affiliated with or endorsed by NPCI. "UPI" is used here descriptively,
for a personal demo project.*

**Live demo:** _add your Render/Railway URL here once deployed — see
[`infra/DEPLOY.md`](infra/DEPLOY.md)_

## Docs

- [Problem statement](docs/01-problem-statement.md) — what breaks when UPI
  requires live connectivity, and the actual settlement-correctness problem
  underneath the mesh gimmick
- [Proposed solution](docs/02-proposed-solution.md) — architecture and the
  reasoning behind each design decision
- [API reference](docs/03-api-reference.md) — every endpoint, request/response
  shapes, in call order

## Project layout

```
UPI-Mesh-Settle/
├── Application/     Spring Boot source (pom.xml, src/)
├── docs/            Problem statement, design rationale, API reference
└── infra/           Dockerfile + deploy instructions (Render/Railway)
```

## Run locally

```bash
cd Application
./mvnw spring-boot:run
```

Then `GET http://localhost:8080/api/accounts` to confirm it's up and seeded.
H2 console at `http://localhost:8080/h2-console` (local only — disabled by
default in the deployed container).
