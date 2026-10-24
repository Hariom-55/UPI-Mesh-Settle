package meshSettle.Application.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;


@Data
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account
{
    @Id
    private String vpa;  //Virtual payment Address

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version  //Optimistic Locking
    private Long version;

    public Account(String vpa, String holderName, BigDecimal balance)
    {
        this.vpa = vpa;
        this.holderName=holderName;
        this.balance=balance;
    }
}
