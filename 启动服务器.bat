@echo off
chcp 65001 >nul
echo ========================================
echo 启动 Ktor 服务器
echo ========================================
echo.

echo [1] 检查并清理旧进程...
call kill_ktor_server.bat

echo.
echo [2] 等待端口释放...
timeout /t 2 /nobreak >nul

echo.
echo [3] 启动服务器...
echo.
echo ========================================
echo 服务器正在启动，请稍候...
echo 提示: 不要关闭这个窗口！
echo 按 Ctrl+C 可以停止服务器
echo ========================================
echo.
echo 等待服务器完全启动后，在浏览器打开：
echo http://localhost:8080/test-emotion-full
echo.
echo ========================================
echo.

./gradlew run --no-daemon

