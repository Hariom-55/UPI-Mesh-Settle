package meshSettle.Application.service;


import lombok.RequiredArgsConstructor;
import meshSettle.Application.model.MeshPacket;
import meshSettle.Application.model.PaymentInstruction;
import meshSettle.Application.model.Transactions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BridgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(
            BridgeIngestionService.class
    );

    private final IdempotencyService idempotencyService;
    private final HybridCryptoService hybridCryptoService;
    private final SettlementService settlementService;

    @Value("${upi.mesh.packet-max-age-seconds:86400}")
    private long maxAgeSeconds;

    public IngestResult ingest(MeshPacket packet, String bridgeNodeId, int hopCount) {
        try {

            String packetHash = hybridCryptoService.hashCiphertext(
                    packet.getCiphertext()
            );

            // ---- Idempotency gate ----
            if (!idempotencyService.claim(packetHash))
            {
                log.info(
                        "DUPLICATE packet {} from bridge {} — dropped",
                        packetHash.substring(0, 12) + "...", bridgeNodeId
                );

                return IngestResult.duplicate(packetHash);
            }

            // ---- Decrypt ----
            PaymentInstruction instruction;
            try {

                instruction = hybridCryptoService.decrypt(
                        packet.getCiphertext()
                );

            } catch (Exception e) {
                log.warn(
                        "Decryption failed for packet {}: {}",
                        packetHash.substring(0, 12) + "...",
                        e.getMessage()
                );

                return IngestResult.invalid(
                        packetHash, "decryption_failed"
                );
            }

            // ---- Freshness check (replay protection) ----
            long ageSeconds = (Instant.now().toEpochMilli() - instruction.getSignedAt()) / 1000;

            if (ageSeconds > maxAgeSeconds) {
                log.warn(
                        "Packet {} too old ({}s), rejected",
                        packetHash.substring(0, 12) + "...", ageSeconds
                );

                return IngestResult.invalid(
                        packetHash, "stale_packet"
                );
            }

            if (ageSeconds < -300) { // small clock-skew tolerance
                return IngestResult.invalid(
                        packetHash, "future_dated"
                );

            }

            // ---- Settle ----
            Transactions tx = settlementService.settle(
                    instruction,
                    packetHash,
                    bridgeNodeId,
                    hopCount
            );
            return IngestResult.settled(
                    packetHash,
                    tx
            );

        } catch (Exception e) {
            log.error(
                    "Ingestion error: {}",
                    e.getMessage(),
                    e
            );

            return IngestResult.invalid(
                    "?",
                    "internal_error: "
                            + e.getMessage()
            );
        }
    }

    public record IngestResult(
            String outcome,
            String packetHash,
            String reason,
            Long transactionId
    ) {
        public static IngestResult settled(
                String hash,
                Transactions tx
        ) {
            return new IngestResult(
                    "SETTLED",
                    hash,
                    null,
                    tx.getId()
            );
        }
        public static IngestResult duplicate(String hash)
        {
            return new IngestResult(
                    "DUPLICATE_DROPPED",
                    hash,
                    null,
                    null
            );
        }
        public static IngestResult invalid(
                String hash,
                String reason
        ) {
            return new IngestResult(
                    "INVALID",
                    hash,
                    reason,
                    null
            );
        }
    }

}
