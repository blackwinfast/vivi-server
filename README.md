# V1.0-server

智能聊天后端 API 服务器，提供情绪分析和动态 AI 模型选择功能。

This project was created using the [Ktor Project Generator](https://start.ktor.io).

## 主要功能

- ✅ **情绪分析**: 自动分析用户输入的情绪（悲伤、愤怒、快乐、中性、紧急情况）
- ✅ **动态模型选择**: 根据情绪和用户会员等级自动选择最合适的 AI 模型
- ✅ **用户配额管理**: 支持免费、高级、VIP 三种会员等级，每种等级有不同的使用配额
- ✅ **紧急情况处理**: 检测到极端负面内容时，直接返回求助信息，不调用 AI 模型
- ✅ **多模型支持**: 支持 OpenAI GPT-4o、GPT-3.5 Turbo 和 Anthropic Claude 3

## API 端点

### POST /chat

处理用户聊天请求。详细文档请参考 [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

**请求示例:**
```json
{
  "user_id": "user_01",
  "input_text": "我今天心情不太好"
}
```

**响应示例:**
```json
{
  "response_text": "我理解你现在的心情...",
  "response_type": "CHAT_MESSAGE",
  "remaining_quota": 99
}
```

## 环境变量配置

在使用真实的 AI 模型 API 之前，需要设置以下环境变量：

```bash
# OpenRouter API Key (统一访问多个 AI 模型)
export OPENROUTER_API_KEY="your-openrouter-api-key"
```

**OpenRouter 说明**: 
- OpenRouter 是一个统一的 API 网关，可以访问多个 AI 模型
- 获取 API Key: https://openrouter.ai/keys
- 支持的模型：GPT-4o, GPT-3.5 Turbo, Claude 3 Opus, Claude 3 Sonnet 等

**注意**: 如果不设置 API Key，系统会使用备用回复机制，不会调用真实的 AI 模型。

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need
  to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                                               | Description                                                                        |
| --------------------------------------------------------------------|------------------------------------------------------------------------------------ |
| [Routing](https://start.ktor.io/p/routing)                         | Provides a structured routing DSL                                                  |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [Call Logging](https://start.ktor.io/p/call-logging)               | Logs client requests                                                               |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                                    | Description                                                          |
| -----------------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

