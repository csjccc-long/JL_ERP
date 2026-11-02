param(
    [int]$Port = 18085,
    [string]$DbPassword = "123456",
    [string]$JwtSecret = "verify-secret-0123456789"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar  = Join-Path $root "ruoyi-admin\target\ruoyi-admin.jar"
if (-not (Test-Path $jar)) { throw "jar not found: $jar  (build with: mvn -DskipTests package)" }

$env:ERP_DB_PASSWORD = $DbPassword
$env:ERP_JWT_SECRET  = $JwtSecret

$javaHome = "C:\Program Files\Java\jdk-23"
if (Test-Path (Join-Path $javaHome "bin\java.exe")) {
    $java = Join-Path $javaHome "bin\java.exe"
} else {
    $java = "java"
}

$logDir = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$out = Join-Path $logDir ("app-" + $stamp + ".log")
$err = Join-Path $logDir ("app-" + $stamp + ".err.log")

Start-Process -FilePath $java -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err -ArgumentList @(
    "-jar", $jar,
    "--server.port=$Port",
    "--logging.level.com.ruoyi=info"
)

Write-Host "Backend starting on port $Port"
Write-Host "  log    : $out"
Write-Host "  health : http://127.0.0.1:$Port/actuator/health"
Write-Host "  ui     : cd ruoyi-ui; npm run dev"
