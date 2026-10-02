package meshSettle.Application.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transactions",
indexes = {
        @Index(name = "idx_packet_hash",
                columnList = "packetHash",
                unique = true)
})

public class Transactions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String packetHash;

    @Column(nullable = false)
    private String senderVpa;

    @Column(nullable = false)
    private String receiverVpa;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant signedAt;

    @Column(nullable = false)
    private Instant settledAt;

    @Column(nullable = false)
    private String bridgeNodeId;

    @Column(nullable = false)
    private int hopCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
}
