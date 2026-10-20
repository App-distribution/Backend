package com.appdist.service

import com.appdist.domain.service.OtpRateLimiter
import com.appdist.domain.service.RateLimitDecision
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OtpRateLimiterTest {
    private class TestClock : Clock {
        var now: Instant = Instant.parse("2026-10-20T12:00:00Z")
        override fun now() = now
        fun advance(by: Duration) { now += by }
    }

    private val clock = TestClock()
    private val limiter = OtpRateLimiter(clock)

    @Test
    fun `second code for the same email within a minute is refused with time left`() {
        assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("a@x.com", "1.1.1.1"))
        clock.advance(20.seconds)

        val decision = assertIs<RateLimitDecision.Limited>(limiter.tryAcquire("a@x.com", "1.1.1.1"))
        assertEquals(40, decision.retryAfterSeconds)
    }

    @Test
    fun `same email is allowed again after the minute passes`() {
        limiter.tryAcquire("a@x.com", "1.1.1.1")
        clock.advance(61.seconds)

        assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("a@x.com", "1.1.1.1"))
    }

    @Test
    fun `sixth code for the same email within an hour is refused until the oldest expires`() {
        repeat(5) {
            assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("a@x.com", "1.1.1.1"))
            clock.advance(2.minutes)
        }
        // Прошло 10 минут с первого запроса: до его выхода из окна — 50 минут.
        val decision = assertIs<RateLimitDecision.Limited>(limiter.tryAcquire("a@x.com", "1.1.1.1"))
        assertEquals(50 * 60, decision.retryAfterSeconds)
    }

    @Test
    fun `one IP cannot request codes for many different emails`() {
        repeat(20) { i ->
            assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("user$i@x.com", "6.6.6.6"))
        }
        assertIs<RateLimitDecision.Limited>(limiter.tryAcquire("user20@x.com", "6.6.6.6"))
        // Другой IP этим не задет.
        assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("user20@x.com", "7.7.7.7"))
    }

    // Отказ не должен сам продлевать блокировку — иначе нетерпеливое нажатие
    // кнопки отодвигало бы момент, когда можно запросить код снова.
    @Test
    fun `refused requests are not counted`() {
        limiter.tryAcquire("a@x.com", "1.1.1.1")
        clock.advance(30.seconds)
        limiter.tryAcquire("a@x.com", "1.1.1.1")
        clock.advance(31.seconds)

        assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("a@x.com", "1.1.1.1"))
    }

    @Test
    fun `email is matched case-insensitively`() {
        limiter.tryAcquire("Alice@X.com", "1.1.1.1")
        assertIs<RateLimitDecision.Limited>(limiter.tryAcquire("alice@x.com", "1.1.1.1"))
    }
}
