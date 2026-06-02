package nl.bldn.housedata.emergencyprep

import java.time.LocalDate
import java.util.*

data class EmergencyProduct(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val amount: Int,
    val containerQuantity: String,
    val bestBeforeDate: LocalDate,
)
