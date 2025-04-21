package org.example.services

import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Servicio para enviar correos electrónicos personalizados
 */
object EmailService {

    private const val USERNAME = "hospola2@il.com"  // Cambiar por tu correo
    private const val PASSWORD = "krff xtzd huvd feqi"  // Contraseña de aplicación
    
    // Inicializar las propiedades para la sesión de correo
    private val properties = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", "smtp.gmail.com")
        put("mail.smtp.port", "587")
    }
    
    /**
     * Envía un correo electrónico personalizado para actualizar el correo
     * 
     * @param to Dirección de correo electrónico del destinatario
     * @param verificationLink Enlace de verificación
     * @return true si el correo se envió correctamente, false en caso contrario
     */
    fun sendEmailUpdateVerification(to: String, verificationLink: String): Boolean {
        val subject = "Verifica tu nueva dirección de correo electrónico - AirPlan"
        
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <img src="https://airplan-f08be.web.app/assets/logo.png" alt="AirPlan Logo" style="max-width: 150px;">
                </div>
                
                <h2 style="color: #4285F4; text-align: center;">Verifica tu nueva dirección de correo electrónico</h2>
                
                <p>Hola,</p>
                
                <p>Has solicitado cambiar tu dirección de correo electrónico en AirPlan. Para completar este proceso, necesitamos verificar que tienes acceso a esta nueva dirección de correo.</p>
                
                <div style="text-align: center; margin: 30px 0;">
                    <a href="$verificationLink" style="background-color: #4285F4; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold;">Verificar mi correo electrónico</a>
                </div>
                
                <p>O puedes copiar y pegar el siguiente enlace en tu navegador:</p>
                <p style="background-color: #f5f5f5; padding: 10px; word-break: break-all;">$verificationLink</p>
                
                <p>Si no has solicitado este cambio, puedes ignorar este correo.</p>
                
                <p>Gracias,<br>El equipo de AirPlan</p>
                
                <div style="margin-top: 20px; padding-top: 20px; border-top: 1px solid #e0e0e0; font-size: 12px; color: #757575; text-align: center;">
                    <p>Este es un correo electrónico automático, por favor no respondas a este mensaje.</p>
                </div>
            </div>
        """.trimIndent()
        
        return sendEmail(to, subject, htmlContent)
    }
    
    /**
     * Método general para enviar correos electrónicos
     * 
     * @param to Dirección de correo electrónico del destinatario
     * @param subject Asunto del correo
     * @param htmlContent Contenido HTML del correo
     * @return true si el correo se envió correctamente, false en caso contrario
     */
    private fun sendEmail(to: String, subject: String, htmlContent: String): Boolean {
        return try {
            val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(USERNAME, PASSWORD)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(USERNAME))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            message.subject = subject
            message.setContent(htmlContent, "text/html; charset=utf-8")

            Transport.send(message)
            println("✅ Correo enviado correctamente a: $to")
            true
        } catch (e: MessagingException) {
            println("❌ Error al enviar correo: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}