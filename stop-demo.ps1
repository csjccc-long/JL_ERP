$jarFragment = "ruoyi-admin"
$procs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -like "*ruoyi-admin*" }
if (-not $procs) { Write-Host "no ruoyi-admin java process found"; exit 0 }
foreach ($p in $procs) {
    Write-Host ("stopping pid " + $p.ProcessId)
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}
Write-Host "done"
