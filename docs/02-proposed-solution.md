# Proposed Solution

## Architecture, end to end

```
 Sender's phone                 Mesh of offline devices             Bridge device         Backend (this repo)
 ───────────────                ─────────────────────             ──────────────         ───────────────────
 DemoService.createPacket()  →  MeshSimulatorService.gossipOnce()  →  (has internet)   →  BridgeIngestionService.ingest()
   builds PaymentInstruction      TTL-bounded flood/gossip             uploads held          decrypt → dedupe →
   encrypts it (hybrid crypto)    across VirtualDevice nodes           packets                freshness check → settle
```

The simulator stands in for real phones and real Bluetooth/Wi-Fi Direct —
the interesting engineering is everything from "packet lands on the backend"
onward, which is real and not simulated.

## Design decisions, and why

| Decision | Why |
|---|---|
| **Hybrid RSA-OAEP + AES-256-GCM encryption** | A payment instruction has to survive hopping through untrusted intermediate devices. RSA alone doesn't scale to arbitrary payload sizes efficiently; AES alone needs a shared key the intermediaries can't be trusted with. Hybrid (AES encrypts the payload, RSA encrypts a one-time AES key with the server's public key) is the standard pattern for exactly this situation. |
| **SHA-256 hash of ciphertext as the idempotency key** | Hashing the *ciphertext* rather than a packet-supplied ID means a replayed/duplicated packet is detected even if something along the mesh path tampered with or regenerated metadata — the dedupe key is derived from the actual bytes, not a value a relay could manipulate. |
| **`ConcurrentHashMap.putIfAbsent` as the dedupe gate** | Atomic claim-or-reject with no lock contention, O(1), and correct under concurrent ingestion — two threads racing to claim the same hash can't both win. |
| **Freshness window (default 24h) on `signedAt`, not `createdAt`** | `signedAt` comes from inside the encrypted payload (tamper-evident via the AES-GCM tag), so staleness is checked against a value the mesh itself can't forge. Packets older than the window are rejected outright rather than settled as if current. |
| **Optimistic locking (`@Version`) on `Account`** | Two near-simultaneous settlement attempts touching the same account must not both apply under a stale read. `@Version` makes the second writer's commit fail and retry rather than silently lose an update — cheaper than pessimistic row locks for the expected low-contention case, and the failure mode is loud (an exception), not silent data loss. |
| **Rejected transactions are still recorded, not just dropped** | Insufficient-balance attempts get a `Transactions` row with `status=REJECTED` instead of being discarded. An auditable system shouldn't need you to replay logs to find out an attempted debit happened and was declined — that's a settlement-correctness, not just a UX, concern. |

## Known limitations (see `01-problem-statement.md` for the full list)

Single-node, no auth on ingestion, no real Bluetooth — all deliberate scope
cuts for a demo-able backend, documented rather than hidden.
