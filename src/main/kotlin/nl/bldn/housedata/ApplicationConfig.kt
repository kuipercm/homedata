package nl.bldn.housedata

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.support.converter.JacksonJsonMessageConverter
import org.springframework.jms.support.converter.MessageConverter
import org.springframework.jms.support.converter.MessageType
import tools.jackson.databind.json.JsonMapper
import java.time.Clock


@Configuration
@EnableJms
@EnableConfigurationProperties(HousedataProperties::class)
class ApplicationConfig {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean // Serialize message content to json using TextMessage
    fun jacksonJmsMessageConverter(objectMapper: JsonMapper): MessageConverter? {
        val converter = JacksonJsonMessageConverter(objectMapper)
        converter.setTargetType(MessageType.TEXT)
        converter.setTypeIdPropertyName("_type")
        return converter
    }
}