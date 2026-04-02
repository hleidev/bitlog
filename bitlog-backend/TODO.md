# TODO

- [ ] bytelogs-common 包含 Spring MVC 组件（GlobalExceptionHandler），导致 Gateway（WebFlux）无法引入，Gateway 中存在重复的 Redis key 前缀和手写 JSON 响应。主流做法是拆分为 bytelogs-common-core（纯 POJO，无框架依赖）和 bytelogs-common-web（MVC 组件），Gateway 只引 core。
- [ ] 搭建集成测试基础设施（H2 或 Testcontainers），补充 DAO 层集成测试，配合 CI 流水线在 PR 合并前自动触发。
