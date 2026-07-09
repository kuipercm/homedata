package nl.bldn.housedata.emergencyprep

import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(EmergencyResource::class)
class EmergencyResourceTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var repository: EmergencyProductRepository

    private val productId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val product = EmergencyProduct(
        id = productId,
        name = "Bottled Water",
        amount = 10,
        containerQuantity = "500ml",
        bestBeforeDate = LocalDate.of(2027, 1, 1)
    )

    @Test
    fun `GET all products returns empty list when no products exist`() {
        given(repository.findAll()).willReturn(emptyList())

        mockMvc.perform(get("/api/emergency-products"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[]"))
    }

    @Test
    fun `GET all products returns list when products exist`() {
        given(repository.findAll()).willReturn(listOf(product))

        mockMvc.perform(get("/api/emergency-products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Bottled Water"))
            .andExpect(jsonPath("$[0].amount").value(10))
            .andExpect(jsonPath("$[0].containerQuantity").value("500ml"))
            .andExpect(jsonPath("$[0].bestBeforeDate").value("2027-01-01"))
    }

    @Test
    fun `GET product by id returns product when it exists`() {
        given(repository.findById(productId)).willReturn(product)

        mockMvc.perform(get("/api/emergency-products/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(productId.toString()))
            .andExpect(jsonPath("$.name").value("Bottled Water"))
    }

    @Test
    fun `GET product by id returns 404 when product does not exist`() {
        given(repository.findById(productId)).willReturn(null)

        mockMvc.perform(get("/api/emergency-products/$productId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST creates product and returns 201 with saved product`() {
        given(repository.save(product)).willReturn(product)

        mockMvc.perform(
            post("/api/emergency-products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(product))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Bottled Water"))
            .andExpect(jsonPath("$.amount").value(10))
    }

    @Test
    fun `PUT updates existing product and returns 200`() {
        val updatedProduct = product.copy(amount = 20)
        given(repository.findById(productId)).willReturn(product)
        given(repository.save(updatedProduct)).willReturn(updatedProduct)

        mockMvc.perform(
            put("/api/emergency-products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updatedProduct))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(20))
    }

    @Test
    fun `PUT ensures path id is used even when body has different id`() {
        val bodyWithDifferentId = product.copy(id = UUID.randomUUID(), amount = 20)
        val savedWithPathId = bodyWithDifferentId.copy(id = productId)
        given(repository.findById(productId)).willReturn(product)
        given(repository.save(savedWithPathId)).willReturn(savedWithPathId)

        mockMvc.perform(
            put("/api/emergency-products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(bodyWithDifferentId))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(productId.toString()))
    }

    @Test
    fun `PUT returns 404 when product does not exist`() {
        given(repository.findById(productId)).willReturn(null)

        mockMvc.perform(
            put("/api/emergency-products/$productId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(product))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE removes product and returns 204`() {
        given(repository.deleteById(productId)).willReturn(true)

        mockMvc.perform(delete("/api/emergency-products/$productId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE returns 404 when product does not exist`() {
        given(repository.deleteById(productId)).willReturn(false)

        mockMvc.perform(delete("/api/emergency-products/$productId"))
            .andExpect(status().isNotFound)
    }
}
