# Full-stack runner (command-driven, Phase 7b onward): brings up infra + the
# four long-running services. Runs are launched over REST, not by static config
# - the old "5 simulation JVMs, one per TFM" model is gone; now ONE simulation
# service consumes RunCommands and mines every requested TFM in-process.
#
# Usage:   .\run-fanout.ps1
# Then:    POST a command to launch a run, e.g.
#   curl -X POST http://localhost:8080/simulations -H "Content-Type: application/json" -d '{
#     "seed": 156915, "numMiners": 10, "label": "study-1",
#     "tfms": {
#       "first_price": {"size_limit":"2000000"},
#       "second_price": {"size_limit":"2000000"},
#       "burning_second_price": {"size_limit":"2000000"},
#       "eip1559": {"size_limit":"4000000","target":"2000000","base_fee":"0.0000002333"},
#       "reserve_pool": {"size_limit":"4000000","target":"2000000","base_fee":"0.0000002333","reserve_base":"134.38","window":"144"}
#     }}'
# Poll results at http://localhost:8080/experiments and /runs.
#
# Stop:    Ctrl+C   (stops the app processes; Docker infra keeps running -
#                     `docker compose down` separately to tear it down)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "==> Starting infra (Kafka, Postgres, Mongo, Elasticsearch, Redis, Kibana, Prometheus, Grafana)..." -ForegroundColor Cyan
docker compose -f "$root\docker-compose.yml" up -d

# Kibana keeps its data views, charts and dashboard in its own .kibana index
# rather than reading them off disk, so they have to be pushed in over the API
# once Kibana is up. Runs in the background so it does not delay the services;
# it is idempotent, so re-running this script just overwrites them.
Write-Host "==> Importing Kibana saved objects (background)..." -ForegroundColor Cyan
$kibanaJob = Start-Job -Name "kibana-setup" -ScriptBlock {
    param($script)
    & $script
} -ArgumentList (Join-Path $root "observability\kibana\setup-data-views.ps1")

# name -> maven module. The producer publishes the dataset to transactions.raw
# and exits; the other three are long-running services.
$services = @(
    @{ Name = "producer";   Module = "producer" },
    @{ Name = "simulation"; Module = "simulation" },
    @{ Name = "indexer";    Module = "indexer" },
    @{ Name = "query-api";  Module = "query-api" }
)

$jobs = @()
foreach ($svc in $services) {
    Write-Host "==> Launching $($svc.Name)..." -ForegroundColor Cyan
    $job = Start-Job -Name $svc.Name -ScriptBlock {
        param($root, $module)
        Set-Location $root
        mvn -pl $module spring-boot:run
    } -ArgumentList $root, $svc.Module
    $jobs += $job
}

Write-Host "==> Services starting. Once query-api is up, POST to /simulations to launch a run." -ForegroundColor Cyan
Write-Host ""
Write-Host "    Grafana    http://localhost:3000  (pipeline health)" -ForegroundColor DarkGray
Write-Host "    Prometheus http://localhost:9090  (metric store / target health)" -ForegroundColor DarkGray
Write-Host "    Kibana     http://localhost:5601/app/dashboards#/view/tx-analytics-fee-mechanisms" -ForegroundColor DarkGray
Write-Host ""

$kibanaReported = $false

try {
    while ($true) {
        foreach ($job in $jobs) {
            $lines = Receive-Job -Job $job
            foreach ($line in $lines) {
                Write-Host "[$($job.Name)] $line"
            }
        }
        if (-not $kibanaReported -and $kibanaJob.State -ne "Running") {
            Receive-Job -Job $kibanaJob | ForEach-Object { Write-Host "[kibana] $_" -ForegroundColor DarkGray }
            if ($kibanaJob.State -eq "Failed") {
                Write-Host "[kibana] saved-object import FAILED - dashboard will be missing." -ForegroundColor Red
            }
            $kibanaReported = $true
        }
        Start-Sleep -Milliseconds 500
    }
}
finally {
    Write-Host ""
    Write-Host "==> Stopping app processes (Docker infra left running)..." -ForegroundColor Yellow
    $jobs | Stop-Job
    $jobs | Remove-Job
    if ($kibanaJob) { Stop-Job $kibanaJob -ErrorAction SilentlyContinue; Remove-Job $kibanaJob -ErrorAction SilentlyContinue }
}
