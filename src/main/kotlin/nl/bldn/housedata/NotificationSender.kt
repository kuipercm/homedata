package nl.bldn.housedata

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.*


@Service
class NotificationSender(
    private val javaMailSender: JavaMailSender,
    private val housedataProperties: HousedataProperties,
) {
    fun sendNotification(type: String, status: String) {
        val message = SimpleMailMessage()
        message.from = housedataProperties.mailFrom
        message.setTo(*housedataProperties.mailTo.toTypedArray())

        val displayType = toDisplayType(type)
        message.subject = "${displayType.capitalizeNew()} notificatie: programma is klaar"
        message.text = "Apparaat $displayType is van status veranderd. Nieuwe status is $status en dus is het programma klaar."
        javaMailSender.send(message)
    }

    private fun toDisplayType(type: String): String =
        when(type) {
            WASHING_MACHINE -> "wasmachine"
            DRYER -> "droger"
            else -> throw IllegalArgumentException("Unknown device type: $type")
        }

    private fun String.capitalizeNew() =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("NL")) else it.toString() }
}