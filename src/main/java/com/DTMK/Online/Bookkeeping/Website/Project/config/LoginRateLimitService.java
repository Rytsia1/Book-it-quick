package com.DTMK.Online.Bookkeeping.Website.Project.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-IP rate limiter for the login endpoint.
 * <p>
 * Each client IP gets its own token bucket sized at 5 tokens with
 * a refill of 5 tokens every 15 minutes. Each call to
 * {@link #tryConsume(String)} consumes one token from the bucket
 * associated with the given IP. A successful consume means the IP
 * is allowed to make another login attempt; a failed consume means
 * the IP has had 5+ failed login attempts in the last 15 minutes
 * and the controller should return {@code 429 Too Many Requests}.
 * <p>
 * The {@code AuthController} is responsible for calling
 * {@code tryConsume} ONLY when a login attempt has FAILED (wrong
 * username/password). This way, legitimate logins don't drain the
 * bucket, and only brute-force attempts trigger the rate limit.
 *
 * <h2>Why Bucket4j</h2>
 * Bucket4j implements the token-bucket algorithm in-process. Each
 * bucket is a counter that refills at a configured rate; requests
 * "consume" tokens, and a request that finds the bucket empty is
 * rejected with a {@link ConsumptionProbe} that tells the caller how
 * long to wait. This is the industry-standard way to throttle APIs
 * without needing an external Redis or Hazelcast store.
 *
 * <h2>Why a plain {@code ConcurrentHashMap} instead of ProxyManager</h2>
 * Bucket4j 8.x's high-level {@code ProxyManager} API is designed for
 * distributed scenarios (Redis, JCache) and adds a layer of
 * indirection we don't need for a single-instance Spring Boot app.
 * A {@code ConcurrentHashMap<String, Bucket>} gives us the same
 * thread-safe lazy-init + atomic token-consumption with simpler code
 * and zero extra dependencies.
 *
 * <h2>Memory bounds</h2>
 * Each bucket is ~200 bytes. The map grows unbounded with unique IPs
 * \u2014 a deliberate attacker hitting the endpoint from millions of
 * IPs could OOM us. In practice a single-instance app sees at most
 * a few thousand unique client IPs per day, well within budget. If
 * this ever becomes a problem, swap the map for a Caffeine cache
 * with a size limit + TTL eviction.
 */
@Service
public class LoginRateLimitService {

    /** Each IP gets a 5-token bucket. The bucket refills 5 tokens every 15 minutes. */
    private static final long CAPACITY = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    /**
     * IP -> bucket. {@code ConcurrentHashMap} gives us:
     *   - thread-safe putIfAbsent for lazy bucket creation
     *   - lock-free reads on the hot path
     * Buckets are independent of each other; one IP being throttled
     * doesn't affect any other IP.
     */
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Try to consume one token from the given IP's bucket. The
     * returned {@link ConsumptionProbe} tells the caller:
     *   - {@code isConsumed() == true}:  the request is allowed,
     *     the bucket has been decremented by one.
     *   - {@code isConsumed() == false}: the bucket is empty. Use
     *     {@code getNanosToWaitForRefill()} to populate the
     *     {@code Retry-After} header.
     */
    public ConsumptionProbe tryConsume(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Convenience: how many seconds the caller should wait before
     * retrying. Returns 0 if the bucket is currently full.
     */
    public long retryAfterSeconds(String ip) {
        Bucket bucket = buckets.get(ip);
        if (bucket == null) return 0L;
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        // tryConsume above consumed a token we didn't actually want to
        // consume. Refund it by adding it back to the bucket.
        bucket.addTokens(1);
        return probe.isConsumed() ? 0L
                                   : (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
    }

    /**
     * Build a fresh bucket: 5-token capacity, refilled INTERVALLY
     * (5 tokens added all at once) every 15 minutes. We use the
     * intervally refill (not the greedy refill) because a "block
     * burst attempts for 15 min, then allow 5 more" model is
     * exactly what the spec asked for.
     */
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(CAPACITY, WINDOW)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
