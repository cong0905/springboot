package vn.techshop.order;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LookupOrderController {
    private final OrderService orderService;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.config.ClientIpResolver clientIps;

    private record WindowCounter(long windowStart, AtomicInteger count) {}
    private final ConcurrentHashMap<String, WindowCounter> ipAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WindowCounter> identifierAttempts = new ConcurrentHashMap<>();

    private static final long WINDOW_MS = 300_000L; // 5 minutes
    private static final int MAX_IP_ATTEMPTS = 15;
    private static final int MAX_IDENTIFIER_FAILURES = 5;
    private static final int MAX_CACHE_ENTRIES = 10_000;

    public LookupOrderController(OrderService orderService, Clock clock) {
        this.orderService = orderService;
        this.clock = clock;
    }

    @GetMapping("/order-lookup")
    public String lookupPage(Model model) {
        return "order-lookup";
    }

    @PostMapping("/order-lookup")
    public String searchOrder(
        @RequestParam(required = false) String orderCode,
        @RequestParam(required = false) String phone,
        HttpServletRequest request,
        HttpServletResponse response,
        Model model
    ) {
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("phone", phone);
        model.addAttribute("searched", true);

        String ip = clientIps == null ? request.getRemoteAddr() : clientIps.resolve(request);
        String identifierKey = ((orderCode != null ? orderCode.strip() : "") + "|" + (phone != null ? phone.strip() : "")).toLowerCase(Locale.ROOT);

        long now = clock.millis();
        synchronized (this) {
            cleanExpired(now);
            var ipCounter = ipAttempts.get(ip);
            var idCounter = identifierAttempts.get(identifierKey);
            boolean idBlocked = idCounter != null && idCounter.count().get() >= MAX_IDENTIFIER_FAILURES;
            boolean blocked = idBlocked || ipCounter != null && ipCounter.count().get() >= MAX_IP_ATTEMPTS
                || ipCounter == null && ipAttempts.size() >= MAX_CACHE_ENTRIES
                || idCounter == null && identifierAttempts.size() >= MAX_CACHE_ENTRIES;
            if (blocked) {
                response.setStatus(429);
                response.setHeader("Retry-After", "300");
                model.addAttribute("error", idBlocked
                    ? "Bạn đã thử tra cứu quá nhiều lần cho thông tin này. Vui lòng thử lại sau ít phút."
                    : "Bạn đã thử tra cứu quá nhiều lần. Vui lòng thử lại sau ít phút.");
                return "order-lookup";
            }
            recordAttempt(ipAttempts, ip, now);
            recordAttempt(identifierAttempts, identifierKey, now);
        }

        if (orderCode == null || orderCode.isBlank() || phone == null || phone.isBlank()) {
            model.addAttribute("error", "Thông tin tra cứu không chính xác hoặc không tìm thấy đơn hàng.");
            return "order-lookup";
        }

        var found = orderService.lookup(orderCode, phone);
        if (found.isPresent()) {
            // Reset failure counter on success
            identifierAttempts.remove(identifierKey);
            model.addAttribute("order", found.get());
        } else {
            model.addAttribute("error", "Thông tin tra cứu không chính xác hoặc không tìm thấy đơn hàng.");
        }

        return "order-lookup";
    }

    private void recordAttempt(ConcurrentHashMap<String, WindowCounter> map, String key, long now) {
        if (key == null || key.isBlank()) return;
        if (map.size() >= MAX_CACHE_ENTRIES && !map.containsKey(key)) {
            cleanExpired(now);
            if (map.size() >= MAX_CACHE_ENTRIES) return;
        }
        map.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart() >= WINDOW_MS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            v.count().incrementAndGet();
            return v;
        });
    }

    private void cleanExpired(long now) {
        ipAttempts.entrySet().removeIf(e -> now - e.getValue().windowStart() >= WINDOW_MS);
        identifierAttempts.entrySet().removeIf(e -> now - e.getValue().windowStart() >= WINDOW_MS);
    }
}
