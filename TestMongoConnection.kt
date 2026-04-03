import java.net.HttpURLConnection
import java.net.URL

fun main() {
    try {
        val url = URL("http://localhost:8081/api/v1/health/mongodb")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        
        val responseCode = connection.responseCode
        val responseBody = connection.inputStream.bufferedReader().readText()
        
        println("=".repeat(60))
        println("MongoDB Connection Test Result")
        println("=".repeat(60))
        println("Status Code: $responseCode")
        println("Response:")
        println(responseBody)
        println("=".repeat(60))
        
        if (responseCode in 200..299) {
            println("✅ CONNECTION SUCCESS")
        } else {
            println("❌ CONNECTION FAILED")
        }
    } catch (e: Exception) {
        println("❌ Error connecting to server: ${e.message}")
        e.printStackTrace()
    }
}
