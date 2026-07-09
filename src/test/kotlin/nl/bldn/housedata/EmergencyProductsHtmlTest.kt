package nl.bldn.housedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmergencyProductsHtmlTest {

    private val html: String by lazy {
        javaClass.classLoader.getResourceAsStream("static/emergency-products.html")
            ?.bufferedReader()?.readText()
            ?: error("emergency-products.html not found on classpath")
    }

    @Test
    fun `babel standalone is pinned to major version 7`() {
        assertThat(html).contains("@babel/standalone@7")
    }

    @Test
    fun `babel script tag explicitly declares react preset`() {
        assertThat(html).contains("""data-presets="react"""")
    }

    @Test
    fun `react 18 is used`() {
        assertThat(html).contains("react@18")
        assertThat(html).contains("react-dom@18")
    }

    @Test
    fun `root div is present for React to mount into`() {
        assertThat(html).contains("""id="root"""")
    }
}
