package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.ai.model.AiFeature;
import top.harrylei.bitlog.ai.port.AiModelRouter;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;
import top.harrylei.bitlog.article.service.ArticleAiService;
import top.harrylei.bitlog.common.enums.ResultCode;

/**
 * 文章 AI 辅助服务实现
 *
 * @author Harry
 * @since 2026-05-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleAiServiceImpl implements ArticleAiService {

    private static final int CONTENT_MAX_CHARS = 3000;
    private static final int SUMMARY_MAX_CHARS = 512;

    private static final String SYSTEM_PROMPT = "你是一个博客写作助手，擅长根据文章内容生成精炼的摘要。";

    private static final String USER_PROMPT_TEMPLATE = """
        请根据以下文章内容生成一段摘要。
        要求：
        - 不超过 200 字
        - 纯文本，不包含任何 Markdown 符号
        - 使用与文章相同的语言（中文文章用中文，英文文章用英文）
        - 直接输出摘要内容，不要有任何前缀或说明文字

        文章标题：%s
        文章内容：
        %s
        """;

    private final ArticleDAO articleDAO;
    private final ArticleVersionDAO articleVersionDAO;
    private final AiModelRouter aiModelRouter;

    @Override
    public String generateSummary(Long userId, Long articleId) {
        ArticleDO article = getArticleById(articleId);
        checkOwner(article, userId);

        if (article.getLatestVersionId() == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }
        ArticleVersionDO version = articleVersionDAO.getVersionById(article.getLatestVersionId());
        if (version == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }

        String content = truncate(version.getContent());
        String userMessage = USER_PROMPT_TEMPLATE.formatted(version.getTitle(), content);
        String summary = aiModelRouter.chat(AiFeature.ARTICLE_SUMMARY, SYSTEM_PROMPT, userMessage);

        if (summary.length() > SUMMARY_MAX_CHARS) {
            summary = summary.substring(0, SUMMARY_MAX_CHARS);
        }

        log.info("AI 摘要生成完成 articleId={} length={}", articleId, summary.length());
        return summary;
    }

    private ArticleDO getArticleById(Long articleId) {
        ArticleDO article = articleDAO.getByIdAndNotDeleted(articleId);
        if (article == null) {
            throw ResultCode.ARTICLE_NOT_EXISTS.toException();
        }
        return article;
    }

    private void checkOwner(ArticleDO article, Long userId) {
        if (!article.getUserId().equals(userId)) {
            ResultCode.ARTICLE_NO_PERMISSION.throwException();
        }
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > CONTENT_MAX_CHARS
            ? content.substring(0, ArticleAiServiceImpl.CONTENT_MAX_CHARS) + "..." : content;
    }
}
