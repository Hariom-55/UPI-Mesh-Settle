package meshSettle.Application;


import meshSettle.Application.component.ServerKeyHolder;
import meshSettle.Application.model.MeshPacket;
import meshSettle.Application.model.PaymentInstruction;
import meshSettle.Application.repository.AccountRepository;
import meshSettle.Application.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BridgeIngestionServiceTest
{
    @Autowired private BridgeIngestionService bridge;
    @Autowired private IdempotencyService idempotency;
    @Autowired private AccountRepository accounts;
    @Autowired private HybridCryptoService crypto;
    @Autowired private ServerKeyHolder serverKey;

    @BeforeEach
    void clear() {
        idempotency.clear();
    }

    @Test
    void insufficientBalanceIsRejectedNotSettled() throws Exception {

        BigDecimal daveBefore = accounts.findById("dave@demo").
                orElseThrow().
                getBalance();

        BigDecimal aliceBefore = accounts.findById("alice@demo")
                .orElseThrow().
                getBalance();

        PaymentInstruction instruction = new PaymentInstruction(
                "dave@demo", "alice@demo",
                new BigDecimal("999999.00"),
                "pinhash",
                UUID.randomUUID().toString(),
                System.currentTimeMillis()
        );

        MeshPacket packet = wrap(instruction);
        BridgeIngestionService.IngestResult result = bridge.ingest(
                packet,
                "bridge-test",
                1
        );


        assertEquals(
                "SETTLED", result.outcome(),
                "pipeline accepts the packet for processing"
        );

        assertNotNull(result.transactionId());

        BigDecimal daveAfter = accounts.findById("dave@demo")
                .orElseThrow()
                .getBalance();

        BigDecimal aliceAfter = accounts.findById("alice@demo")
                .orElseThrow()
                .getBalance();

        assertEquals(
                daveBefore,
                daveAfter,
                "sender balance must not move on insufficient funds"
        );

        assertEquals(
                aliceBefore,
                aliceAfter,
                "receiver balance must not move on insufficient funds"
        );
    }

    @Test
    void stalePacketIsRejectedAsReplay() throws Exception {

        BigDecimal aliceBefore = accounts.findById("alice@demo")
                .orElseThrow()
                .getBalance();

        BigDecimal bobBefore = accounts.findById("bob@demo")
                .orElseThrow()
                .getBalance();


        long staleTimestamp = Instant.now().minus(25, ChronoUnit.HOURS).toEpochMilli();

        PaymentInstruction instruction = new PaymentInstruction(
                "alice@demo",
                "bob@demo",
                new BigDecimal("75.00"),
                "pinhash",
                UUID.randomUUID().toString(),
                staleTimestamp
        );

        MeshPacket packet = wrap(instruction);

        BridgeIngestionService.IngestResult result = bridge.ingest(
                packet,
                "bridge-test",
                1
        );

        assertEquals("INVALID", result.outcome());
        assertEquals("stale_packet", result.reason());


        assertEquals(
                aliceBefore,
                accounts.findById("alice@demo").orElseThrow().getBalance()
        );

        assertEquals(
                bobBefore,
                accounts.findById("bob@demo").orElseThrow().getBalance()
        );
    }

    private MeshPacket wrap(PaymentInstruction instruction) throws Exception
    {
        String ciphertext = crypto.encrypt(
                instruction,
                serverKey.getPublicKey()
        );

        MeshPacket packet = new MeshPacket();

        packet.setPacketId(UUID.randomUUID().toString());

        packet.setTtl(5);

        packet.setCreatedAt(System.currentTimeMillis());

        packet.setCiphertext(ciphertext);

        return packet;
    }
}
