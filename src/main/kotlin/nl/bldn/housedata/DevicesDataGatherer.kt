package nl.bldn.housedata

import mu.KLogging
import nl.bldn.housedata.EventObserver.Companion.POWER_MEASUREMENTS_TOPIC
import org.springframework.jms.core.JmsTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.concurrent.TimeUnit.SECONDS

@Service
class DevicesDataGatherer(
    private val dataStore: PowerMeasureDataStore,
    private val plugRestTemplate: RestTemplate,
    private val jmsTemplate: JmsTemplate,
    private val housedataProperties: HousedataProperties,
) {

    @Scheduled(initialDelay = 30, timeUnit = SECONDS, fixedRate = 60)
    fun gatherPowerMeasuresData() {
        logger.info { "Gathering power measurements" }
        housedataProperties.urls.map {
            it.key to plugRestTemplate.getForEntity(
                "${it.value}$SWITCH_STATUS_URL_SUFFIX",
                SwitchStatus::class.java,
                mutableMapOf<String, Any>()
            ).body!!
        }.forEach { (deviceName, switchStatus) ->
            val measuredValue = switchStatus.aenergy.by_minute[1]
            val measurement = PowerMeasureData(
                timeStamp = switchStatus.aenergy.minute_ts.truncateToMinutes(),
                measuredOutputInMilliWattHourMinute = measuredValue,
                source = deviceName,
                categorization = findCategory(deviceName, measuredValue)
            )
            dataStore.store(measurement)
            jmsTemplate.convertAndSend(POWER_MEASUREMENTS_TOPIC, measurement)
        }
        logger.info { "Power measurements gathered and stored" }
    }

    private fun Long.truncateToMinutes(): LocalDateTime =
        LocalDateTime
            .ofEpochSecond(this, 0, UTC)
            .withSecond(0)

    private fun findCategory(deviceName: String, measuredValue: BigDecimal): String {
        return when (deviceName) {
            WASHING_MACHINE -> when {
                measuredValue < 3.toBigDecimal() -> OFF
                measuredValue < 40.toBigDecimal() -> IDLE
                measuredValue < 100.toBigDecimal() -> ACTIVE
                else -> RUNNING_CYCLE
            }
            DRYER -> when {
                measuredValue < 3.toBigDecimal() -> OFF
                measuredValue < 10.toBigDecimal() -> IDLE
                measuredValue < 35.toBigDecimal() -> ACTIVE
                else -> RUNNING_CYCLE
            }
            else -> throw IllegalArgumentException("Unknown device $deviceName")
        }
    }

    companion object : KLogging() {
        private const val SWITCH_STATUS_URL_SUFFIX = "/rpc/Switch.GetStatus?id=0"
    }
}