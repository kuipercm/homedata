package nl.bldn.housedata

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service


@Service
class NotificationSender(
    private val javaMailSender: JavaMailSender,
    private val housedataProperties: HousedataProperties,
) {
    fun sendNotification(type: String, status: String) {
        val message = SimpleMailMessage()
        message.from = housedataProperties.mailFrom
        message.setTo(housedataProperties.mailTo)
        message.subject = "$type notificatie: $status"
        message.text = "Apparaat $type is van status veranderd. Nieuwe status is $status"
        javaMailSender.send(message)
    }
}