package com.openmmo.ai.controller

import com.openmmo.ai.service.MongoDbHealthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller để test MongoDB connection
 */
@RestController
@RequestMapping("/api/v1/health")
class HealthController(
    private val mongoDbHealthService: MongoDbHealthService
) {

    /**
     * GET /api/v1/health/mongodb
     * Test kết nối MongoDB
     */
    @GetMapping("/mongodb")
    fun testMongoDbConnection(): ResponseEntity<Map<String, Any?>> {
        val status = mongoDbHealthService.testConnection()
        
        return if (status.success) {
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to status.message,
                "connection" to status.details,
                "database" to status.dbName,
                "collections" to status.collections,
                "timestamp" to status.timestamp
            ))
        } else {
            ResponseEntity.status(503).body(mapOf(
                "status" to "error",
                "message" to status.message,
                "error" to (status.error ?: "Unknown error"),
                "timestamp" to status.timestamp
            ))
        }
    }

    /**
     * GET /api/v1/health/mongodb/server-info
     * Get MongoDB server information
     */
    @GetMapping("/mongodb/server-info")
    fun getMongoDbServerInfo(): ResponseEntity<Map<String, Any?>> {
        val info = mongoDbHealthService.getServerInfo()
        
        return if (info.containsKey("error")) {
            ResponseEntity.status(503).body(info)
        } else {
            ResponseEntity.ok(info)
        }
    }

    /**
     * GET /api/v1/health/mongodb/test-read-write
     * Test write and read operations to MongoDB
     */
    @GetMapping("/mongodb/test-read-write")
    fun testReadWrite(): ResponseEntity<Map<String, Any?>> {
        val testResult = mongoDbHealthService.testReadWrite()
        
        return if (testResult.success) {
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to testResult.message,
                "write_test" to testResult.writeSuccess,
                "read_test" to testResult.readSuccess,
                "timestamp" to testResult.timestamp
            ))
        } else {
            ResponseEntity.status(503).body(mapOf(
                "status" to "error",
                "message" to testResult.message,
                "error" to (testResult.error ?: "Unknown error"),
                "timestamp" to testResult.timestamp
            ))
        }
    }

    @GetMapping("/full")
    fun fullHealthCheck(): ResponseEntity<Map<String, Any?>> {
        val mongoConnection = mongoDbHealthService.testConnection()
        val serverInfo = mongoDbHealthService.getServerInfo()
        val readWriteTest = mongoDbHealthService.testReadWrite()
        
        val allHealthy = mongoConnection.success && !serverInfo.containsKey("error") && readWriteTest.success
        
        val resultMap: MutableMap<String, Any?> = mutableMapOf()
        
        if (allHealthy) {
            resultMap["status"] = "healthy"
            resultMap["mongodb_connection"] = mapOf(
                "status" to "Connected",
                "database" to mongoConnection.dbName,
                "collections" to (mongoConnection.collections?.size ?: 0)
            )
            resultMap["server_info"] = serverInfo as Map<String, Any?>
            resultMap["read_write_test"] = mapOf(
                "status" to "OK",
                "write_ok" to readWriteTest.writeSuccess,
                "read_ok" to readWriteTest.readSuccess
            )
            resultMap["timestamp"] = System.currentTimeMillis()
            return ResponseEntity.ok(resultMap)
        } else {
            resultMap["status"] = "unhealthy"
            resultMap["mongodb_connection"] = mapOf(
                "status" to if (mongoConnection.success) "Connected" else "Failed",
                "error" to mongoConnection.error
            )
            resultMap["server_info"] = serverInfo as Map<String, Any?>
            resultMap["read_write_test"] = mapOf(
                "status" to if (readWriteTest.success) "OK" else "Failed",
                "error" to readWriteTest.error
            )
            resultMap["timestamp"] = System.currentTimeMillis()
            return ResponseEntity.status(503).body(resultMap)
        }
    }
}
