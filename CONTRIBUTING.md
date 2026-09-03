# 贡献指南

感谢你关注 BitLog。提交改动前，请先确认没有内容相同的 Issue 或 Pull Request。

## 开发流程

1. `main` 是发布分支，`dev` 是日常开发分支。
2. 功能分支从 `dev` 创建，并通过 Pull Request 合并回 `dev`。
3. 提交信息遵循 Conventional Commits，常用类型包括 `feat`、`fix`、`docs`、`refactor`、
   `test`、`chore`、`build`、`ci`、`style` 和 `perf`。
4. 本仓库同时包含前后端，提交尽量按目录拆分；确需同时改动两侧时，在提交信息中说明关联。

## 提交前检查

只跑改动涉及的那一侧即可。

### 后端

```bash
cd bitlog-backend
mvn verify
```

集成测试使用 Testcontainers，需要 Docker。

### 前端

```bash
cd bitlog-frontend
npm ci
npm run lint
npm run build
```

CI 使用 `npm run build:ci`。虽然它与 `build` 的脚本主体相同，但脚本名不同，因此 npm 不会触发
依赖生产 API 数据的 `prebuild` 和 `postbuild` 生命周期钩子；本地提交前仍应运行完整的
`npm run build`。

## 代码约定

代码格式遵循根目录 `.editorconfig`，前端另有 `.prettierrc`。代码注释以中文为主，应解释设计
原因或容易误解的行为，而不是重复代码本身。

后端保持 `Controller -> Service -> DAO/Mapper` 分层，跨模块调用通过目标模块的 `port` API 完成。

## 依赖升级

项目依赖由维护者定期手动检查和升级，不使用自动依赖更新机器人。依赖升级应单独提交，并在合并
前运行测试、构建并验证受影响的模块或功能。
