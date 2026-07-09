package nl.bldn.housedata

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(UiController::class)
class UiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET root returns 200 with show-graph view`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("show-graph"))
    }

    @Test
    fun `GET root contains chart canvas elements`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("washingMachineChart")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("dryerChart")))
    }
}
