# TODO

- [x] bytelogs-common 包含 Spring MVC 组件（GlobalExceptionHandler），导致 Gateway（WebFlux）无法引入，Gateway 中存在重复的 Redis key 前缀和手写 JSON 响应。主流做法是拆分为 bytelogs-common-core（纯 POJO，无框架依赖）和 bytelogs-common-web（MVC 组件），Gateway 只引 core。
- [ ] 搭建集成测试基础设施（H2 或 Testcontainers），补充 DAO 层集成测试，配合 CI 流水线在 PR 合并前自动触发。

## 文件存储

- [ ] 头像/封面同步删除：file-service 新增内部删除接口（不对外暴露），user-service 更新头像时、article-service 替换封面时调用，删除旧文件。
- [ ] 正文内图片定时清理：定期扫描 MinIO 中 `bitlog/article_content/` 路径下的文件，过滤掉仍被 article_version.content 引用的，删除孤儿文件。

## UploadScene 细化

- [x] 将 `UploadScene.article` 拆分为 `article_cover`（文章封面）和 `article_content`（正文内图片），便于后续分场景管理和清理。

## 安全加固

- [ ] 文件上传 MIME 类型白名单：`FileServiceImpl.upload()` 按 `UploadScene` 校验 `contentType`，avatar/article_cover/article_content 只允许 image/* 类型，拒绝其他 MIME 类型上传。
- [ ] 内部服务 Header 防伪造：Gateway 转发时注入共享密钥 Header（如 `X-Internal-Token`），各服务 filter 验证该 Header，防止绕过 Gateway 直接调用服务时伪造 `X-User-Id`/`X-User-Role`。
