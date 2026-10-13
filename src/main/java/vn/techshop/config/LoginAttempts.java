package vn.techshop.config;

import java.time.Clock;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded per-instance throttle with independent IP and Account limits. */
final class LoginAttempts {
    record Counter(long started, AtomicInteger failures) {}

    static final long WINDOW_MS = 900_000L; // 15 minutes
    static final int MAX_ACCOUNT_FAILURES = 5;
    static final int MAX_IP_FAILURES = 20;
    static final int MAX_ENTRIES = 10_000;

    private final ConcurrentHashMap<String, Counter> accountAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> ipAttempts = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    static String normalizeUser(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    boolean blocked(String ip, String username) {
        long now = clock.millis();
        String u = normalizeUser(username);

        if (!u.isEmpty()) {
            Counter ac = accountAttempts.get(u);
            if (ac != null && now - ac.started() < WINDOW_MS && ac.failures().get() >= MAX_ACCOUNT_FAILURES) {
                return true;
            }
        }

        if (ip != null && !ip.isBlank()) {
            Counter ipc = ipAttempts.get(ip.trim());
            if (ipc != null && now - ipc.started() < WINDOW_MS && ipc.failures().get() >= MAX_IP_FAILURES) {
                return true;
            }
        }

        return false;
    }

    void failed(String ip, String username) {
        long now = clock.millis();
        String u = normalizeUser(username);

        if (!u.isEmpty()) {
            recordFailure(accountAttempts, u, now);
        }
        if (ip != null && !ip.isBlank()) {
            recordFailure(ipAttempts, ip.trim(), now);
        }
    }

    void success(String ip, String username) {
        String u = normalizeUser(username);
        if (!u.isEmpty()) {
            accountAttempts.remove(u);
        }
        if (ip != null && !ip.isBlank()) {
            ipAttempts.remove(ip.trim());
        }
    }

    private void recordFailure(ConcurrentHashMap<String, Counter> map, String key, long now) {
        if (map.size() >= MAX_ENTRIES) {
            map.entrySet().removeIf(e -> now - e.getValue().started() >= WINDOW_MS);
            if (map.size() >= MAX_ENTRIES && !map.containsKey(key)) {
                // Evict the oldest entry to prevent denial-of-throttle / unmetered brute force
                map.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().started()))
                    .ifPresent(oldest -> map.remove(oldest.getKey()));
            }
        }

        map.compute(key, (k, v) -> {
            if (v == null || now - v.started() >= WINDOW_MS) {
                return new Counter(now, new AtomicInteger(1));
            }
            v.failures().incrementAndGet();
            return v;
        });
    }

    ConcurrentHashMap<String, Counter> getAccountAttempts() {
        return accountAttempts;
    }

    ConcurrentHashMap<String, Counter> getIpAttempts() {
        return ipAttempts;
    }
}
