package vn.techshop.config;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded per-instance throttle. Distributed deployments need a shared limiter. */
final class LoginAttempts {
    private record Counter(long started, AtomicInteger failures) {}
    private final ConcurrentHashMap<String, Counter> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    LoginAttempts(Clock clock) { this.clock=clock; }
    String key(String ip, String username) { return ip + "|" + (username==null?"":username.strip().toLowerCase(Locale.ROOT)); }
    boolean blocked(String key) { var c=attempts.get(key); return c!=null && clock.millis()-c.started()<900000 && c.failures().get()>=10; }
    void failed(String key) {
        long now=clock.millis();
        attempts.entrySet().removeIf(e -> now-e.getValue().started()>=900000);
        if (attempts.size()>=10000 && !attempts.containsKey(key)) return;
        attempts.compute(key,(k,v) -> { if(v==null || now-v.started()>=900000) return new Counter(now,new AtomicInteger(1)); v.failures().incrementAndGet(); return v; });
    }
    void success(String key) { attempts.remove(key); }
}
