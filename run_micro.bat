@echo off
chcp 65001
cd /d %~dp0

echo [微微] 正在編譯靈魂模組...

kotlinc ^
src\main\kotlin\Main.kt ^
src\main\kotlin\models\*.kt ^
src\main\kotlin\services\*.kt ^
-include-runtime -d test.jar

if %errorlevel% neq 0 (
    echo [微微] 編譯失敗，請檢查錯誤訊息。
    pause
    exit /b
)

echo [微微] 啟動中...
java -jar test.jar

echo.
echo [微微] 已完成執行，請按任意鍵結束。
pause