package meshSettle.Application.model;

import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PaymentInstruction
{
    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;
    private String pinHash;
    private String nonce;
    private Long signedAt;

    public PaymentInstruction(String senderVpa, String receiverVpa, BigDecimal amount,
                              String pinHash, String nonce, Long signedAt) {
        this.senderVpa = senderVpa;
        this.receiverVpa = receiverVpa;
        this.amount = amount;
        this.pinHash = pinHash;
        this.nonce = nonce;
        this.signedAt = signedAt;
    }
}
