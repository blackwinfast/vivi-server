# 微微情绪系统测试指南

## 方法1：通过 API 端点测试（推荐）

### 1. 启动服务器

```bash
./gradlew run
```

服务器会在 `http://localhost:8080` 启动

### 2. 测试端点

#### 基础测试：查看当前情绪状态
```bash
curl http://localhost:8080/test-emotion?type=status
```

#### 设置初始情绪（HAPPY，强度 0.7）
```bash
curl http://localhost:8080/test-emotion?type=basic
```

#### 测试称赞事件
```bash
curl http://localhost:8080/test-emotion?type=praise
```

#### 指定用户ID测试
```bash
curl http://localhost:8080/test-emotion?type=status&user_id=user_01
```

### 3. 通过浏览器测试

直接在浏览器中访问：
- `http://localhost:8080/test-emotion?type=status` - 查看情绪状态
- `http://localhost:8080/test-emotion?type=basic` - 基础测试
- `http://localhost:8080/test-emotion?type=praise` - 测试称赞

## 方法2：运行 Main.kt 测试程序

### 方式A：使用 Gradle 运行（需要修改配置）

1. 修改 `build.gradle.kts`，添加测试任务：

```kotlin
tasks.register<JavaExec>("runTest") {
    group = "application"
    description = "运行情绪系统测试"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.MainKt")
}
```

2. 运行测试：
```bash
./gradlew runTest
```

### 方式B：直接运行编译后的类

1. 编译项目：
```bash
./gradlew build
```

2. 运行 Main.kt：
```bash
java -cp "build/classes/kotlin/main:build/resources/main:$(./gradlew printClasspath -q)" com.example.MainKt
```

## 方法3：通过 /chat 端点测试（完整流程）

### 1. 启动服务器
```bash
./gradlew run
```

### 2. 发送测试请求

#### 测试1：称赞微微（应该提升高兴情绪）
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d "{\"user_id\": \"test_user\", \"input_text\": \"微微你好棒！\"}"
```

#### 测试2：再次称赞（测试疲乏效应）
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d "{\"user_id\": \"test_user\", \"input_text\": \"微微你好棒！\"}"
```

#### 测试3：道歉（测试情绪修复）
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d "{\"user_id\": \"test_user\", \"input_text\": \"抱歉，刚才说错了\"}"
```

#### 测试4：查看情绪状态
```bash
curl http://localhost:8080/test-emotion?type=status&user_id=test_user
```

## 测试场景说明

### 场景1：初始情绪设置
- 设置微微情绪为 HAPPY，强度 0.7
- 预期：情绪状态正确显示

### 场景2：称赞事件
- 用户称赞微微
- 预期：高兴情绪提升（基础0.3，首次100% = 0.3）
- 最终强度应该约为 1.0（0.7 + 0.3）

### 场景3：重复称赞（疲乏效应）
- 再次称赞微微
- 预期：情绪提升幅度递减（第二次80% = 0.24）
- 最终强度应该约为 1.24

### 场景4：时间衰减
- 等待1小时后
- 预期：高兴情绪衰减（衰减速度 0.05/小时）
- 1小时后强度应该约为 1.19（1.24 - 0.05）

### 场景5：道歉修复
- 用户道歉
- 预期：生气情绪降低，高兴情绪提升
- 如果之前有生气情绪，应该会降低

## 快速测试脚本

创建一个 `test.bat`（Windows）或 `test.sh`（Linux/Mac）：

### Windows (test.bat)
```batch
@echo off
echo 测试1: 查看情绪状态
curl http://localhost:8080/test-emotion?type=status

echo.
echo 测试2: 基础测试
curl http://localhost:8080/test-emotion?type=basic

echo.
echo 测试3: 测试称赞
curl http://localhost:8080/test-emotion?type=praise

echo.
echo 测试4: 再次查看状态
curl http://localhost:8080/test-emotion?type=status
```

### Linux/Mac (test.sh)
```bash
#!/bin/bash
echo "测试1: 查看情绪状态"
curl http://localhost:8080/test-emotion?type=status

echo ""
echo "测试2: 基础测试"
curl http://localhost:8080/test-emotion?type=basic

echo ""
echo "测试3: 测试称赞"
curl http://localhost:8080/test-emotion?type=praise

echo ""
echo "测试4: 再次查看状态"
curl http://localhost:8080/test-emotion?type=status
```

## 预期输出示例

### 基础测试输出
```json
{
  "response_text": "测试完成！\n微微現在的情緒是：HAPPY，強度：0.7",
  "response_type": "CHAT_MESSAGE",
  "remaining_quota": null
}
```

### 状态查询输出
```json
{
  "response_text": "微微当前情绪状态：\n主要情绪：HAPPY\n强度：1.0\n\n所有情绪：\n  HAPPY: 1.0 (衰减速度: 0.05/小时)",
  "response_type": "CHAT_MESSAGE",
  "remaining_quota": null
}
```

## 注意事项

1. **用户ID**: 不同用户ID的情绪状态是独立的
2. **时间衰减**: 每次请求会自动应用时间衰减
3. **记忆**: 情绪记忆会记录最近100条事件
4. **持久化**: 目前数据存储在内存中，重启服务器会丢失

## 调试技巧

1. 查看服务器日志：所有操作都会记录在日志中
2. 使用相同 user_id：确保测试的是同一个用户的状态
3. 检查时间：时间衰减基于 LocalDateTime，确保系统时间正确


