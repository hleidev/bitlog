# BitLog

自托管的个人博客系统。后端是 Java 21 + Spring Boot 的模块化单体，前端是 Vue 3 + Vite SSG，
公开站点、内容管理后台和写作端在同一个仓库里。

**线上站点：[bitlog.harrylei.top](https://bitlog.harrylei.top)**，这个仓库的代码就跑在上面。

[![Backend CI](https://github.com/hleidev/bitlog/actions/workflows/ci-backend.yml/badge.svg)](https://github.com/hleidev/bitlog/actions/workflows/ci-backend.yml)
[![Frontend CI](https://github.com/hleidev/bitlog/actions/workflows/ci-frontend.yml/badge.svg)](https://github.com/hleidev/bitlog/actions/workflows/ci-frontend.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 它是什么

一套完整的个人博客实现，不是脚手架也不是教程项目：文章的写作、版本、发布、检索、评论、友链
和后台管理都已经在线上跑着。

做它的出发点是自己用得顺手，同时把一些工程上的取舍认真做一遍：模块化单体的边界怎么划、
Web 与桌面端如何共用一套编辑器、博客这种读多写少的站点为什么走静态生成。这些决定和它们的
代价写在 [架构说明](docs/architecture.md) 里，比技术栈列表更能说明这个项目在做什么。

## 功能

- **文章**：Markdown 写作，草稿与发布分离，历史版本可回溯，分类与标签
- **阅读体验**：公开页面全部静态生成，构建期校验文章已进入产物，代码高亮与 Mermaid 图表
- **互动**：评论与回复，带频率限制；友链申请与审核
- **账号**：邮箱注册登录、Google OAuth，JWT 双令牌，管理员与普通用户分权
- **后台**：文章、用户、评论、友链的管理界面
- **AI 辅助**：接入大模型做写作辅助，按能力而非按模型配置，换供应商只改一行映射

桌面端 Markwright（Tauri）也在仓库里，它是为自己写作准备的，没有按对外分发来设计。

## 快速开始

环境要求：JDK 21、Maven 3.9+、Node.js 22.22.2，以及本机可用的 PostgreSQL 16+（库名和角色默认
都是 `bitlog`）、Redis 7+ 和 MinIO。这三项服务需要自行安装并启动。

```bash
cp bitlog-backend/.env.example bitlog-backend/.env  # 填入各项密钥
(cd bitlog-frontend && npm ci)
./start.sh                                          # 前后端一起起，Ctrl-C 一并停止
```

后端监听 `12301`，前端开发服务器监听 `5173` 并把 `/api` 代理到后端。首次启动时 Flyway 会自动
建表。也可以只起一侧：`./start.sh backend`、`./start.sh frontend`。

环境变量的完整说明在 [`bitlog-backend/.env.example`](bitlog-backend/.env.example) 里。目前所有
变量都没有默认值，缺任何一项启动都会失败并指明缺哪一项，这意味着即使只想本地看看，也要先准备
好 AI、邮件和 OAuth 三项的密钥。

### 把自己设成管理员

注册接口创建的都是普通用户（`user_role = 0`），写文章和进后台都需要管理员权限。

注册要走邮箱验证码，验证码由 Resend 发送，所以 `MAIL_API_KEY` 得是有效的、邮箱也要能真实收信。
注册完之后把自己升成管理员：

```sql
UPDATE user_account SET user_role = 1 WHERE email = lower('你注册用的邮箱');
```

邮箱统一按小写存储，所以套一层 `lower()`。角色写在 JWT 里，改完要重新登录一次才会生效。

## 架构

后端是模块化单体：`user`、`auth`、`article`、`comment`、`file`、`link`、`ai` 各自维护数据和
业务边界，跨模块调用走目标模块暴露的 `port` 接口，而不是直接注入对方的 Service。这条约定还有
存量没收口，架构说明里写明了现状。
前端的 Web 站点和桌面端共用 `packages/editor`，让编辑时和阅读时的渲染结果出自同一份实现。

模块依赖关系、这些边界解决了什么问题、以及它们的代价，见 [docs/architecture.md](docs/architecture.md)。

## 技术栈

<details>
<summary>展开</summary>

**后端**：Java 21、Spring Boot 3.5、MyBatis-Plus、PostgreSQL、Flyway、Redis、MinIO / AWS SDK v2、
Spring Security、JWT（jjwt）、OAuth 2.0、MapStruct、Lombok、JUnit 5、Mockito、Testcontainers、
JaCoCo

**前端**：Vue 3.5、Vue Router、Pinia、Vite 7、vite-ssg、TypeScript 5.8、Vditor、Mermaid、
highlight.js、Tauri 2、Rust、ESLint、Prettier、Husky、lint-staged

</details>


## 开发与贡献

欢迎 Issue 和 Pull Request。仓库结构、构建测试命令、代码约定和提交规范都在
[CONTRIBUTING.md](CONTRIBUTING.md) 里，安全问题的报告方式见 [SECURITY.md](SECURITY.md)。

## 许可

[MIT](LICENSE)
