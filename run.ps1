# One-command dev runner: brings up Kafka + Postgres, then boots all 3
# Spring Boot modules in the background and streams their logs here.
#
# Usage:   .\run.ps1
# Stop:    Ctrl+C   (stops the 3 app processes; Kafka/Postgres keep running
#                     in Docker - run `docker compose down` separately if
#                     you want to tear those down too)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$modules = @("producer", "simulation", "indexer")
# indexer now consumes blocks.results into Mongo. common module is a library
# (no runnable app), don't try to boot it.

Write-Host "==> Starting Kafka + Postgres (docker compose)..." -ForegroundColor Cyan
docker compose -f "$root\docker-compose.yml" up -d

$jobs = @()
foreach ($m in $modules) {
    Write-Host "==> Launching $m (mvn spring-boot:run)..." -ForegroundColor Cyan
    $job = Start-Job -Name $m -ScriptBlock {
        param($root, $module)
        Set-Location $root
        mvn -pl $module spring-boot:run
    } -ArgumentList $root, $m
    $jobs += $job
}

Write-Host "==> All services starting. Streaming logs below (Ctrl+C to stop)." -ForegroundColor Cyan
Write-Host ""

try {
    while ($true) {
        foreach ($job in $jobs) {
            $lines = Receive-Job -Job $job
            foreach ($line in $lines) {
                Write-Host "[$($job.Name)] $line"
            }
        }
        Start-Sleep -Milliseconds 500
    }
}
finally {
    Write-Host ""
    Write-Host "==> Stopping app processes (Kafka/Postgres left running)..." -ForegroundColor Yellow
    $jobs | Stop-Job
    $jobs | Remove-Job
}
