# TODO

- [ ] bytelogs-common 包含 Spring MVC 组件（GlobalExceptionHandler），导致 Gateway（WebFlux）无法引入，Gateway 中存在重复的 Redis key 前缀和手写 JSON 响应。主流做法是拆分为 bytelogs-common-core（纯 POJO，无框架依赖）和 bytelogs-common-web（MVC 组件），Gateway 只引 core。
