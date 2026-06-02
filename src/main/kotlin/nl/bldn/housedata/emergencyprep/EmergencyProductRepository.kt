package nl.bldn.housedata.emergencyprep

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Repository
class EmergencyProductRepository(private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val storageFile = File("emergency_products.json")
    private val products = mutableListOf<EmergencyProduct>()
    private val lock = ReentrantReadWriteLock()

    @PostConstruct
    fun init() {
        if (storageFile.exists()) {
            try {
                val loadedProducts: List<EmergencyProduct> = objectMapper.readValue(
                    storageFile,
                    object : TypeReference<List<EmergencyProduct>>() {}
                )
                products.addAll(loadedProducts)
                log.info("Loaded ${products.size} emergency products from ${storageFile.absolutePath}")
            } catch (e: Exception) {
                log.error("Failed to load emergency products from file", e)
            }
        } else {
            log.info("No existing storage file found at ${storageFile.absolutePath}. A new one will be created upon saving.")
        }
    }

    private fun saveToFile() {
        try {
            objectMapper.writeValue(storageFile, products)
        } catch (e: Exception) {
            log.error("Failed to save emergency products to file", e)
        }
    }

    fun findAll(): List<EmergencyProduct> = lock.read {
        products.toList()
    }

    fun findById(id: UUID): EmergencyProduct? = lock.read {
        products.find { it.id == id }
    }

    fun save(product: EmergencyProduct): EmergencyProduct = lock.write {
        val index = products.indexOfFirst { it.id == product.id }
        if (index != -1) {
            products[index] = product
        } else {
            products.add(product)
        }
        saveToFile()
        product
    }

    fun deleteById(id: UUID): Boolean = lock.write {
        val removed = products.removeIf { it.id == id }
        if (removed) {
            saveToFile()
        }
        removed
    }
}
