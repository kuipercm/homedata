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
        if (lastMeasurement.categorization != measurement.categorization) {
            logger.debug { "Received ${measurement.source} measurement with changed category: ${measurement.measuredOutputInMilliWattHourMinute} --> Labeled as ${measurement.categorization}" }
            notificationSender.sendNotification(measurement.source, measurement.categorization)
        }

        last[measurement.source] = measurement
    }

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
