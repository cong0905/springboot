package vn.techshop.payment;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.techshop.order.OrderService;
import vn.techshop.shared.BusinessException;

@Controller
public class PaymentController {
    public record PaymentView(String transactionCode,String paymentGateway,java.math.BigDecimal amount,String status) {}
    private final PaymentService payments;
    private final OrderService orders;
    @org.springframework.beans.factory.annotation.Value("${app.payment.mock.enabled:false}") private boolean mockEnabled;

    public PaymentController(PaymentService payments, OrderService orders) {
        this.payments = payments;
        this.orders = orders;
    }

    @GetMapping("/payment/mock-pay/{code}")
    public String mockPayPage(@PathVariable String code, @org.springframework.security.core.annotation.AuthenticationPrincipal vn.techshop.identity.TechPrincipal user, Model model) {
        if(!mockEnabled)throw BusinessException.missing();
        var txn = payments.findByCode(code)
            .orElseThrow(BusinessException::missing);
        orders.detail(txn.getOrderId(),user.id(),false);
        if(!"MOCK".equals(txn.getPaymentGateway()))throw BusinessException.invalid("Không phải giao dịch demo.");
        model.addAttribute("txn", new PaymentView(txn.getTransactionCode(),txn.getPaymentGateway(),txn.getAmount(),txn.getStatus()));
        return "mock-payment";
    }

    @PostMapping("/payment/mock-pay/{code}/confirm")
    public String mockPayConfirm(
        @PathVariable String code,
        @org.springframework.security.core.annotation.AuthenticationPrincipal vn.techshop.identity.TechPrincipal user,
        @RequestParam(defaultValue = "00") String responseCode,
        @RequestParam(defaultValue = "TECHSHOP_BANK") String bankCode
    ) {
        if(!mockEnabled)throw BusinessException.missing();
        var original=payments.findByCode(code).orElseThrow(BusinessException::missing);
        orders.detail(original.getOrderId(),user.id(),false);
        if(!"MOCK".equals(original.getPaymentGateway()))throw BusinessException.invalid("Không phải giao dịch demo.");
        var txn = payments.completePayment(code, responseCode, bankCode, "Mô phỏng thanh toán trực tuyến " + ("00".equals(responseCode) ? "thành công" : "thất bại"));
        return "redirect:/orders/" + txn.getOrderId();
    }

    @GetMapping("/payment/vnpay/return")
    public String vnpayReturn(HttpServletRequest request) {
        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String bankCode = request.getParameter("vnp_BankCode");
        String message = "VNPay phản hồi mã: " + responseCode;

        var params=new java.util.HashMap<String,String>();request.getParameterMap().forEach((k,v)->{if(v.length==1)params.put(k,v[0]);});
        if(!payments.verifyVNPay(params))throw BusinessException.invalid("Chữ ký VNPay không hợp lệ.");
        var txn = payments.findByCode(txnRef).orElseThrow(BusinessException::missing);
        return "redirect:/orders/" + txn.getOrderId();
    }

    @ResponseBody
    @GetMapping("/api/v1/payment/vnpay/ipn")
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        var params=new java.util.HashMap<String,String>();request.getParameterMap().forEach((k,v)->{if(v.length==1)params.put(k,v[0]);});
        return payments.vnPayIpn(params);
    }
}
