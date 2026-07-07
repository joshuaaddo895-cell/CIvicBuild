# CivicBuild API route smoke test — run while server is up (default port 8081)
# Usage: .\scripts\test-routes.ps1

$BaseUrl = "http://localhost:8081"
$Password = "Secret123"
$Email = "route-test-$(Get-Random)@example.com"
$Results = @()

function Test-Route {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [int[]]$ExpectedStatus
    )
    $uri = "$BaseUrl$Path"
    $status = 0
    $content = $null
    try {
        $params = @{
            Uri             = $uri
            Method          = $Method
            ContentType     = "application/json"
            UseBasicParsing = $true
        }
        if ($Body) { $params.Body = ($Body | ConvertTo-Json -Compress) }
        $response = Invoke-WebRequest @params
        $status = [int]$response.StatusCode
        if ($response.Content) { $content = $response.Content | ConvertFrom-Json }
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $raw = $reader.ReadToEnd()
                if ($raw) { $content = $raw | ConvertFrom-Json }
            } catch { }
        }
    }

    $pass = $ExpectedStatus -contains $status
    $script:Results += [PSCustomObject]@{
        Test    = $Name
        Method  = $Method
        Path    = $Path
        Status  = $status
        Pass    = $pass
        Message = if ($content -and $content.message) { $content.message } else { "" }
    }
    Write-Host "$(if ($pass) { 'PASS' } else { 'FAIL' }) $Name -> $status"
    return $content
}

Write-Host "`n=== CivicBuild Route Tests ===" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl`n"

Test-Route "Health check" GET "/api/health" -ExpectedStatus @(200) | Out-Null
Test-Route "Actuator health" GET "/actuator/health" -ExpectedStatus @(200) | Out-Null

Test-Route "Register" POST "/api/auth/register" -Body @{
    fullName = "Route Test User"; email = $Email; password = $Password
} -ExpectedStatus @(201) | Out-Null

Test-Route "Register duplicate email" POST "/api/auth/register" -Body @{
    fullName = "Another"; email = $Email; password = $Password
} -ExpectedStatus @(409) | Out-Null

Test-Route "Login wrong password" POST "/api/auth/login" -Body @{
    email = $Email; password = "WrongPass1"
} -ExpectedStatus @(401) | Out-Null

$login = Test-Route "Login" POST "/api/auth/login" -Body @{
    email = $Email; password = $Password
} -ExpectedStatus @(200)

$refreshToken = $login.data.refreshToken
$refreshed = Test-Route "Refresh token" POST "/api/auth/refresh" -Body @{
    refreshToken = $refreshToken
} -ExpectedStatus @(200)

$newRefresh = $refreshed.data.refreshToken

Test-Route "Refresh with revoked token" POST "/api/auth/refresh" -Body @{
    refreshToken = $refreshToken
} -ExpectedStatus @(401) | Out-Null

Test-Route "Forgot password" POST "/api/auth/forgot-password" -Body @{
    email = $Email
} -ExpectedStatus @(200) | Out-Null

Test-Route "Forgot password unknown email" POST "/api/auth/forgot-password" -Body @{
    email = "nobody-$(Get-Random)@example.com"
} -ExpectedStatus @(200) | Out-Null

Test-Route "Reset password invalid token" POST "/api/auth/reset-password" -Body @{
    token = "invalid-token-value"; newPassword = "NewSecret456"
} -ExpectedStatus @(401) | Out-Null

Test-Route "Logout" POST "/api/auth/logout" -Body @{
    refreshToken = $newRefresh
} -ExpectedStatus @(200) | Out-Null

Test-Route "Refresh after logout" POST "/api/auth/refresh" -Body @{
    refreshToken = $newRefresh
} -ExpectedStatus @(401) | Out-Null

Test-Route "Register validation error" POST "/api/auth/register" -Body @{
    fullName = ""; email = "bad"; password = "short"
} -ExpectedStatus @(400) | Out-Null

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
$passed = ($Results | Where-Object { $_.Pass }).Count
$total = $Results.Count
Write-Host "$passed / $total tests passed"
$Results | Format-Table -AutoSize
if ($passed -lt $total) { exit 1 }
