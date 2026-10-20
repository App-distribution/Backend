package com.appdist.infrastructure.email

import com.appdist.domain.service.EmailMessage
import com.appdist.domain.service.EmailSender
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Для локальной разработки и тестов, когда SMTP не настроен: письмо целиком
 * уходит в лог. На проде так нельзя — по логам можно войти под чужой почтой.
 */
class LogEmailSender : EmailSender {
    override suspend fun send(message: EmailMessage) {
        log.warn { "SMTP not configured, email to ${message.to} logged instead:\n${message.text}" }
    }
}
