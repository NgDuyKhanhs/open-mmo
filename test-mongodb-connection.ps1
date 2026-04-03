$uri = "http://localhost:8081/api/v1/health/mongodb"

Write-Host ""
Write-Host "============================================================"
Write-Host "MongoDB Connection Test"
Write-Host "============================================================"
Write-Host "Testing endpoint: $uri"
Write-Host ""

try {
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
    
    $response = Invoke-WebRequest -Uri $uri -Method Get -TimeoutSec 10 -UseBasicParsing
    
    $statusCode = $response.StatusCode
    $responseBody = $response.Content
    
    Write-Host "Status Code: $statusCode"
    Write-Host ""
    Write-Host "Response Body:"
    Write-Host $responseBody
    Write-Host ""
    Write-Host "============================================================"
    
    if ($statusCode -eq 200) {
        Write-Host "✅ CONNECTION SUCCESS - MongoDB is connected!"
        Write-Host "============================================================"
        exit 0
    } else {
        Write-Host "⚠️  Server responded but with status: $statusCode"
        Write-Host "============================================================"
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "❌ CONNECTION ERROR"
    Write-Host "============================================================"
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host ""
    Write-Host "Possible causes:"
    Write-Host "1. Application not running on port 8081"
    Write-Host "2. MongoDB not running on localhost:27017"
    Write-Host "3. Network connectivity issue"
    Write-Host ""
    Write-Host "To start MongoDB:"
    Write-Host "  mongod --dbpath C:\data\db (or your MongoDB data directory)"
    Write-Host "============================================================"
    exit 1
}
