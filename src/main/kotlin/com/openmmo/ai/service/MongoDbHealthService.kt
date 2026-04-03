package com.openmmo.ai.service

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import com.mongodb.client.MongoClient
import org.bson.Document

/**
 * Service để test và verify MongoDB connection
 */
@Service
class MongoDbHealthService(
    private val mongoTemplate: MongoTemplate,
    private val mongoClient: MongoClient
) {

    /**
     * Test kết nối MongoDB
     * @return Thông tin kết nối và trạng thái
     */
    fun testConnection(): ConnectionStatus {
        return try {
            // Test 1: Check MongoDB client connection
            val serverAddress = mongoClient.clusterDescription.clusterSettings.hosts.firstOrNull()
            
            // Test 2: Ping database
            val pingResult = mongoTemplate.db.runCommand(Document("ping", 1))
            
            // Test 3: Get database info
            val dbName = mongoTemplate.db.name
            val collections = mongoTemplate.db.listCollectionNames().toList()
            
            ConnectionStatus(
                success = true,
                message = "[OK] MongoDB connection successful",
                details = mapOf(
                    "server" to serverAddress.toString(),
                    "database" to dbName,
                    "collections" to collections.size,
                    "ping_response" to "OK"
                ),
                dbName = dbName,
                collections = collections,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            ConnectionStatus(
                success = false,
                message = "[ERROR] MongoDB connection failed: ${e.message}",
                error = e.message,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Get MongoDB server info
     */
    fun getServerInfo(): Map<String, Any?> {
        return try {
            val admin = mongoClient.getDatabase("admin")
            val serverInfo = admin.runCommand(Document("serverStatus", 1))
            
            mapOf(
                "version" to (serverInfo.getString("version") ?: "unknown"),
                "uptime_seconds" to (serverInfo.getInteger("uptime") ?: 0),
                "connections" to (serverInfo.get("connections")?.toString() ?: "N/A"),
                "timestamp" to System.currentTimeMillis()
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            )
        }
    }

    /**
     * Test write/read operation
     */
    fun testReadWrite(): ReadWriteTest {
        return try {
            val testCollection = mongoTemplate.db.getCollection("_connection_test")
            val testDoc = Document("test", true)
                .append("timestamp", System.currentTimeMillis())
            
            // Write test
            testCollection.insertOne(testDoc)
            
            // Read test
            val retrieved = testCollection.find().first()
            
            // Cleanup
            testCollection.deleteOne(Document("test", true))
            
            ReadWriteTest(
                success = retrieved != null,
                message = "[OK] Write/Read test successful",
                writeSuccess = true,
                readSuccess = retrieved != null
            )
        } catch (e: Exception) {
            ReadWriteTest(
                success = false,
                message = "[ERROR] Write/Read test failed: ${e.message}",
                error = e.message
            )
        }
    }
}

data class ConnectionStatus(
    val success: Boolean,
    val message: String,
    val details: Map<String, Any?>? = null,
    val dbName: String? = null,
    val collections: List<String>? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReadWriteTest(
    val success: Boolean,
    val message: String,
    val writeSuccess: Boolean? = null,
    val readSuccess: Boolean? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
