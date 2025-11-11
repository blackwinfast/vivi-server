@echo off
chcp 65001 >nul
echo ========================================
echo 启动 Ktor 服务器（后台模式）
echo ========================================
echo.

echo [1] 检查并清理旧进程...
call kill_ktor_server.bat

echo.
echo [2] 等待端口释放...
timeout /t 2 /nobreak >nul

echo.
echo [3] 在后台启动服务器...
start "Ktor Server" cmd /c "cd /d %~dp0 && ./gradlew run --no-daemon"

echo.
echo ========================================
echo 服务器正在后台启动...
echo 请等待 10-15 秒后访问：
echo http://localhost:8080/test-emotion-full
echo.
echo 要停止服务器，运行: kill_ktor_server.bat
echo ========================================
echo.

timeout /t 3 /nobreak >nul
echo 检查服务器状态...
netstat -ano | findstr :8080
if %errorlevel% equ 0 (
    echo.
    echo ✓ 服务器已启动！
) else (
    echo.
    echo ⚠ 服务器可能还在启动中，请稍候...
)

pause


