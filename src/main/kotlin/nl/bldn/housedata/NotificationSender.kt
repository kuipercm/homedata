package nl.bldn.housedata

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service


@Service
class NotificationSender(
    private val javaMailSender: JavaMailSender,
    private val housedataProperties: HousedataProperties,
) {
    fun sendNotification(email: EmailMessage) {
        val mailMessage = SimpleMailMessage()
        mailMessage.from = housedataProperties.mailFrom
        mailMessage.setTo(*housedataProperties.mailTo.toTypedArray())

        mailMessage.subject = email.subject
        mailMessage.text = email.contentText
        javaMailSender.send(mailMessage)
    }
}

data class EmailMessage(
    val subject: String,
    val contentText: String,
)