# PowerShell 脚本：终止占用 8080 端口的所有进程

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "修复端口占用问题 - PowerShell 版本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 查找占用 8080 端口的进程
Write-Host "[1] 正在查找占用 8080 端口的进程..." -ForegroundColor Yellow
$connections = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

if ($null -eq $connections -or $connections.Count -eq 0) {
    Write-Host "✓ 端口 8080 未被占用，可以直接启动服务器！" -ForegroundColor Green
    exit 0
}

Write-Host "找到以下进程占用端口 8080:" -ForegroundColor Yellow
$connections | ForEach-Object {
    $process = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "  PID: $($_.OwningProcess) - $($process.ProcessName)" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "[2] 正在终止所有占用端口的进程..." -ForegroundColor Yellow

# 终止所有占用端口的进程
$killed = 0
$connections | ForEach-Object {
    $pid = $_.OwningProcess
    try {
        Stop-Process -Id $pid -Force -ErrorAction Stop
        Write-Host "  ✓ 进程 $pid 已终止" -ForegroundColor Green
        $killed++
    } catch {
        Write-Host "  ✗ 无法终止进程 $pid : $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "[3] 验证端口是否已释放..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

$remaining = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($null -eq $remaining -or $remaining.Count -eq 0) {
    Write-Host "✓ 端口 8080 已成功释放！" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "现在可以重新启动服务器了！" -ForegroundColor Green
    Write-Host "运行: ./gradlew run" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
} else {
    Write-Host "✗ 端口仍被占用，请手动检查" -ForegroundColor Red
}


