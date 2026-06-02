package nl.bldn.housedata.emergencyprep

import mu.KLogging
import nl.bldn.housedata.EmailMessage
import nl.bldn.housedata.NotificationSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Component
class EmergencyProductExpiryChecker(
    val repository: EmergencyProductRepository,
    val notificationSender: NotificationSender,
    val clock: Clock,
) {
    @Scheduled(initialDelay = 1, timeUnit = TimeUnit.HOURS, fixedRate = 48)
    fun checkExpiry() {
        val oneWeekFromNow = LocalDate.now(clock).plusWeeks(1)
        logger.debug { "Checking expiry - products with expiry before $oneWeekFromNow are reported" }

        repository.findAll()
            .filter { it.bestBeforeDate <= oneWeekFromNow }
            .map {
                EmailMessage(
                    subject = "Houdbaarheidsdatum van ${it.name} in noodpakket verloopt",
                    contentText = "Op ${it.bestBeforeDate} verloopt de houdbaarheid van ${it.name} (aantal ${it.amount}, inhoud ${it.containerQuantity}). Vervang deze op tijd.",
                )
            }
            .forEach { notificationSender.sendNotification(it) }
    }

    companion object : KLogging()
}