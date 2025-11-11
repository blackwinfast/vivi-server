# Chat API 文档

## 概述

这是一个智能聊天后端 API 服务器，提供情绪分析和动态 AI 模型选择功能。

## API 端点

### POST /chat

处理用户聊天请求，进行情绪分析并返回 AI 生成的回复。

#### 请求格式

**Content-Type:** `application/json`

```json
{
  "user_id": "user_01",
  "input_text": "使用者在 App 中輸入的文字"
}
```

**字段说明：**
- `user_id` (String, 必需): 用户唯一标识符
- `input_text` (String, 必需): 用户输入的文本内容

#### 响应格式

```json
{
  "response_text": "這是 AI 模型生成的回應文字",
  "response_type": "CHAT_MESSAGE",
  "remaining_quota": 99
}
```

**字段说明：**
- `response_text` (String, 必需): AI 生成的回复文本
- `response_type` (String, 可选): 响应类型
  - `CHAT_MESSAGE`: 普通聊天消息（默认）
  - `EMERGENCY_RESOURCE`: 紧急资源（检测到极端负面情绪时）
- `remaining_quota` (Int, 可选): 用户剩余使用次数

#### 响应类型说明

1. **CHAT_MESSAGE**: 正常的 AI 聊天回复
2. **EMERGENCY_RESOURCE**: 当检测到极端负面内容（如自杀倾向）时，会直接返回求助信息，不调用 AI 模型

## 核心功能

### 1. 情绪分析

系统会对用户输入进行情绪分析，识别以下情绪类型：
- `SAD`: 悲伤
- `ANGRY`: 愤怒
- `HAPPY`: 快乐
- `NEUTRAL`: 中性
- `EMERGENCY`: 紧急情况（极端负面内容）

### 2. 动态模型选择

根据情绪分析结果和用户会员等级，系统会自动选择最合适的 AI 模型：

| 情绪 | 免费用户 | 高级会员 | VIP 会员 |
|------|---------|---------|---------|
| 紧急情况 | Claude 3 Opus | Claude 3 Opus | Claude 3 Opus |
| 悲伤 | GPT-4o | Claude 3 Sonnet | Claude 3 Opus |
| 愤怒 | GPT-4o | GPT-4o | Claude 3 Sonnet |
| 其他（快乐/中性） | GPT-3.5 Turbo | GPT-4o | Claude 3 Sonnet |

### 3. 用户配额管理

- **免费用户**: 初始配额 10 次
- **高级会员**: 初始配额 100 次
- **VIP 会员**: 初始配额 1000 次

每次成功调用 API 会消耗 1 次配额。

## 环境变量配置

为了使用真实的 AI 模型 API，需要设置以下环境变量：

```bash
# OpenRouter API Key (统一访问多个 AI 模型)
export OPENROUTER_API_KEY="your-openrouter-api-key"
```

**OpenRouter 说明**: 
- OpenRouter 是一个统一的 API 网关，可以访问多个 AI 模型（OpenAI、Anthropic 等）
- 获取 API Key: https://openrouter.ai/keys
- 支持的模型包括：
  - `openai/gpt-4o` - GPT-4o（高质量）
  - `openai/gpt-3.5-turbo` - GPT-3.5 Turbo（低成本）
  - `anthropic/claude-3-opus` - Claude 3 Opus（最高质量）
  - `anthropic/claude-3-sonnet` - Claude 3 Sonnet（高质量）

**注意**: 如果不设置 API Key，系统会使用备用回复机制，不会调用真实的 AI 模型。

## 运行服务器

```bash
# 构建项目
./gradlew build

# 运行服务器
./gradlew run

# 或构建可执行 JAR
./gradlew buildFatJar
java -jar build/libs/V1.0-server-0.0.1-all.jar
```

服务器默认运行在 `http://localhost:8080`

## 测试示例

使用 curl 测试 API：

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user_01",
    "input_text": "我今天心情不太好"
  }'
```

## 错误处理

- 如果 `user_id` 或 `input_text` 为空，返回错误消息
- 如果用户配额已用完，返回配额不足消息
- 如果 API 调用失败，返回备用回复
- 所有错误都会记录在服务器日志中

## 未来改进

1. 集成真实的情绪分析 API（如 Google NLP、IBM Watson）
2. 连接数据库存储用户信息和历史记录
3. 添加更多 AI 模型支持
4. 实现更精细的配额管理策略
5. 添加请求限流和安全性增强

