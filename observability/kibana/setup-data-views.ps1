$ErrorActionPreference = "Stop"
$kibana = if ($env:KIBANA_URL) { $env:KIBANA_URL } else { "http://localhost:5601" }

$ndjson = Join-Path $PSScriptRoot "saved-objects.ndjson"
if (-not (Test-Path $ndjson)) { throw "missing $ndjson" }

Write-Host "==> Waiting for Kibana at $kibana ..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    try {
        $status = Invoke-RestMethod -Uri "$kibana/api/status" -Method Get -TimeoutSec 5
        if ($status.status.overall.level -eq "available") { break }
    } catch { }
    if ((Get-Date) -gt $deadline) { throw "Kibana did not become available within 3 minutes" }
    Start-Sleep -Seconds 5
}
Write-Host "    Kibana is available." -ForegroundColor Green

Write-Host "==> Importing saved objects from saved-objects.ndjson ..." -ForegroundColor Cyan
$raw = & curl.exe -s -X POST "$kibana/api/saved_objects/_import?overwrite=true" `
    -H "kbn-xsrf: true" -F "file=@$ndjson"
$result = $raw | ConvertFrom-Json

if (-not $result.success) {
    Write-Host "Import reported errors:" -ForegroundColor Red
    $result.errors | ForEach-Object { Write-Host "  $($_.type) $($_.id): $($_.error | ConvertTo-Json -Compress)" -ForegroundColor Red }
    throw "saved object import failed"
}
Write-Host "    imported $($result.successCount) saved objects." -ForegroundColor Green

$api = if ($env:QUERY_API_URL) { $env:QUERY_API_URL } else { "http://localhost:8080" }
$es  = if ($env:ELASTIC_URL)   { $env:ELASTIC_URL }   else { "http://localhost:9200" }

$experiments = $null
try {
    $experiments = Invoke-RestMethod -Uri "$api/experiments" -Method Get -TimeoutSec 10
} catch {
    Write-Host "==> query-api not reachable, skipping backfill." -ForegroundColor Yellow
}

if ($experiments) {
    try {
        Invoke-RestMethod -Uri "$es/block_results/_mapping" -Method Put `
            -ContentType "application/json" `
            -Body '{"properties":{"experimentId":{"type":"keyword"},"experimentLabel":{"type":"keyword"}}}' | Out-Null
    } catch {
        Write-Host "    could not set experimentId mapping (already typed?): $($_.Exception.Message)" -ForegroundColor Yellow
    }

    $rosterExists = $true
    try { Invoke-RestMethod -Uri "$es/miner_roster" -Method Get | Out-Null } catch { $rosterExists = $false }
    if (-not $rosterExists) {
        Invoke-RestMethod -Uri "$es/miner_roster" -Method Put -ContentType "application/json" -Body (@{
            mappings = @{ properties = @{
                experimentId = @{ type = "keyword" }
                minerId      = @{ type = "integer" }
                stake        = @{ type = "integer" }
                stakePct     = @{ type = "double" }
                totalStake   = @{ type = "integer" }
                numMiners    = @{ type = "integer" }
            } }
        } | ConvertTo-Json -Depth 10) | Out-Null
        Write-Host "    created miner_roster with explicit mapping." -ForegroundColor Green
    }

    Write-Host "==> Backfilling experimentId and miner roster for $($experiments.Count) experiments..." -ForegroundColor Cyan
    $stamped = 0
    $rostered = 0

    foreach ($exp in $experiments) {
        $detail = Invoke-RestMethod -Uri "$api/experiments/$($exp.id)" -Method Get

        if ($detail.runs -and $detail.runs.Count -gt 0) {
            $runIds = @($detail.runs | ForEach-Object { $_.id })
            $ds = "$($exp.datasetHash)"
            if ($ds.Length -gt 8) { $ds = $ds.Substring(0, 8) }
            $started = ([datetime]$exp.startedAt).ToUniversalTime().ToString("yyyy-MM-dd")
            $label = "seed=$($exp.seed) miners=$($exp.numMiners) ds=$ds $started"

            $body = @{
                script = @{
                    source = "ctx._source.experimentId = params.eid; ctx._source.experimentLabel = params.lbl"
                    lang   = "painless"
                    params = @{ eid = $exp.id; lbl = $label }
                }
                query = @{
                    bool = @{
                        must     = @(@{ terms = @{ runId = $runIds } })
                        must_not = @(@{ exists = @{ field = "experimentLabel" } })
                    }
                }
            } | ConvertTo-Json -Depth 10 -Compress

            $res = Invoke-RestMethod -Uri "$es/block_results/_update_by_query?refresh=true&conflicts=proceed" `
                -Method Post -ContentType "application/json" -Body $body
            $stamped += $res.updated
        }

        $miners = Invoke-RestMethod -Uri "$api/experiments/$($exp.id)/miners" -Method Get
        if ($miners -and $miners.Count -gt 0) {
            $totalStake = ($miners | Measure-Object -Property stake -Sum).Sum
            $lines = New-Object System.Collections.Generic.List[string]
            foreach ($m in $miners) {
                $docId = "$($exp.id):$($m.minerId)"
                $lines.Add((@{ index = @{ _index = "miner_roster"; _id = $docId } } | ConvertTo-Json -Compress -Depth 5))
                $lines.Add((@{
                    experimentId = $exp.id
                    minerId      = [int]$m.minerId
                    stake        = [int]$m.stake
                    stakePct     = [double]$m.stakePct
                    totalStake   = [int]$totalStake
                    numMiners    = [int]$miners.Count
                } | ConvertTo-Json -Compress -Depth 5))
            }
            $ndjson = ($lines -join "`n") + "`n"
            $bulk = Invoke-RestMethod -Uri "$es/_bulk?refresh=true" -Method Post `
                -ContentType "application/x-ndjson" -Body $ndjson
            if ($bulk.errors) {
                Write-Host "    bulk roster index reported errors for $($exp.id)" -ForegroundColor Red
            } else {
                $rostered += $miners.Count
            }
        }
    }

    Write-Host "    stamped $stamped blocks with experimentId; indexed $rostered roster entries." -ForegroundColor Green
}

Write-Host ""
Write-Host "Dashboard: $kibana/app/dashboards#/view/tx-analytics-fee-mechanisms" -ForegroundColor Cyan
Write-Host "NOTE: timestamps are SIMULATED chain time (June 2023), not wall-clock." -ForegroundColor Yellow
Write-Host "      The dashboard pins that range for you; in Discover set it yourself." -ForegroundColor Yellow
