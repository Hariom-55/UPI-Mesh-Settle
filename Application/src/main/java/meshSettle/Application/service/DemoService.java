package meshSettle.Application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import meshSettle.Application.component.ServerKeyHolder;
import meshSettle.Application.model.Account;
import meshSettle.Application.model.MeshPacket;
import meshSettle.Application.model.PaymentInstruction;
import meshSettle.Application.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoService
{
    private static final Logger log = LoggerFactory.getLogger(
            DemoService.class
    );

    private final AccountRepository accountRepository;
    private final HybridCryptoService hybridCryptoService;
    private final ServerKeyHolder serverKeyHolder;

    @PostConstruct
    public void seedAccounts()
    {
        if(accountRepository.count() == 0)
        {
            accountRepository.save(new Account("alice@demo", "Alice",   new BigDecimal("5000.00")));
            accountRepository.save(new Account("bob@demo",   "Bob",     new BigDecimal("1000.00")));
            accountRepository.save(new Account("carol@demo", "Carol",   new BigDecimal("2500.00")));
            accountRepository.save(new Account("dave@demo",  "Dave",    new BigDecimal("500.00")));

            log.info(
                    "Seeded 4 demo accounts"
            );

        }
    }

    public MeshPacket createPacket(
            String senderVpa,
            String receiverVpa,
            BigDecimal amount,
            String pin,
            int ttl
    ) throws Exception
    {
        PaymentInstruction instruction = new PaymentInstruction(
                senderVpa,
                receiverVpa,
                amount,
                sha256Hex(pin),
                UUID.randomUUID().toString(),       // nonce — guarantees uniqueness
                Instant.now().toEpochMilli()        // signedAt — for freshness check
        );

        String ciphertext = hybridCryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        MeshPacket packet = new MeshPacket();
        packet.setPacketId(UUID.randomUUID().toString());
        packet.setTtl(ttl);
        packet.setCreatedAt(Instant.now().toEpochMilli());
        packet.setCiphertext(ciphertext);
        return packet;
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

}
