package ma.dev7hd.fileservice.repositories;

import ma.dev7hd.fileservice.entities.PaymentInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentInvoiceRepository extends JpaRepository<PaymentInvoice, UUID> {
    Optional<PaymentInvoice> findByPaymentId(UUID paymentId);
}
