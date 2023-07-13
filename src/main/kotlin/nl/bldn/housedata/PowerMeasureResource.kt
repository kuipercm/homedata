package nl.bldn.housedata

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/power")
class PowerMeasureResource(
    private val powerMeasureDataStore: PowerMeasureDataStore,
) {
    @GetMapping("/washingmachine")
    fun getAllWashingMachineData(): List<PowerMeasureData> {
        return powerMeasureDataStore.retrieveStandardTimeFrame(WASHING_MACHINE).values.sortedBy { it.timeStamp }
    }

    @GetMapping("/dryer")
    fun getAllDryerData(): List<PowerMeasureData> {
        return powerMeasureDataStore.retrieveStandardTimeFrame(DRYER).values.sortedBy { it.timeStamp }
    }
}