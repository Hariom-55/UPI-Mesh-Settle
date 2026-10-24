# API Reference

Base path: `/api`

## Demo flow, in call order

### `GET /server-key`
Returns the backend's RSA public key so a client can encrypt a
`PaymentInstruction` for it.

**Response**
```json
{
  "publicKey": "base64...",
  "algorithm": "RSA-2048 / OAEP-SHA256",
  "hybridScheme": "RSA-OAEP encrypts an AES-256-GCM session key"
}
```

### `POST /demo/send`
Builds and encrypts a `PaymentInstruction`, wraps it in a `MeshPacket`, and
injects it onto a chosen starting device in the simulated mesh.

**Request**
```json
{
  "senderVpa": "alice@demo",
  "receiverVpa": "bob@demo",
  "amount": 250.00,
  "pin": "1234",
  "ttl": 5,
  "startDevice": "phone-alice"
}
```
`ttl` and `startDevice` are optional (default `5` and `"phone-alice"`).

**Response**
```json
{
  "packetId": "uuid",
  "ciphertextPreview": "first 64 chars...",
  "ttl": 5,
  "injectedAt": "phone-alice"
}
```

### `POST /mesh/gossip`
Advances the mesh simulation by one round — every device holding a packet
with `ttl > 0` forwards a copy (with `ttl - 1`) to every other device that
doesn't already hold it.

**Response**
```json
{
  "transfers": 12,
  "deviceCounts": { "phone-alice": 1, "phone-bridge": 1, "...": 0 }
}
```

### `GET /mesh/state`
Snapshot of every simulated device: which packets it's holding, whether it
has internet, and the current idempotency-cache size.

### `POST /mesh/flush`
Simulates every internet-connected device uploading everything it's
currently holding to the backend. This is where ingestion — decrypt,
dedupe, freshness check, settle — actually happens, once per held packet,
in parallel.

**Response**
```json
{
  "uploadsAttempted": 3,
  "results": [
    {
      "bridgeNode": "phone-bridge",
      "packetId": "first 8 chars",
      "outcome": "SETTLED",
      "reason": "",
      "transactionId": 14
    }
  ]
}
```

### `POST /mesh/reset`
Clears all simulated devices' held packets and the idempotency cache.
Used to reset the demo between runs.

## Direct ingestion (what a real bridge device would call)

### `POST /bridge/ingest`
The actual settlement entry point. `/mesh/flush` calls this internally for
every packet a simulated bridge device is holding — this endpoint also
works standalone if you want to post a `MeshPacket` directly.

**Headers**
- `X-Bridge-Node-Id` (optional, defaults to `"unknown"`)
- `X-Hop-Count` (optional, defaults to `0`)

**Request body**: a `MeshPacket` (`packetId`, `ttl`, `createdAt`, `ciphertext`)

**Response** — one of three outcomes:
| `outcome` | Meaning |
|---|---|
| `SETTLED` | Packet decrypted, was fresh, and reached settlement — note this includes instructions that were *rejected for insufficient balance*; check the underlying transaction's `status` for that distinction. |
| `DUPLICATE_DROPPED` | This ciphertext hash was already claimed; no-op. |
| `INVALID` | Decryption failed, packet was stale (`reason: "stale_packet"`), or future-dated beyond clock-skew tolerance (`reason: "future_dated"`). |

## Read endpoints

### `GET /accounts`
All seeded demo accounts and current balances.

### `GET /transactions`
Most recent 20 transactions (settled and rejected), newest first.
