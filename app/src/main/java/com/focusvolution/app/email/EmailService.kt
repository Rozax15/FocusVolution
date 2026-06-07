package com.focusvolution.app.email

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmailConfig {
    var smtpHost: String = "smtp.gmail.com"
    var smtpPort: Int = 587
    var smtpUsername: String = ""
    var smtpPassword: String = ""
    var fromEmail: String = ""
    var fromName: String = "FOCUSVOLUTION"
}

object EmailService {

    suspend fun sendVerificationEmail(
        toEmail: String,
        username: String,
        verificationCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (EmailConfig.smtpUsername.isBlank()) {
                return@withContext Result.failure(IllegalStateException("SMTP não configurado. Define EmailConfig.smtpUsername e EmailConfig.smtpPassword."))
            }

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", EmailConfig.smtpHost)
                put("mail.smtp.port", EmailConfig.smtpPort.toString())
                put("mail.smtp.ssl.trust", EmailConfig.smtpHost)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EmailConfig.smtpUsername, EmailConfig.smtpPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EmailConfig.fromEmail.ifBlank { EmailConfig.smtpUsername }, EmailConfig.fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Verifica o teu email — FOCUSVOLUTION"
                setContent(
                    buildEmailHtml(username, verificationCode),
                    "text/html; charset=utf-8"
                )
            }

            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildEmailHtml(username: String, code: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"></head>
        <body style="font-family: Arial, sans-serif; background: #080E1E; margin: 0; padding: 0;">
            <table width="100%" cellpadding="0" cellspacing="0" style="background: #080E1E; padding: 40px 0;">
                <tr>
                    <td align="center">
                        <table width="400" cellpadding="0" cellspacing="0" style="background: #111B2E; border-radius: 12px; padding: 32px;">
                            <tr>
                                <td style="color: #E2E8F0; font-size: 16px; line-height: 1.5; text-align: center;">
                                    <p>Olá, <strong>$username</strong>!</p>
                                    <p>Obrigado por criares a tua conta. Para ativar a conta, insere o código abaixo:</p>
                                </td>
                            </tr>
                            <tr>
                                <td align="center" style="padding: 20px 0;">
                                    <span style="background: #1E293B; color: #3B82F6; font-size: 28px; font-weight: bold; letter-spacing: 8px; padding: 10px 20px; border-radius: 8px; display: inline-block; font-family: 'Courier New', monospace;">
                                        $code
                                    </span>
                                </td>
                            </tr>
                            <tr>
                                <td style="color: #94A3B8; font-size: 13px; line-height: 1.4; text-align: center;">
                                    <p>Se não criaste esta conta, ignora este email.</p>
                                    <p>O código expira em 24 horas.</p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.trimIndent()
    }
}
