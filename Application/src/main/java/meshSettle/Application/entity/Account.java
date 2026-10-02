package meshSettle.Application.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "accounts")
public class Account
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private String vpa;  //Virtual payment Address

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version  //Optimistic Locking
    private Long version;
}
