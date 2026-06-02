package nl.bldn.housedata

import org.springframework.stereotype.Component
import java.util.*

@Component
class CreateDeviceMessage {
    operator fun invoke(type: String, status: String): EmailMessage {
        val displayType = toDisplayType(type)
        return EmailMessage(
            subject = "${displayType.capitalizeNew()} notificatie: programma is klaar",
            contentText = "Apparaat $displayType is van status veranderd. Nieuwe status is $status en dus is het programma klaar."
        )
    }

    private fun toDisplayType(type: String): String =
        when (type) {
            WASHING_MACHINE -> "wasmachine"
            DRYER -> "droger"
            else -> throw IllegalArgumentException("Unknown device type: $type")
        }

private fun String.capitalizeNew() =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("NL")) else it.toString() }
}
