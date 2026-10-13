package vn.techshop.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptsTest {

    private AtomicLong currentTime;
    private Clock mutableClock;
    private LoginAttempts attempts;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicLong(1_000_000_000L);
        mutableClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(currentTime.get());
            }

            @Override
            public long millis() {
                return currentTime.get();
            }
        };
        attempts = new LoginAttempts(mutableClock);
    }

    @Test
    void testMultipleIpsAgainstSameAccountTriggersLimit() {
        String targetAccount = "admin@techshop.vn";

        // 5 failures from 5 different IPs
        for (int i = 1; i <= 5; i++) {
            assertThat(attempts.blocked("192.168.1." + i, targetAccount)).isFalse();
            attempts.failed("192.168.1." + i, targetAccount);
        }

        // Account reached 5 failures -> any subsequent attempt for this account from a new IP is blocked
        assertThat(attempts.blocked("192.168.1.99", targetAccount)).isTrue();
        // But another unaffected account from that same new IP is NOT blocked
        assertThat(attempts.blocked("192.168.1.99", "otheruser@techshop.vn")).isFalse();
    }

    @Test
    void testMultipleAccountsFromSameIpTriggersIpLimit() {
        String attackerIp = "10.10.10.10";

        // 20 failures on 20 different usernames from the same IP (spraying)
        for (int i = 1; i <= 20; i++) {
            assertThat(attempts.blocked(attackerIp, "user" + i)).isFalse();
            attempts.failed(attackerIp, "user" + i);
        }

        // IP reached 20 failures -> blocked for any username
        assertThat(attempts.blocked(attackerIp, "newuser")).isTrue();
        // But another innocent IP trying the same newuser is NOT blocked
        assertThat(attempts.blocked("10.10.10.99", "newuser")).isFalse();
    }

    @Test
    void testAccountRecoversAfterExpiry() {
        String account = "customer@techshop.vn";
        String ip = "172.16.0.1";

        for (int i = 0; i < 5; i++) {
            attempts.failed(ip, account);
        }
        assertThat(attempts.blocked(ip, account)).isTrue();

        // Advance time past 15 minutes window (900_000 ms)
        currentTime.addAndGet(LoginAttempts.WINDOW_MS + 1000);

        // Account is now unlocked
        assertThat(attempts.blocked(ip, account)).isFalse();
    }

    @Test
    void testSuccessfulLoginClearsFailures() {
        String account = "customer2@techshop.vn";
        String ip = "172.16.0.2";

        for (int i = 0; i < 4; i++) {
            attempts.failed(ip, account);
        }
        assertThat(attempts.blocked(ip, account)).isFalse();

        // Success clears the failure count
        attempts.success(ip, account);

        // Can fail again without hitting the threshold prematurely
        for (int i = 0; i < 4; i++) {
            attempts.failed(ip, account);
        }
        assertThat(attempts.blocked(ip, account)).isFalse();
    }

    @Test
    void testFullCacheEvictionDoesNotAllowUnmeteredFailures() {
        // Pre-fill cache up to MAX_ENTRIES
        for (int i = 0; i < LoginAttempts.MAX_ENTRIES; i++) {
            attempts.getAccountAttempts().put("prefill_user_" + i, new LoginAttempts.Counter(currentTime.get() - 100, new java.util.concurrent.atomic.AtomicInteger(1)));
        }
        assertThat(attempts.getAccountAttempts().size()).isEqualTo(LoginAttempts.MAX_ENTRIES);

        // A new target account fails
        String newAccount = "victim@techshop.vn";
        for (int i = 0; i < 5; i++) {
            attempts.failed("1.2.3.4", newAccount);
        }

        // The new account MUST still be tracked and blocked (old entries evicted)
        assertThat(attempts.blocked("1.2.3.4", newAccount)).isTrue();
    }
}
