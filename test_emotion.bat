@echo off
chcp 65001 >nul
echo ========================================
echo 微微情绪系统测试脚本
echo ========================================
echo.

echo [选项1] 运行完整测试流程（推荐）
echo 这会运行所有测试场景，显示详细结果
echo.
curl http://localhost:8080/test-emotion-full
echo.
echo.

echo ========================================
echo 或者运行单独测试：
echo ========================================
echo.

echo [选项2] 查看当前情绪状态
curl http://localhost:8080/test-emotion?type=status
echo.
echo.

echo [选项3] 设置初始情绪（HAPPY，强度 0.7）
curl http://localhost:8080/test-emotion?type=basic
echo.
echo.

echo [选项4] 测试称赞事件（应该提升高兴情绪）
curl http://localhost:8080/test-emotion?type=praise
echo.
echo.

echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 提示：推荐使用选项1查看完整测试结果
pause

