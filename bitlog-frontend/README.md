# BitLog Frontend

BitLog Frontend is the Vue 3 and Vite SSG client for the self-hosted
[BitLog](https://bitlog.harrylei.top) blogging platform. It includes a shared Markdown editor
workspace and Markwright, a Tauri desktop writing client. The backend repository is available at
[hleidev/bitlog-backend](https://github.com/hleidev/bitlog-backend).

## 项目简介

BitLog Frontend 同时提供面向读者的静态生成博客、文章与用户管理后台，以及桌面端 Markdown
写作工具。Web 与桌面端共用编辑器包，避免编辑和展示效果因重复实现而产生差异。

## 技术栈

- Vue 3.5、Vue Router、Pinia
- Vite 7、vite-ssg、TypeScript 5.8
- Vditor、Mermaid、highlight.js
- Tauri 2、Rust
- ESLint、Prettier、Husky、lint-staged

## 工作区结构

```text
src/                 Web 应用：公开站点、管理后台、API 与状态管理
packages/editor/     Web 与桌面端共用的 Markdown 编辑器和内容样式
apps/desktop/        Markwright 桌面客户端（Tauri）
public/              静态资源
scripts/             sitemap、友链数据生成与 SSG 构建校验脚本
```

## 快速开始

需要 Node.js 22.22.2（项目已通过 Volta 固定版本）和 npm。

```bash
git clone https://github.com/hleidev/bitlog-frontend.git
cd bitlog-frontend
npm ci
npm run dev
```

开发服务器默认将 `/api` 代理到 `http://127.0.0.1:12301`。如需完整功能，请先启动
[BitLog Backend](https://github.com/hleidev/bitlog-backend)。

## 构建

```bash
npm run lint
npm run build
```

`npm run build` 是完整的生产构建：npm 会依次执行 `prebuild`、`build` 和 `postbuild`，生成
sitemap 与友链数据，完成类型检查和 SSG 构建，并校验文章是否进入静态产物。该流程需要生产
API 可达且能返回文章数据，否则产物校验会失败。

CI 使用 `npm run build:ci`，只执行类型检查和 SSG 编译，不触发依赖生产数据的 `prebuild` 与
`postbuild` 生命周期钩子。

## 桌面端

桌面端位于 `apps/desktop/`，构建前需安装 Rust 以及当前平台对应的
[Tauri prerequisites](https://v2.tauri.app/start/prerequisites/)。

```bash
npm run tauri --workspace apps/desktop -- dev
npm run tauri --workspace apps/desktop -- build
```

## 代码规范

项目使用 ESLint 和 Prettier，并通过 Husky 与 lint-staged 检查暂存文件。提交变更前请运行：

```bash
npm run lint
npm run build
```

## License

[MIT](LICENSE)
