package com.appdist.domain.service

import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.*
import com.appdist.infrastructure.email.LogEmailSender
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

// Шестизначный код за время жизни иначе можно перебрать запросами.
private const val MAX_FAILED_OTP_ATTEMPTS = 5

val FREE_EMAIL_DOMAINS = setOf(
    "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.uk", "yahoo.co.in",
    "hotmail.com", "hotmail.co.uk", "outlook.com", "live.com", "msn.com",
    "icloud.com", "me.com", "mac.com", "protonmail.com", "proton.me",
    "aol.com", "yandex.com", "yandex.ru", "mail.ru", "inbox.com",
)

data class AuthTokens(val accessToken: String, val refreshToken: String)

open class AuthService(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val otpRepository: OtpRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtConfig: AppConfig.JwtConfig,
    private val otpConfig: AppConfig.OtpConfig,
    private val auditRepository: AuditRepository? = null,
    private val emailSender: EmailSender = LogEmailSender(),
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    open suspend fun requestOtp(email: String): String {
        val code = generateOtp(otpConfig.length)
        otpRepository.create(email, code, otpConfig.ttlMinutes)
        try {
            emailSender.send(otpEmail(email, code))
        } catch (e: Exception) {
            log.error(e) { "Failed to send OTP email to $email" }
            throw EmailDeliveryException(e)
        }
        return code
    }

    private fun otpEmail(email: String, code: String): EmailMessage {
        val ttl = "${otpConfig.ttlMinutes} минут"
        return EmailMessage(
            to = email,
            subject = "Код входа в AppDist: $code",
            text = "Ваш код входа в AppDist: $code\n\nКод действует $ttl. " +
                "Если вы не запрашивали вход, просто проигнорируйте это письмо.",
            html = """
                <p>Ваш код входа в AppDist:</p>
                <p style="font-size:28px;font-weight:600;letter-spacing:4px;font-family:monospace">$code</p>
                <p>Код действует $ttl. Если вы не запрашивали вход, просто проигнорируйте это письмо.</p>
            """.trimIndent(),
        )
    }

    suspend fun verifyOtp(email: String, code: String): AuthTokens {
        // 1. Verify OTP
        if (otpRepository.findValid(email, code) == null) {
            otpRepository.registerFailedAttempt(email, MAX_FAILED_OTP_ATTEMPTS)
            error("Неверный или просроченный код")
        }
        otpRepository.markUsed(email, code)

        // 2. Determine workspace slug from email domain
        val domain = email.substringAfter("@").lowercase()
        val slug = if (domain in FREE_EMAIL_DOMAINS) {
            email.replace("@", "-at-").lowercase()
        } else {
            domain
        }

        // 3. Find or create workspace
        // Race condition: concurrent first-logins from same domain → UNIQUE constraint on slug
        val existingWorkspace = workspaceRepository.findBySlug(slug)
        val workspaceJustCreated = existingWorkspace == null
        val workspace = existingWorkspace ?: try {
            workspaceRepository.create(name = slug, slug = slug)
        } catch (_: Exception) {
            workspaceRepository.findBySlug(slug)
                ?: error("Failed to find or create workspace for slug=$slug")
        }

        // 4. Find or create user
        val existingUser = userRepository.findByEmail(email)
        val user = if (existingUser == null) {
            val role = if (workspaceJustCreated) UserRole.ADMIN else UserRole.TESTER
            val displayName = email.substringBefore("@")
            val newUser = userRepository.create(workspace.id, email, displayName, role)
            if (workspaceJustCreated) {
                workspaceRepository.updateOwnerId(workspace.id, newUser.id)
            }
            newUser
        } else {
            existingUser
        }

        // 5. Issue tokens
        val tokens = issueTokens(user)

        // 6. Audit — fire-and-forget
        launch {
            runCatching {
                auditRepository?.log(user.id, "user.login", "user", user.id)
            }.onFailure { log.warn(it) { "Audit log failed for user.login" } }
        }

        return tokens
    }

    suspend fun refreshToken(token: String): AuthTokens {
        val refreshToken = refreshTokenRepository.findValid(token)
            ?: error("Invalid or expired refresh token")
        refreshTokenRepository.revoke(token)
        val user = userRepository.findById(refreshToken.userId)
            ?: error("User not found")
        return issueTokens(user)
    }

    private suspend fun issueTokens(user: com.appdist.domain.model.User): AuthTokens {
        requireNotNull(user.workspaceId) { "Cannot issue token for user ${user.id} with no workspace" }
        val algorithm = Algorithm.HMAC256(jwtConfig.secret)
        val accessToken = JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withSubject(user.id.toString())
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withClaim("workspace_id", user.workspaceId.toString())
            .withExpiresAt(Date.from(
                (Clock.System.now() + jwtConfig.accessTokenTtlMinutes.minutes).toJavaInstant()
            ))
            .sign(algorithm)
        val rawRefreshToken = UUID.randomUUID().toString()
        refreshTokenRepository.create(user.id, rawRefreshToken, jwtConfig.refreshTokenTtlDays)
        return AuthTokens(accessToken, rawRefreshToken)
    }

    private val secureRandom = java.security.SecureRandom()

    private fun generateOtp(length: Int) = (0 until length)
        .map { secureRandom.nextInt(10) }
        .joinToString("")
}
