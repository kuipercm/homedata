package nl.bldn.housedata.emergencyprep

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/emergency-products")
class EmergencyResource(
    private val repository: EmergencyProductRepository
) {

    @GetMapping
    fun getAllProducts(): List<EmergencyProduct> {
        return repository.findAll()
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: UUID): ResponseEntity<EmergencyProduct> {
        val product = repository.findById(id)
        return if (product != null) {
            ok(product)
        } else {
            notFound().build()
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@RequestBody product: EmergencyProduct): EmergencyProduct {
        return repository.save(product)
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: UUID,
        @RequestBody updatedProduct: EmergencyProduct
    ): ResponseEntity<EmergencyProduct> {
        // Ensuring the ID from path is used, even if the body has a different ID
        val productToSave = updatedProduct.copy(id = id)
        val existing = repository.findById(id)
        return if (existing != null) {
            val savedProduct = repository.save(productToSave)
            ok(savedProduct)
        } else {
            notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: UUID): ResponseEntity<Void> {
        val removed = repository.deleteById(id)
        return if (removed) {
            noContent().build()
        } else {
            notFound().build()
        }
    }
}
