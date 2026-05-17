# TODO

## 文件存储

- [ ] 头像/封面同步删除：用户更新头像或文章替换封面时，删除 MinIO 中的旧文件。
- [ ] 正文内图片定时清理：定期扫描 MinIO 中 `bitlog/article_content/` 路径下的孤儿文件（未被任何 article_version.content 引用的），定期清除。

## 安全加固

- [ ] 文件上传 MIME 类型白名单：`FileServiceImpl.upload()` 按 `UploadScene` 校验 `contentType`，avatar/article_cover/article_content 只允许 image/* 类型。

## 代码质量

- [ ] PageVO 工厂方法：`ArticleServiceImpl.toArticlePageVO` 中手动赋 7 个字段且重复两次，提取 `PageVO.from(IPage<?>, List<T>)` 静态工厂方法消除样板代码。
- [ ] FileUrlHelper 移到 converter 层：`buildDetailVO` 和 `toArticlePageVO` 中的 `fileUrlHelper.buildUrl()` 是展示层转换，应由 `ArticleConverter` 负责，消除 service 对 `FileUrlHelper` 的依赖。

## 搜索

- [ ] 文章正文全文检索：当前关键词搜索仅匹配标题和摘要，正文内容（`article_version.content`）未纳入。`LIKE` 无法走索引，正文搜索需引入 MySQL FULLTEXT 或 Elasticsearch。

## 评论功能

- [ ] 评论功能实现

## 传输对象

- [ ] 统一传输对象命名：入参 *Param，出参 *VO，模块间 *DTO，数据库实体 *DO；将现有 *Request 重命名为 *Param，*PageQuery 重命名为 *PageParam

## 数据库迁移

- [ ] 生产首次部署前，将 V1__init_user_tables.sql 和 V2__init_article_tables.sql 合并为单个初始化文件