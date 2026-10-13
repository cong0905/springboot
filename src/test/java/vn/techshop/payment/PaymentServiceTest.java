package vn.techshop.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.techshop.order.OrderHistory;
import vn.techshop.order.OrderHistoryRepository;
import vn.techshop.order.OrderRepository;
import vn.techshop.order.PurchaseOrder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private PaymentTransactionRepository transactions;
    private OrderRepository orders;
    private OrderHistoryRepository histories;
    private Clock clock;
    private PaymentService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        transactions = Mockito.mock(PaymentTransactionRepository.class);
        orders = Mockito.mock(OrderRepository.class);
        histories = Mockito.mock(OrderHistoryRepository.class);
        now = Instant.parse("2026-10-01T10:00:00Z");
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        service = new PaymentService(transactions, orders, histories, clock);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"vnpTmnCode","TECHSHOP");
        org.springframework.test.util.ReflectionTestUtils.setField(service,"vnpHashSecret","test-signature-key-not-real");
        org.springframework.test.util.ReflectionTestUtils.setField(service,"mockEnabled",true);
    }

    @Test
    void codInitiateReturnsNullPaymentUrl() {
        PurchaseOrder order = new PurchaseOrder();
        order.setTotalAmount(new BigDecimal("500000"));

        var result = service.initiatePayment(order, "COD", "127.0.0.1");

        assertThat(result.gateway()).isEqualTo("COD");
        assertThat(result.paymentUrl()).isNull();
        assertThat(result.transactionCode()).isNull();
        verifyNoInteractions(transactions);
    }

    @Test
    void mockInitiateCreatesTransactionAndMockUrl() {
        PurchaseOrder order = new PurchaseOrder();
        order.setTotalAmount(new BigDecimal("1200000"));

        when(transactions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orders.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.initiatePayment(order, "MOCK", "127.0.0.1");

        assertThat(result.gateway()).isEqualTo("MOCK");
        assertThat(result.paymentUrl()).startsWith("/payment/mock-pay/TXN-");
        assertThat(result.transactionCode()).isNotBlank();
        assertThat(order.getPaymentMethod()).isEqualTo("MOCK");
        verify(transactions).saveAndFlush(any(PaymentTransaction.class));
    }

    @Test
    void vnpayInitiateCreatesUrlWithHmacSha512() {
        PurchaseOrder order = new PurchaseOrder();
        order.setTotalAmount(new BigDecimal("800000"));

        when(transactions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orders.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.initiatePayment(order, "VNPAY", "127.0.0.1");

        assertThat(result.gateway()).isEqualTo("VNPAY");
        assertThat(result.paymentUrl()).contains("vnp_SecureHash=");
        assertThat(result.paymentUrl()).contains("vnp_Amount=80000000");
    }

    @Test
    void completePaymentSuccessUpdatesOrderToConfirmedAndPaid() {
        String txnCode = "TXN-1-TEST1234";
        PaymentTransaction txn = new PaymentTransaction();
        txn.setOrderId(1L);
        txn.setTransactionCode(txnCode);
        txn.setPaymentGateway("MOCK");
        txn.setStatus("PENDING");

        PurchaseOrder order = new PurchaseOrder();
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");

        when(transactions.orderIdByCode(txnCode)).thenReturn(Optional.of(1L));
        when(transactions.lockByCode(txnCode)).thenReturn(Optional.of(txn));
        when(orders.lockById(1L)).thenReturn(Optional.of(order));
        when(transactions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orders.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var completed = service.completePayment(txnCode, "00", "NCB", "Giao dịch thành công");

        assertThat(completed.getStatus()).isEqualTo("SUCCESS");
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        verify(histories).saveAndFlush(any(OrderHistory.class));
    }

    @Test
    void completePaymentFailureKeepsOrderUnpaid() {
        String txnCode = "TXN-1-FAIL";
        PaymentTransaction txn = new PaymentTransaction();
        txn.setOrderId(1L);
        txn.setTransactionCode(txnCode);
        txn.setPaymentGateway("VNPAY");
        txn.setStatus("PENDING");

        PurchaseOrder order = new PurchaseOrder();
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");

        when(transactions.orderIdByCode(txnCode)).thenReturn(Optional.of(1L));
        when(transactions.lockByCode(txnCode)).thenReturn(Optional.of(txn));
        when(orders.lockById(1L)).thenReturn(Optional.of(order));

        var completed = service.completePayment(txnCode, "24", "NCB", "Khách hàng hủy giao dịch");

        assertThat(completed.getStatus()).isEqualTo("FAILED");
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getPaymentStatus()).isEqualTo("PENDING");
        verifyNoInteractions(histories);
    }

    @Test
    void findByCodeIsExactAndDoesNotFallbackToLatestOrderTxn() {
        String exactCode = "TXN-1-CORRECT";
        PaymentTransaction txn = new PaymentTransaction();
        txn.setTransactionCode(exactCode);

        when(transactions.findByTransactionCode(exactCode)).thenReturn(Optional.of(txn));
        when(transactions.findByTransactionCode("TXN-1-WRONG")).thenReturn(Optional.empty());

        assertThat(service.findByCode(exactCode)).isPresent().contains(txn);
        assertThat(service.findByCode("TXN-1-WRONG")).isEmpty();
        assertThat(service.findByCode("")).isEmpty();
        assertThat(service.findByCode(null)).isEmpty();
    }
}
