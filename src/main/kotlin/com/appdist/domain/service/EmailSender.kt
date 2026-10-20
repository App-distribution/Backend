package com.appdist.domain.service

data class EmailMessage(
    val to: String,
    val subject: String,
    val text: String,
    val html: String,
)

interface EmailSender {
    suspend fun send(message: EmailMessage)
}

/** Письмо не ушло: клиенту честнее сказать об этом, чем ждать кода, который не придёт. */
class EmailDeliveryException(cause: Throwable) : RuntimeException("Email delivery failed", cause)
