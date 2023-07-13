package nl.bldn.housedata

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mu.KLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface PowerMeasureDataStore {
    fun store(measurement: PowerMeasureData)

    fun retrieveStandardTimeFrame(source: String = WASHING_MACHINE): Map<LocalDateTime, PowerMeasureData>
}

@Repository
class LocalStoragePowerMeasureDataStore(
    private val clock: Clock,
    private val objectMapper: ObjectMapper,
): PowerMeasureDataStore {
    private val storage = mapOf(
        WASHING_MACHINE to ConcurrentHashMap<LocalDateTime, PowerMeasureData>(),
        DRYER to ConcurrentHashMap<LocalDateTime, PowerMeasureData>(),
    )

    override fun store(measurement: PowerMeasureData) {
        storage.getValue(measurement.source)[measurement.timeStamp] = measurement
    }

    override fun retrieveStandardTimeFrame(source: String): Map<LocalDateTime, PowerMeasureData> {
        return storage.getValue(source)
    }

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
    fun evictDataSurplus() {
        logger.debug { "Evicting data older than 2 days..." }
        val twoDaysAgo = now(clock).withSecond(0).withNano(0).minusHours(48)
        storage.values.forEach { measurements ->
            measurements.keys
                .filter { ts -> ts.isBefore(twoDaysAgo) }
                .forEach { measurements.remove(it) }
        }
        logger.debug { "Data evicted." }
    }

    @PreDestroy
    fun storeLocally() {
        logger.debug { "Storing data locally in $USAGE_FILE" }
        val userHome = System.getProperty("user.home")
        val rootDir = Paths.get(userHome, USAGE_FOLDER).toFile()
        if (!rootDir.exists()) {
            Files.createDirectories(rootDir.toPath())
        }

        val file = File(rootDir, USAGE_FILE)
        if (file.createNewFile()) {
            logger.debug { "Created $USAGE_FILE" }
        }

        val content = objectMapper.writeValueAsString(storage)
        logger.debug { "Storing $content" }
        file.writeText(content)
    }

    @PostConstruct
    fun retrieveLocally() {
        logger.debug { "Reloading from $USAGE_FILE" }
        val userHome = System.getProperty("user.home")
        val file = Paths.get(userHome, USAGE_FOLDER, USAGE_FILE).toFile()
        if (!file.exists()) {
            return
        }

        val typeReference = object: TypeReference<Map<String, Map<LocalDateTime, PowerMeasureData>>>() {}

        val map = objectMapper.readValue(file.readText(), typeReference)
        storage.getValue(WASHING_MACHINE).putAll(map[WASHING_MACHINE] as Map<LocalDateTime, PowerMeasureData>)

        val dryerMap = map[DRYER] ?: mutableMapOf()
        storage.getValue(DRYER).putAll(dryerMap)
    }

    companion object : KLogging() {
        private const val USAGE_FOLDER = ".verbruiksapp"
        private const val USAGE_FILE = "store.json"
    }
}

data class PowerMeasureData(
    val timeStamp: LocalDateTime,
    val measuredOutputInMilliWattHourMinute: BigDecimal,
    val source: String = WASHING_MACHINE,
    val categorization: String = OFF,
)

const val WASHING_MACHINE: String = "WASHING_MACHINE"
const val DRYER: String = "DRYER"
const val OFF = "OFF"
const val IDLE = "IDLE"
const val ACTIVE = "ACTIVE"
const val RUNNING_CYCLE = "RUNNING CYCLE"