package nl.bldn.housedata

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class HousedataApplication

fun main(args: Array<String>) {
    runApplication<HousedataApplication>(*args)
}
