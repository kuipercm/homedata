package nl.bldn.housedata

import mu.KLogging
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime.MIN

@Service
class EventObserver(
    private val notificationSender: NotificationSender,
) {
    private val last: MutableMap<String, PowerMeasureData> =
        mutableMapOf(
            WASHING_MACHINE to EMPTY_MEASUREMENT_WASHER,
            DRYER to EMPTY_MEASUREMENT_DRYER
        )

    @JmsListener(destination = POWER_MEASUREMENTS_TOPIC)
    fun observe(measurement: PowerMeasureData) {
        val lastMeasurement = last.getValue(measurement.source)
        if (hasStoppedCycleSinceLastMeasurement(lastMeasurement, measurement)) {
            logger.debug { "Received ${measurement.source} measurement with changed category: ${measurement.measuredOutputInMilliWattHourMinute} --> Labeled as ${measurement.categorization}" }
            notificationSender.sendNotification(measurement.source, measurement.categorization)
        }

        last[measurement.source] = measurement
    }

    private fun hasStoppedCycleSinceLastMeasurement(lastMeasurement: PowerMeasureData, measurement: PowerMeasureData) =
        lastMeasurement.categorization == RUNNING_CYCLE &&
                (measurement.categorization == IDLE || measurement.categorization == ACTIVE)

    companion object: KLogging() {
        const val POWER_MEASUREMENTS_TOPIC = "powermeasurements"

        private val EMPTY_MEASUREMENT_WASHER = PowerMeasureData(
            timeStamp = MIN,
            measuredOutputInMilliWattHourMinute = BigDecimal("-1.0"),
            source = WASHING_MACHINE,
            categorization = "INVALID"
        )
        private val EMPTY_MEASUREMENT_DRYER = PowerMeasureData(
            timeStamp = MIN,
            measuredOutputInMilliWattHourMinute = BigDecimal("-1.0"),
            source = DRYER,
            categorization = "INVALID"
        )
    }

}
