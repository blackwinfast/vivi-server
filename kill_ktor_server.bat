@echo off
chcp 65001 >nul
echo ========================================
echo 终止所有 Ktor 服务器进程
echo ========================================
echo.

echo [1] 正在查找所有 Ktor 服务器进程...
jps -l | findstr EngineMain
if %errorlevel% neq 0 (
    echo 未找到运行中的 Ktor 服务器
    goto :check_port
)

echo.
echo [2] 正在终止所有 Ktor 服务器进程...
for /f "tokens=1" %%a in ('jps -l ^| findstr EngineMain') do (
    echo   找到 Ktor 服务器进程 PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo   ✓ 进程 %%a 已终止
    ) else (
        echo   ✗ 无法终止进程 %%a
    )
)

:check_port
echo.
echo [3] 检查 8080 端口占用情况...
netstat -ano | findstr :8080
if %errorlevel% neq 0 (
    echo ✓ 端口 8080 未被占用
) else (
    echo ⚠ 端口 8080 仍被占用，正在清理...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
        echo   终止占用端口的进程: %%a
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    netstat -ano | findstr :8080
    if %errorlevel% neq 0 (
        echo ✓ 端口已释放
    ) else (
        echo ✗ 端口仍被占用
    )
)

echo.
echo ========================================
echo 清理完成！现在可以启动服务器了
echo 运行: ./gradlew run
echo ========================================
pause


