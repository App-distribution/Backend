package com.appdist.fixtures

import com.appdist.domain.service.EmailMessage
import com.appdist.domain.service.EmailSender

/** Запоминает письма вместо отправки; с [failWith] имитирует недоступный SMTP. */
class RecordingEmailSender(private val failWith: Exception? = null) : EmailSender {
    val sent = mutableListOf<EmailMessage>()

    override suspend fun send(message: EmailMessage) {
        failWith?.let { throw it }
        sent += message
    }
}
