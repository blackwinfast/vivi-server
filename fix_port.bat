@echo off
chcp 65001 >nul
echo ========================================
echo 修复端口占用问题 - 自动终止所有占用 8080 的进程
echo ========================================
echo.

echo [1] 正在查找占用 8080 端口的进程...
netstat -ano | findstr :8080
if %errorlevel% neq 0 (
    echo 端口 8080 未被占用，可以直接启动服务器！
    goto :end
)

echo.
echo [2] 正在终止所有占用端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo   找到进程ID: %%a
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo   ✓ 进程 %%a 已终止
    ) else (
        echo   ✗ 无法终止进程 %%a（可能需要管理员权限）
    )
)

echo.
echo [3] 验证端口是否已释放...
timeout /t 2 /nobreak >nul
netstat -ano | findstr :8080
if %errorlevel% neq 0 (
    echo ✓ 端口 8080 已释放！
) else (
    echo ✗ 端口仍被占用，请手动检查
)

:end
echo.
echo ========================================
echo 现在可以重新启动服务器了！
echo 运行: ./gradlew run
echo ========================================
pause

