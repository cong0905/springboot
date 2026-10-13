package vn.techshop.payment;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.order.OrderHistory;
import vn.techshop.order.OrderHistoryRepository;
import vn.techshop.order.OrderRepository;
import vn.techshop.order.PurchaseOrder;
import vn.techshop.shared.BusinessException;

@Service
@Transactional(readOnly = true)
public class PaymentService {
    public record PaymentResult(
        String transactionCode,
        String paymentUrl,
        String gateway
    ) {}

    private final PaymentTransactionRepository transactions;
    private final OrderRepository orders;
    private final OrderHistoryRepository histories;
    private final Clock clock;
    private final org.springframework.beans.factory.ObjectProvider<RefundService> refundsProvider;

    @Value("${app.payment.vnpay.tmn-code:}")
    private String vnpTmnCode = "";

    @Value("${app.payment.vnpay.hash-secret:}")
    private String vnpHashSecret = "";

    @Value("${app.payment.vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    @Value("${app.payment.vnpay.return-url:http://localhost:8080/payment/vnpay/return}")
    private String vnpReturnUrl = "http://localhost:8080/payment/vnpay/return";

    @Value("${app.payment.mock.enabled:false}")
    private boolean mockEnabled=false;

    public PaymentService(
        PaymentTransactionRepository transactions,
        OrderRepository orders,
        OrderHistoryRepository histories,
        Clock clock
    ) {
        this(transactions, orders, histories, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentService(
        PaymentTransactionRepository transactions,
        OrderRepository orders,
        OrderHistoryRepository histories,
        Clock clock,
        org.springframework.beans.factory.ObjectProvider<RefundService> refundsProvider
    ) {
        this.transactions = transactions;
        this.orders = orders;
        this.histories = histories;
        this.clock = clock;
        this.refundsProvider = refundsProvider;
    }

    @Transactional
    public PaymentResult initiatePayment(PurchaseOrder order, String gateway, String clientIp) {
        String cleanGateway = (gateway == null || gateway.isBlank()) ? "COD" : gateway.trim().toUpperCase();
        if (!Set.of("COD","MOCK","VNPAY").contains(cleanGateway)) throw BusinessException.invalid("Phương thức thanh toán không hợp lệ.");
        if("MOCK".equals(cleanGateway)&&!mockEnabled)throw BusinessException.invalid("Thanh toán mock chỉ được bật trong demo/test.");
        if ("VNPAY".equals(cleanGateway) && (vnpTmnCode.isBlank() || vnpHashSecret.isBlank())) throw BusinessException.invalid("VNPay chưa được cấu hình TMN code và hash secret.");
        if ("COD".equalsIgnoreCase(cleanGateway)) {
            return new PaymentResult(null, null, "COD");
        }

        String txnCode = "TXN-" + order.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = clock.instant();

        PaymentTransaction txn = new PaymentTransaction();
        txn.setOrderId(order.getId());
        txn.setTransactionCode(txnCode);
        txn.setPaymentGateway(cleanGateway);
        txn.setClientIp(clientIp);
        txn.setAmount(order.getTotalAmount());
        txn.setStatus("PENDING");
        txn.setCreatedAt(now);
        txn.setUpdatedAt(now);
        transactions.saveAndFlush(txn);

        order.setPaymentMethod(cleanGateway);
        orders.saveAndFlush(order);

        String paymentUrl;
        if ("MOCK".equalsIgnoreCase(cleanGateway)) {
            paymentUrl = "/payment/mock-pay/" + txnCode;
        } else {
            paymentUrl = buildVNPayUrl(txnCode, order.getTotalAmount(), clientIp != null ? clientIp : "127.0.0.1", now);
        }

        return new PaymentResult(txnCode, paymentUrl, cleanGateway);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentTransaction completePayment(String transactionCode, String responseCode, String bankCode, String message) {
        Long orderId=transactions.orderIdByCode(transactionCode).orElseThrow(BusinessException::missing);
        // Read a scalar first; a preloaded versioned transaction can be stale after waiting for this lock.
        var order = orders.lockById(orderId).orElseThrow(BusinessException::missing);
        var txn = transactions.lockByCode(transactionCode).orElseThrow(BusinessException::missing);
        if ("SUCCESS".equals(txn.getStatus())) return txn;
        if ("CANCELLED".equals(order.getStatus())) {
            if ("00".equals(responseCode) || "SUCCESS".equalsIgnoreCase(responseCode)) {
                var refundService = refundsProvider.getIfAvailable();
                if (refundService != null) {
                    refundService.recordLatePaymentReconciliation(order.getId(), txn.getId(), txn.getAmount(), txn.getPaymentGateway());
                }
                txn.setStatus("SUCCESS"); txn.setUpdatedAt(clock.instant());
                transactions.saveAndFlush(txn);
            }
            throw BusinessException.conflict("PAYMENT_ORDER_CANCELLED","Đơn đã hủy cần đối soát hoàn tiền.");
        }

        Instant now = clock.instant();
        boolean isSuccess = "00".equals(responseCode) || "SUCCESS".equalsIgnoreCase(responseCode);

        txn.setStatus(isSuccess ? "SUCCESS" : "FAILED");
        txn.setBankCode(bankCode);
        txn.setResponseMessage(message);
        txn.setUpdatedAt(now);
        transactions.saveAndFlush(txn);

        if (isSuccess) {
            order.setPaymentStatus("PAID");
            String previous=order.getStatus();
            if ("PENDING".equals(previous)) order.setStatus("CONFIRMED");
            order.setUpdatedAt(now);
            orders.saveAndFlush(order);

            OrderHistory history = new OrderHistory();
            history.setOrderId(order.getId());
            history.setActorUserId(order.getUserId());
            history.setFromStatus(previous);
            history.setToStatus(order.getStatus());
            history.setReason("Đã thanh toán online qua " + txn.getPaymentGateway() + " (Mã GD: " + txn.getTransactionCode() + ")");
            history.setOrderVersion(order.getVersion());
            history.setOccurredAt(now);
            histories.saveAndFlush(history);
        }

        return txn;
    }

    public Optional<PaymentTransaction> findByCode(String transactionCode) {
        if (transactionCode == null || transactionCode.isBlank()) return Optional.empty();
        return transactions.findByTransactionCode(transactionCode.trim());
    }

    public Optional<PaymentTransaction> findLatestByOrderId(Long orderId) {
        if (orderId == null) return Optional.empty();
        var list = transactions.findByOrderIdOrderByCreatedAtDesc(orderId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }
    public String resumeUrl(PurchaseOrder order) {
        if("PAID".equals(order.getPaymentStatus())||!Set.of("PENDING","CONFIRMED").contains(order.getStatus()))return null;
        var txn=transactions.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
            .filter(t->!"SUCCESS".equals(t.getStatus())&&t.getPaymentGateway().equals(order.getPaymentMethod())).findFirst().orElse(null);
        if(txn==null)return null;
        if("MOCK".equals(txn.getPaymentGateway()))return mockEnabled?"/payment/mock-pay/"+txn.getTransactionCode():null;
        if("VNPAY".equals(txn.getPaymentGateway())&&!vnpTmnCode.isBlank()&&!vnpHashSecret.isBlank())
            return buildVNPayUrl(txn.getTransactionCode(),txn.getAmount(),txn.getClientIp()==null?"127.0.0.1":txn.getClientIp(),clock.instant());
        return null;
    }

    public boolean verifyVNPay(Map<String,String> params) {
        if(vnpHashSecret.isBlank() || !Objects.equals(vnpTmnCode,params.get("vnp_TmnCode")))return false;
        String signature=params.get("vnp_SecureHash");
        if(signature==null||!signature.matches("[a-fA-F0-9]{128}"))return false;
        var data=new TreeMap<String,String>();
        params.forEach((k,v)->{if(k.startsWith("vnp_")&&!k.equals("vnp_SecureHash")&&!k.equals("vnp_SecureHashType")&&v!=null&&!v.isEmpty())data.put(k,v);});
        String encoded=data.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));
        return MessageDigest.isEqual(signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),hmacSHA512(vnpHashSecret,encoded).getBytes(StandardCharsets.US_ASCII));
    }
    @Transactional public Map<String,String> vnPayIpn(Map<String,String> params) {
        if(!verifyVNPay(params))return Map.of("RspCode","97","Message","Invalid signature");
        String code=params.get("vnp_TxnRef");var orderId=transactions.orderIdByCode(code);
        if(orderId.isEmpty())return Map.of("RspCode","01","Message","Order not found");
        var order=orders.lockById(orderId.get()).orElseThrow(BusinessException::missing);
        var txn=transactions.lockByCode(code).orElseThrow(BusinessException::missing);
        if(!"VNPAY".equals(txn.getPaymentGateway()))return Map.of("RspCode","01","Message","Order not found");
        try {if(txn.getAmount().multiply(BigDecimal.valueOf(100)).compareTo(new BigDecimal(params.get("vnp_Amount")))!=0)return Map.of("RspCode","04","Message","Invalid amount");}
        catch(RuntimeException ex) {return Map.of("RspCode","04","Message","Invalid amount");}
        if(!"PENDING".equals(txn.getStatus()))return Map.of("RspCode","02","Message","Already confirmed");
        String result="00".equals(params.get("vnp_ResponseCode"))&&"00".equals(params.get("vnp_TransactionStatus"))?"00":"FAILED";
        if("CANCELLED".equals(order.getStatus())) {
            if("00".equals(result)) {
                // Same transaction as the held order/transaction locks; do not open a second connection.
                var refunds=refundsProvider.getIfAvailable();
                if(refunds!=null)refunds.recordLatePaymentReconciliation(order.getId(),txn.getId(),txn.getAmount(),txn.getPaymentGateway());
                txn.setStatus("SUCCESS");txn.setUpdatedAt(clock.instant());transactions.saveAndFlush(txn);
            }
            return Map.of("RspCode","02","Message","Cancelled order requires reconciliation");
        }
        completePayment(txn.getTransactionCode(),result,params.get("vnp_BankCode"),"Verified VNPay IPN");
        return Map.of("RspCode","00","Message","Confirm Success");
    }

    private String buildVNPayUrl(String txnRef, BigDecimal amount, String clientIp, Instant now) {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        // VNPay amount is multiplied by 100
        vnpParams.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).setScale(0).toPlainString());
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + txnRef);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", clientIp);

        String dateStr = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(now);
        vnpParams.put("vnp_CreateDate", dateStr);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v != null && !v.isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }
                hashData.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(k, StandardCharsets.US_ASCII)).append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
            }
        }

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return vnpUrl + "?" + query;
    }

    public static String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac hmac512 = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception ex) {
            throw new IllegalStateException("Lỗi mã hóa HMAC-SHA512", ex);
        }
    }
}
