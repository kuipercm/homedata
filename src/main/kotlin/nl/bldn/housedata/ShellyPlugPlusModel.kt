package nl.bldn.housedata

import java.math.BigDecimal

data class SwitchStatus (
    val id: Long,
    val source: String,
    val output: Boolean,
    val apower: BigDecimal,
    val voltage: BigDecimal,
    val current: BigDecimal,
    val aenergy: SwitchEnergy,
    val temperature: SwitchTemperature,
)

data class SwitchEnergy(
    val total: BigDecimal,
    val by_minute: List<BigDecimal>,
    val minute_ts: Long,
)

data class SwitchTemperature(
    val tC: BigDecimal,
    val tF: BigDecimal,
)