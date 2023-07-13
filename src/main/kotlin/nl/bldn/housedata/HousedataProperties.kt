package nl.bldn.housedata

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "housedata")
data class HousedataProperties(
    val urls: Map<String, String>,
    val mailTo: String,
    val mailFrom: String,
)