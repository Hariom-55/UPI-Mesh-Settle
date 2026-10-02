package meshSettle.Application.repository;

import meshSettle.Application.model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transactions, Long>
{
    List<Transactions> findTop20ByOrderByIdDesc();

    boolean existsByPacketHash(String packetHash);
}
