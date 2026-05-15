# TODO

## 文件存储

- [ ] 头像/封面同步删除：用户更新头像或文章替换封面时，删除 MinIO 中的旧文件。
- [ ] 正文内图片定时清理：定期扫描 MinIO 中 `bitlog/article_content/` 路径下的孤儿文件（未被任何 article_version.content 引用的），定期清除。

## 安全加固

- [ ] 文件上传 MIME 类型白名单：`FileServiceImpl.upload()` 按 `UploadScene` 校验 `contentType`，avatar/article_cover/article_content 只允许 image/* 类型。
