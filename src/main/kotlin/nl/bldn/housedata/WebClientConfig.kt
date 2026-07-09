package nl.bldn.housedata

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class WebClientConfig {

    @Bean
    fun plugRestTemplate(): RestTemplate = RestTemplate()

}