package com.appdist.infrastructure.email

import com.appdist.config.AppConfig
import com.appdist.domain.service.EmailMessage
import com.appdist.domain.service.EmailSender
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * SMTP с SSL с первого байта (порт 465): так работают и Mail.ru, и Яндекс.
 * Таймауты обязательны — без них зависший SMTP держит запрос на вход вечно.
 */
class SmtpEmailSender(private val config: AppConfig.SmtpConfig) : EmailSender {
    private val session: Session = Session.getInstance(Properties().apply {
        setProperty("mail.smtp.host", config.host)
        setProperty("mail.smtp.port", config.port.toString())
        setProperty("mail.smtp.auth", "true")
        setProperty("mail.smtp.ssl.enable", "true")
        setProperty("mail.smtp.ssl.checkserveridentity", "true")
        setProperty("mail.smtp.connectiontimeout", TIMEOUT_MS)
        setProperty("mail.smtp.timeout", TIMEOUT_MS)
        setProperty("mail.smtp.writetimeout", TIMEOUT_MS)
    })

    override suspend fun send(message: EmailMessage) = withContext(Dispatchers.IO) {
        val mime = MimeMessage(session).apply {
            setFrom(InternetAddress(config.from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.to))
            setSubject(message.subject, Charsets.UTF_8.name())
            setContent(MimeMultipart("alternative").apply {
                addBodyPart(MimeBodyPart().apply { setText(message.text, Charsets.UTF_8.name()) })
                addBodyPart(MimeBodyPart().apply { setContent(message.html, "text/html; charset=UTF-8") })
            })
        }
        Transport.send(mime, config.username, config.password)
    }

    private companion object {
        const val TIMEOUT_MS = "10000"
    }
}
