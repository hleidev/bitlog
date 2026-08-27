# BitLog Backend

BitLog Backend is the Java 21 and Spring Boot 3.5 API for the self-hosted
[BitLog](https://bitlog.harrylei.top) blogging platform. It is organized as a modular monolith and
uses PostgreSQL, Redis and MinIO-compatible object storage. The frontend repository is available at
[hleidev/bitlog-frontend](https://github.com/hleidev/bitlog-frontend).

## 项目简介

BitLog Backend 为博客公开站点、内容管理后台和桌面写作客户端提供统一 API。项目采用模块化单体
架构：各领域在同一应用内独立维护数据和业务边界，跨模块调用通过目标模块公开的 port 完成。

## 技术栈

- Java 21、Spring Boot 3.5.0
- MyBatis-Plus 3.5.12、PostgreSQL、Flyway
- Redis、MinIO / AWS SDK v2
- Spring Security、JWT（jjwt）、OAuth 2.0
- springdoc-openapi、MapStruct、Lombok
- JUnit 5、Mockito、Testcontainers、JaCoCo

## 模块结构

| 模块 | 职责 |
| --- | --- |
| `bitlog-common` | API 响应、分页、安全配置、上下文及公共基础设施 |
| `bitlog-ai` | AI 模型配置与调用能力 |
| `bitlog-user` | 用户账号、资料及后台用户查询 |
| `bitlog-auth` | 登录注册、令牌、验证码、OAuth 身份和凭据管理 |
| `bitlog-article` | 文章、版本、分类和标签 |
| `bitlog-file` | 基于 S3 / MinIO 的文件上传与地址转换 |
| `bitlog-comment` | 评论与回复 |
| `bitlog-link` | 友链申请、审核与展示 |
| `bitlog-server` | 应用入口、运行配置和 Flyway 数据库迁移 |

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- PostgreSQL 16+（数据库名和角色默认均为 `bitlog`）
- Redis 7+
- MinIO 或其他 S3 兼容对象存储

复制本地环境变量示例并填写配置：

```bash
git clone https://github.com/hleidev/bitlog-backend.git
cd bitlog-backend
cp .env.example .env
```

完整变量说明和申请地址见 [`.env.example`](.env.example)。`.env` 的读取路径相对于进程工作
目录，因此启动命令必须在 `bitlog-backend/` 根目录执行。首次启动时 Flyway 会自动创建和升级
数据库结构。

```bash
./start.sh
```

启动脚本会先执行 `mvn -q package -DskipTests`，然后从仓库根目录运行生成的可执行 JAR，
确保 Spring Boot 能正确读取本地 `.env`。

开发环境默认监听 `12301` 端口。

## 构建与测试

```bash
mvn compile
mvn test
mvn verify
```

集成测试使用 Testcontainers，需要本机已启动 Docker。可按测试类运行：

```bash
mvn test -pl bitlog-server -Dtest=ArticlePaginationIT
```

## API 文档

应用启动后可通过 `http://localhost:12301/v3/api-docs` 获取 OpenAPI JSON。

## 开发约定

后端保持 `Controller -> Service -> DAO/Mapper` 分层。跨模块调用必须通过目标模块的 `port` API，
复杂查询放在 MyBatis XML Mapper 中，事务边界放在 Service 层。

## License

[MIT](LICENSE)
