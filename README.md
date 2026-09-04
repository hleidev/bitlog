# BitLog

BitLog is a self-hosted blogging platform. This repository contains both halves: a Java 21 and
Spring Boot API, and a Vue 3 client that ships the public site, the admin console and Markwright,
a Tauri desktop writing client. Live site: [bitlog.harrylei.top](https://bitlog.harrylei.top).

## 项目简介

BitLog 为博客公开站点、内容管理后台和桌面写作客户端提供完整实现。后端采用模块化单体架构：
各领域在同一应用内独立维护数据和业务边界，跨模块调用通过目标模块公开的 port 完成。前端 Web
与桌面端共用编辑器包，避免编辑和展示效果因重复实现而产生差异。

## 仓库结构

```text
bitlog-backend/          Java 21 + Spring Boot 3.5 API
  bitlog-common/         API 响应、分页、安全配置、上下文及公共基础设施
  bitlog-ai/             AI 模型配置与调用能力
  bitlog-user/           用户账号、资料及后台用户查询
  bitlog-auth/           登录注册、令牌、验证码、OAuth 身份和凭据管理
  bitlog-article/        文章、版本、分类和标签
  bitlog-file/           基于 S3 / MinIO 的文件上传与地址转换
  bitlog-comment/        评论与回复
  bitlog-link/           友链申请、审核与展示
  bitlog-server/         应用入口、运行配置和 Flyway 数据库迁移

bitlog-frontend/         Vue 3 + Vite SSG 客户端
  src/                   Web 应用：公开站点、管理后台、API 与状态管理
  packages/editor/       Web 与桌面端共用的 Markdown 编辑器和内容样式
  apps/desktop/          Markwright 桌面客户端（Tauri）
  public/                静态资源
  scripts/               sitemap、友链数据生成与 SSG 构建校验脚本
```

## 技术栈

**后端**：Java 21、Spring Boot 3.5.0、MyBatis-Plus 3.5.12、PostgreSQL、Flyway、Redis、
MinIO / AWS SDK v2、Spring Security、JWT（jjwt）、OAuth 2.0、springdoc-openapi、MapStruct、
Lombok、JUnit 5、Mockito、Testcontainers、JaCoCo

**前端**：Vue 3.5、Vue Router、Pinia、Vite 7、vite-ssg、TypeScript 5.8、Vditor、Mermaid、
highlight.js、Tauri 2、Rust、ESLint、Prettier、Husky、lint-staged

## 快速开始

环境要求：JDK 21、Maven 3.9+、PostgreSQL 16+（数据库名和角色默认均为 `bitlog`）、Redis 7+、
MinIO 或其他 S3 兼容对象存储、Node.js 22.22.2（根 `mise.toml` 与前端 `package.json` 的 volta 字段均已声明，CI 用的也是这个版本）。

首次运行需先准备后端环境变量并安装前端依赖：

```bash
cp bitlog-backend/.env.example bitlog-backend/.env
(cd bitlog-frontend && npm ci)
```

之后在仓库根目录一条命令同时启动前后端，Ctrl-C 一并停止：

```bash
./start.sh
```

也可以只启动其中一个：`./start.sh backend`、`./start.sh frontend`、`./start.sh desktop`。

### 后端

```bash
./start.sh backend
```

完整变量说明和申请地址见 [`bitlog-backend/.env.example`](bitlog-backend/.env.example)。Spring Boot
按进程工作目录读取 `.env`，启动脚本内部会切到 `bitlog-backend/` 再运行，因此从仓库根调用也没问题；
手动运行 JAR 时则必须在 `bitlog-backend/` 下执行。首次启动时 Flyway 会自动创建和升级数据库结构。
启动脚本会先执行 `mvn -q package -DskipTests`，再运行生成的可执行 JAR。开发环境默认监听 `12301` 端口。

### 前端

```bash
./start.sh frontend
```

启动脚本会检查依赖是否已安装，然后运行 Vite 开发服务器。开发服务器默认将 `/api` 代理到
`http://127.0.0.1:12301`，完整功能需先启动后端。

## 构建与测试

### 后端

```bash
cd bitlog-backend
mvn compile
mvn test
mvn verify
```

集成测试使用 Testcontainers，需要本机已启动 Docker。可按测试类运行：

```bash
mvn test -pl bitlog-server -Dtest=ArticlePaginationIT
```

### 前端

```bash
cd bitlog-frontend
npm run lint
npm run build
```

`npm run build` 是完整的生产构建：npm 会依次执行 `prebuild`、`build` 和 `postbuild`，生成
sitemap 与友链数据，完成类型检查和 SSG 构建，并校验文章是否进入静态产物。该流程需要生产 API
可达且能返回文章数据，否则产物校验会失败。

CI 使用 `npm run build:ci`，只执行类型检查和 SSG 编译，不触发依赖生产数据的 `prebuild` 与
`postbuild` 生命周期钩子。

## 桌面端

桌面端位于 `bitlog-frontend/apps/desktop/`，构建前需安装 Rust 以及当前平台对应的
[Tauri prerequisites](https://v2.tauri.app/start/prerequisites/)。

```bash
./start.sh desktop                                    # 构建并启动
cd bitlog-frontend && npm run tauri --workspace apps/desktop -- dev    # 开发模式
```

## API 文档

后端启动后可通过 `http://localhost:12301/v3/api-docs` 获取 OpenAPI JSON。

## 开发约定

后端保持 `Controller -> Service -> DAO/Mapper` 分层。跨模块调用必须通过目标模块的 `port` API，
复杂查询放在 MyBatis XML Mapper 中，事务边界放在 Service 层。

前端使用 ESLint 和 Prettier，提交时由 Husky 与 lint-staged 检查暂存文件。代码注释以中文为主，
应解释设计原因或容易误解的行为，而不是重复代码本身。

CI 按目录触发：改动 `bitlog-backend/` 只跑后端构建，改动 `bitlog-frontend/` 只跑前端构建。

贡献流程见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可

[MIT](LICENSE)
