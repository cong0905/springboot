package vn.techshop.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @org.springframework.data.jpa.repository.Query("select t.orderId from PaymentTransaction t where t.transactionCode=:code")
    Optional<Long> orderIdByCode(String code);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from PaymentTransaction t where t.transactionCode=:code")
    Optional<PaymentTransaction> lockByCode(String code);
    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
