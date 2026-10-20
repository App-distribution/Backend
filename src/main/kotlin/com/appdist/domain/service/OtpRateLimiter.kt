package com.appdist.domain.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.ArrayDeque
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

sealed interface RateLimitDecision {
    data object Allowed : RateLimitDecision
    data class Limited(val retryAfterSeconds: Long) : RateLimitDecision
}

/**
 * Ограничивает запросы кода входа: каждый запрос — письмо с нашего ящика,
 * и без лимита кто угодно может засыпать чужую почту, а почтовик —
 * заблокировать ящик как спамерский.
 *
 * Счётчики в памяти: бэкенд работает в одном экземпляре, а сброс лимитов
 * при перезапуске безвреден.
 */
class OtpRateLimiter(
    private val clock: Clock = Clock.System,
    private val rules: List<Rule> = DEFAULT_RULES,
) {
    enum class Key { EMAIL, IP }
    data class Rule(val key: Key, val limit: Int, val window: Duration)

    private val hits = HashMap<String, ArrayDeque<Instant>>()

    @Synchronized
    fun tryAcquire(email: String, ip: String): RateLimitDecision {
        val now = clock.now()
        val keys = mapOf(Key.EMAIL to "email:${email.trim().lowercase()}", Key.IP to "ip:$ip")

        val waits = rules.mapNotNull { rule ->
            val window = hitsFor(keys.getValue(rule.key), now)
            val inWindow = window.count { it > now - rule.window }
            if (inWindow < rule.limit) return@mapNotNull null
            // Освободится место, когда из окна выйдет самый старый из последних `limit` запросов.
            val oldestCounted = window.toList().takeLast(rule.limit).first()
            oldestCounted + rule.window - now
        }
        if (waits.isNotEmpty()) {
            return RateLimitDecision.Limited(ceil(waits.max().inWholeMilliseconds / 1000.0).toLong().coerceAtLeast(1))
        }

        keys.values.forEach { hitsFor(it, now).addLast(now) }
        return RateLimitDecision.Allowed
    }

    /** Отдаёт историю ключа, выбросив всё старше самого длинного окна. */
    private fun hitsFor(key: String, now: Instant): ArrayDeque<Instant> {
        val longest = rules.maxOf { it.window }
        val deque = hits.getOrPut(key) { ArrayDeque() }
        while (deque.isNotEmpty() && deque.first() <= now - longest) deque.removeFirst()
        return deque
    }

    companion object {
        val DEFAULT_RULES = listOf(
            Rule(Key.EMAIL, limit = 1, window = 1.minutes),
            Rule(Key.EMAIL, limit = 5, window = 1.hours),
            Rule(Key.IP, limit = 20, window = 1.hours),
        )
    }
}
