@echo off
chcp 65001 >nul
echo ========================================
echo 重启服务器（重新加载代码）
echo ========================================
echo.

echo [1] 停止旧服务器...
call kill_ktor_server.bat

echo.
echo [2] 重新编译代码...
./gradlew compileKotlin
if %errorlevel% neq 0 (
    echo 编译失败！请检查错误信息
    pause
    exit /b 1
)

echo.
echo [3] 启动新服务器...
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

