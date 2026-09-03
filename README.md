# BitLog

BitLog is a self-hosted blogging platform. This repository contains both halves: a Java 21 and
Spring Boot API, and a Vue 3 client that ships the public site, the admin console and Markwright,
a Tauri desktop writing client. Live site: [bitlog.harrylei.top](https://bitlog.harrylei.top).

## 项目简介

BitLog 为博客公开站点、内容管理后台和桌面写作客户端提供完整实现。前后端此前是两个独立仓库，
现合并到同一仓库维护，各自的构建工具链保持不变，互不影响。

## 仓库结构

```text
bitlog-backend/     Java 21 + Spring Boot 3.5 API，模块化单体，PostgreSQL / Redis / MinIO
bitlog-frontend/    Vue 3 + Vite SSG 公开站与管理后台，含共用编辑器包和 Tauri 桌面端
```

两个子项目各自独立，技术栈、模块划分、构建与测试说明见各自目录下的 README：
[bitlog-backend/README.md](bitlog-backend/README.md)、[bitlog-frontend/README.md](bitlog-frontend/README.md)。

## 快速开始

后端需要 JDK 21、Maven 3.9+、PostgreSQL 16+、Redis 7+ 和 S3 兼容对象存储；前端需要
Node.js 22.22.2（已由 Volta 固定）。

```bash
# 后端，默认监听 12301
cd bitlog-backend
cp .env.example .env    # 按文件内说明填写各项，无默认值
./start.sh

# 前端，开发服务器将 /api 代理到 127.0.0.1:12301
cd bitlog-frontend
npm ci
./start.sh
```

## 开发约定

- 前端提交前由 husky 触发 lint-staged，对改动文件跑 ESLint 与 Prettier
- CI 按目录触发：改 `bitlog-backend/` 只跑后端构建，改 `bitlog-frontend/` 只跑前端构建，互不牵连

## 许可

MIT，见各子目录下的 LICENSE。
