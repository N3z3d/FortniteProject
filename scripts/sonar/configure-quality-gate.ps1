param(
    [string]$SonarHostUrl = "http://localhost:9000",
    [string]$GateName = "Fortnite Local Gate",
    [string[]]$ProjectKeys = @("fortnite-pronos-backend", "fortnite-pronos-frontend"),
    [string]$SonarAdminUser = "admin",
    [string]$SonarAdminPassword = "admin"
)

$authBytes = [Text.Encoding]::ASCII.GetBytes("${SonarAdminUser}:${SonarAdminPassword}")
$authHeader = [Convert]::ToBase64String($authBytes)
$headers = @{ Authorization = "Basic $authHeader" }

function Invoke-SonarApiGet {
    param([string]$PathWithQuery)
    return Invoke-WebRequest -UseBasicParsing -Headers $headers -Uri "$SonarHostUrl$PathWithQuery"
}

function Invoke-SonarApiPost {
    param([string]$PathWithQuery)
    return Invoke-WebRequest -UseBasicParsing -Method Post -Headers $headers -Uri "$SonarHostUrl$PathWithQuery"
}

$encodedGateName = [Uri]::EscapeDataString($GateName)

try {
    $gateResponse = Invoke-SonarApiGet "/api/qualitygates/show?name=$encodedGateName"
} catch {
    Invoke-SonarApiPost "/api/qualitygates/create?name=$encodedGateName" | Out-Null
    $gateResponse = Invoke-SonarApiGet "/api/qualitygates/show?name=$encodedGateName"
}

$gateJson = $gateResponse.Content | ConvertFrom-Json
$existingConditions = @($gateJson.conditions)
foreach ($condition in $existingConditions) {
    Invoke-SonarApiPost "/api/qualitygates/delete_condition?id=$($condition.id)" | Out-Null
}

$conditions = @(
    @{ metric = "new_blocker_violations"; op = "GT"; error = "0" },
    @{ metric = "new_critical_violations"; op = "GT"; error = "0" },
    @{ metric = "new_reliability_rating"; op = "GT"; error = "1" },
    @{ metric = "new_security_rating"; op = "GT"; error = "1" },
    @{ metric = "new_maintainability_rating"; op = "GT"; error = "1" },
    @{ metric = "new_coverage"; op = "LT"; error = "30" },
    @{ metric = "new_duplicated_lines_density"; op = "GT"; error = "5" }
)

foreach ($condition in $conditions) {
    $query = "/api/qualitygates/create_condition?gateName=$encodedGateName&metric=$($condition.metric)&op=$($condition.op)&error=$($condition.error)"
    Invoke-SonarApiPost $query | Out-Null
}

foreach ($projectKey in $ProjectKeys) {
    $encodedProject = [Uri]::EscapeDataString($projectKey)
    Invoke-SonarApiPost "/api/qualitygates/select?gateName=$encodedGateName&projectKey=$encodedProject" | Out-Null
}

$result = @()
foreach ($projectKey in $ProjectKeys) {
    $encodedProject = [Uri]::EscapeDataString($projectKey)
    $statusResponse = Invoke-SonarApiGet "/api/qualitygates/project_status?projectKey=$encodedProject"
    $statusJson = $statusResponse.Content | ConvertFrom-Json
    $result += [PSCustomObject]@{
        ProjectKey = $projectKey
        GateName = $GateName
        Status = $statusJson.projectStatus.status
    }
}

$result | Format-Table -AutoSize
