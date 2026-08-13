package top.harrylei.bitlog.article.service;

import java.util.Set;

/**
 * 文章正文图片引用查询服务
 *
 * @author Harry
 * @since 2026-08-13
 */
public interface ArticleImageReferenceService {

    /**
     * 流式扫描全部未删除文章的正文，收集其中引用的图片 Key
     *
     * @return 被正文引用的图片 Key 集合
     */
    Set<String> collectReferencedKeys();
}
