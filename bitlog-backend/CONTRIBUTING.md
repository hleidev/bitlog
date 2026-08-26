# 贡献指南

感谢你关注 BitLog Backend。提交改动前，请先确认没有内容相同的 Issue 或 Pull Request。

## 开发流程

1. `main` 是发布分支，`dev` 是日常开发分支。
2. 功能分支从 `dev` 创建，并通过 Pull Request 合并回 `dev`。
3. 提交信息遵循 Conventional Commits，常用类型包括 `feat`、`fix`、`docs`、`refactor`、
   `test`、`chore`、`build`、`ci`、`style` 和 `perf`。

## 提交前检查

```bash
mvn verify
```

集成测试使用 Testcontainers，需要 Docker。代码格式遵循 `.editorconfig`，代码注释以中文为主。
请保持 `Controller -> Service -> DAO/Mapper` 分层，跨模块调用通过目标模块的 `port` API 完成。

项目依赖由维护者定期手动检查和升级，不使用自动依赖更新机器人。依赖升级应单独提交，并在合并
前运行测试和验证受影响模块。
