# Problem Statement

## The scenario

UPI requires both the payer's and payee's devices (or at least one of them) to
have live internet connectivity at the moment of payment, because the payment
instruction has to reach a PSP/switch in real time. In India, this assumption
breaks down constantly and predictably:

- Basements, elevators, parking structures — no signal.
- Rural and semi-urban areas with patchy mobile data.
- Crowded venues (concerts, stadiums) where towers are saturated.
- Flight mode, international roaming without a local SIM, low-data-balance
  situations where a user deliberately keeps data off.

In every one of these cases, two phones standing next to each other with
Bluetooth/Wi-Fi Direct available *could* exchange a payment instruction
locally — they just have no way to get it **settled**, because settlement
requires a backend that both sides currently can't reach.

## The actual engineering problem

Routing the instruction over a device-to-device mesh until *some* device
regains connectivity is the "fun" half of this problem and is relatively
easy to simulate. The hard half, and the part this project is actually
about, is what happens once that instruction reaches a backend **late,
out of order, and possibly more than once**:

1. **Confidentiality in transit.** The payment instruction hops through
   phones belonging to strangers before it reaches a connected device. Those
   intermediate phones must not be able to read the amount, VPAs, or PIN
   hash they're relaying.
2. **Duplicate delivery is not an edge case, it's the default.** A gossip
   protocol with multiple relay paths means the same packet can plausibly
   reach the backend from more than one bridge device, or get re-uploaded
   after a retry. The backend must settle it exactly once no matter how many
   times it arrives.
3. **Concurrent settlement correctness.** Multiple duplicate/near-duplicate
   packets for the same transaction can be ingested in parallel. The balance
   update must not race — two threads must not both read a sender's balance,
   both see "sufficient funds," and both debit it.
4. **Staleness/replay.** A packet signed hours or days ago and only now
   surfacing from a device that was offline the whole time shouldn't settle
   as if it just happened — there has to be a freshness boundary, and
   packets outside it get rejected, not silently settled.
5. **Honest degradation.** The sender has no proof of delivery until the
   packet is actually bridged. The system has to be explicit about that
   rather than pretending otherwise.

None of these five are mesh-networking problems — they're **distributed
settlement correctness problems** that show up any time a payment system has
to tolerate unreliable, delayed, or duplicated delivery. That's also, not
coincidentally, the same category of problem a UPI switch/PSP reconciliation
layer deals with in production, just triggered by a different cause (network
partitions and retries, not offline phones).

## What this project deliberately does NOT solve

Documented honestly rather than glossed over:

- No real Bluetooth/BLE mesh — `MeshSimulatorService` simulates gossip
  routing in-process so the whole flow is demoable without hardware.
- No user authentication/session layer — VPAs are used directly, there's no
  login.
- No rate limiting or API auth on `/api/bridge/ingest` — a real bridge
  device would need to authenticate itself to the backend.
- Single-node backend — no distributed consensus, no multi-region
  settlement. The "exactly once" guarantee holds for one backend instance
  with one database, not a horizontally scaled cluster.
