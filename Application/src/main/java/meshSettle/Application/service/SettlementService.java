package meshSettle.Application.service;

import lombok.RequiredArgsConstructor;
import meshSettle.Application.model.Account;
import meshSettle.Application.model.PaymentInstruction;
import meshSettle.Application.model.Status;
import meshSettle.Application.model.Transactions;
import meshSettle.Application.repository.AccountRepository;
import meshSettle.Application.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;


@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transactions settle(
            PaymentInstruction instruction,
            String packetHash,
            String bridgeNodeId,
            int hopCount
    ) {

        Account sender = accountRepository.findById(
                instruction.getSenderVpa()
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                        "Unknown sender VPA: " + instruction.getSenderVpa())
                );

        Account receiver = accountRepository.findById(
                instruction.getReceiverVpa()
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                        "Unknown receiver VPA: " + instruction.getReceiverVpa())
                );

        BigDecimal amount = instruction.getAmount();

        if (amount.signum() <= 0)
        {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        if (sender.getBalance().compareTo(amount) < 0)
        {
            log.warn(
                    "Insufficient balance: {} has ₹{}, tried to send ₹{}",
                    sender.getVpa(),
                    sender.getBalance(),
                    amount
            );

            return recordRejected(
                    instruction,
                    packetHash,
                    bridgeNodeId,
                    hopCount
            );
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transactions tx = new Transactions();
        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.getSenderVpa());
        tx.setReceiverVpa(instruction.getReceiverVpa());
        tx.setAmount(amount);
        tx.setSignedAt(Instant.ofEpochMilli(instruction.getSignedAt()));
        tx.setSettledAt(Instant.now());
        tx.setBridgeNodeId(bridgeNodeId);
        tx.setHopCount(hopCount);
        tx.setStatus(Status.SETTLED);
        transactionRepository.save(tx);

        log.info(
                "SETTLED ₹{} from {} to {} (packetHash={}, bridge={}, hops={})",
                amount, sender.getVpa(),
                receiver.getVpa(),
                packetHash.substring(0, 12) + "...",
                bridgeNodeId,
                hopCount
        );

        return tx;
    }

    private Transactions recordRejected(
            PaymentInstruction instruction,
            String packetHash,
            String bridgeNodeId,
            int hopCount
    )
    {
        Transactions tx = new Transactions();
        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.getSenderVpa());
        tx.setReceiverVpa(instruction.getReceiverVpa());
        tx.setAmount(instruction.getAmount());
        tx.setSignedAt(Instant.ofEpochMilli(instruction.getSignedAt()));
        tx.setSettledAt(Instant.now());
        tx.setBridgeNodeId(bridgeNodeId);
        tx.setHopCount(hopCount);
        tx.setStatus(Status.REJECTED);
        return transactionRepository.save(tx);
    }
}
