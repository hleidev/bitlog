# 贡献指南

感谢你关注 BitLog。提交改动前，请先确认没有内容相同的 Issue 或 Pull Request。

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
  apps/desktop/          桌面写作客户端（Tauri），自用为主
  public/                静态资源
  scripts/               sitemap、友链数据生成与 SSG 构建校验脚本
```

模块边界和分层约定见 [架构说明](docs/architecture.md)。

## 开发流程

1. `main` 是发布分支，`dev` 是日常开发分支。
2. 功能分支从 `dev` 创建，并通过 Pull Request 合并回 `dev`。
3. 提交信息遵循 Conventional Commits，常用类型包括 `feat`、`fix`、`docs`、`refactor`、
   `test`、`chore`、`build`、`ci`、`style` 和 `perf`，通常带作用域：`feat(article): ...`。
4. 本仓库同时包含前后端，提交尽量按目录拆分；确需同时改动两侧时，在提交信息中说明关联。

## 本地运行

环境准备和启动命令见 [README 的快速开始](README.md#快速开始)。

后端启动脚本会先执行 `mvn -q package -DskipTests` 再运行生成的可执行 JAR。Spring Boot 按进程
工作目录读取 `.env`，启动脚本内部会切到 `bitlog-backend/` 再运行，所以从仓库根调用没问题；手动
运行 JAR 时必须在 `bitlog-backend/` 下执行。

桌面端需要先装 Rust 和对应平台的 [Tauri prerequisites](https://v2.tauri.app/start/prerequisites/)：

```bash
./start.sh desktop                                                    # 构建并启动
cd bitlog-frontend && npm run tauri --workspace apps/desktop -- dev    # 开发模式
```

## 提交前检查

只跑改动涉及的那一侧即可。

### 后端

```bash
cd bitlog-backend
mvn verify
```

集成测试使用 Testcontainers，需要 Docker。单元测试以 `*Test` 命名，集成测试以 `*IT` 命名，放在
对应模块的 `src/test/java` 下。可以按测试类单独运行：

```bash
mvn test -pl bitlog-server -Dtest=ArticlePaginationIT
```

### 前端

```bash
cd bitlog-frontend
npm ci
npm run lint
npm run build:ci
```

前端目前没有自动化测试套件，靠 ESLint、类型检查和构建校验兜底。

`npm run build:ci` 只做类型检查和 SSG 编译，是外部贡献者应该用的命令。完整的 `npm run build` 会
触发 `prebuild` 和 `postbuild` 钩子，其中包含依赖生产 API 数据的 sitemap 生成与产物校验，在没有
生产数据访问权限的环境下必然失败。CI 用的也是 `build:ci`。

## 代码约定

代码格式遵循根目录 `.editorconfig`（统一两空格，`*.java` 四空格），前端另有 `.prettierrc`。提交时
Husky 与 lint-staged 会检查暂存的前端文件。

代码注释以中文为主，应解释设计原因或容易误解的行为，而不是重复代码本身。

后端保持 `Controller -> Service -> DAO/Mapper` 分层，使用构造器注入和 MapStruct 转换器，遵循
`*Param`、`*VO`、`*DTO`、`*DO` 的命名后缀。跨模块调用必须通过目标模块的 `port` API，事务边界放在
Service 层，复杂查询放在 MyBatis XML Mapper 中。

前端 Vue 组件用 PascalCase 命名，组合式函数和 Pinia store 用 `useXxx`，API 访问统一放在 `src/api/`。

## CI

工作流按目录过滤，只有改动到对应目录才会触发：改 `bitlog-backend/` 跑后端的 `mvn -B verify`，
改 `bitlog-frontend/` 跑前端的 lint 和 `build:ci`。只改 `.github/` 下的文件两侧都不触发。

## 数据库变更

结构变更一律通过 Flyway 迁移文件完成，不要手改线上表。迁移文件放在
`bitlog-server/src/main/resources/db/migration`，按 `V{n}__{描述}.sql` 命名，合并后不再修改已有
文件。破坏性变更需要在 PR 里说明兼容性影响。

## 依赖升级

项目依赖由维护者定期手动检查和升级，不使用自动依赖更新机器人。依赖升级应单独提交，并在合并
前运行测试、构建并验证受影响的模块或功能。

Node 版本在三处声明：根 `mise.toml`、`bitlog-frontend/package.json` 的 `volta` 字段、以及 CI 的
`node-version`。升级时三处要同步。
