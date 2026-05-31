package top.harrylei.bitlog.article.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.ai.model.AiFeature;
import top.harrylei.bitlog.ai.port.AiModelRouter;
import top.harrylei.bitlog.api.model.article.vo.AiArticleMetadataVO;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.service.ArticleAiService;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final int SUGGESTED_TAGS_MAX = 2;

    private static final String METADATA_SYSTEM_PROMPT = "你是一个博客写作助手，擅长根据文章内容生成摘要并推荐合适的分类和标签。";

    private static final String METADATA_USER_PROMPT_TEMPLATE = """
        请根据以下文章内容完成三项任务，以 JSON 格式输出结果。

        现有分类（从以下 ID 中选择，格式为 id:名称）：
        %s

        现有标签（从以下 ID 中选择，格式为 id:名称）：
        %s

        文章标题：%s
        文章内容：
        %s

        输出 JSON 结构如下：
        {
          "summary": "（80-100汉字的文章摘要，纯文本）",
          "categoryId": 1,
          "tagIds": [1, 2, 3],
          "suggestedTags": []
        }

        规则：
        - summary：严格控制在 80 至 100 个汉字，纯文本，无 Markdown
        - categoryId：从现有分类中选最合适的 ID；没有合适的填 null
        - tagIds：从现有标签中选 3-5 个最相关的 ID；可以少于 3 个
        - suggestedTags：现有标签无法覆盖时才建议新标签名称，最多 2 个，通常为空数组
        """;

    private final ArticleDAO articleDAO;
    private final ArticleVersionDAO articleVersionDAO;
    private final CategoryDAO categoryDAO;
    private final TagDAO tagDAO;
    private final AiModelRouter aiModelRouter;
    private final ObjectMapper objectMapper;

    @Override
    public AiArticleMetadataVO generateMetadata(Long userId, Long articleId) {
        ArticleDO article = getArticleById(articleId);
        checkOwner(article, userId);

        ArticleVersionDO version = getLatestVersion(article);

        List<CategoryDO> categories = categoryDAO.listAll(null);
        List<TagVO> tags = tagDAO.listAll(null);

        String categoryOptions =
            categories.stream().map(c -> c.getId() + ":" + c.getName()).collect(Collectors.joining("\n"));
        String tagOptions = tags.stream().map(t -> t.getId() + ":" + t.getName()).collect(Collectors.joining("\n"));

        String userMessage = METADATA_USER_PROMPT_TEMPLATE.formatted(categoryOptions, tagOptions, version.getTitle(),
            version.getContent());

        String raw = aiModelRouter.chat(AiFeature.ARTICLE_SUGGESTIONS, METADATA_SYSTEM_PROMPT, userMessage);
        log.debug("AI 元数据原始响应 articleId={} raw={}", articleId, raw);

        return parseMetadata(articleId, raw, categories, tags);
    }

    private AiArticleMetadataVO parseMetadata(Long articleId, String raw, List<CategoryDO> categories,
        List<TagVO> tags) {
        AiRawMetadata parsed;
        try {
            parsed = objectMapper.readValue(raw, AiRawMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("AI 元数据 JSON 解析失败: {}", e.getMessage());
            throw ResultCode.AI_SERVICE_ERROR.toException("AI 返回格式异常，请重试");
        }

        Map<Long, CategoryDO> categoryMap = categories.stream().collect(Collectors.toMap(CategoryDO::getId, c -> c));
        Map<Long, TagVO> tagMap = tags.stream().collect(Collectors.toMap(TagVO::getId, t -> t));

        String summary = parsed.summary != null ? parsed.summary : "";

        CategoryVO category = null;
        if (parsed.categoryId != null && categoryMap.containsKey(parsed.categoryId)) {
            CategoryDO c = categoryMap.get(parsed.categoryId);
            category = new CategoryVO().setId(c.getId()).setName(c.getName());
        }

        List<TagVO> matchedTags = new ArrayList<>();
        if (parsed.tagIds != null) {
            for (Long tagId : parsed.tagIds) {
                if (tagMap.containsKey(tagId)) {
                    TagVO t = tagMap.get(tagId);
                    matchedTags.add(new TagVO().setId(t.getId()).setName(t.getName()));
                }
            }
        }

        List<String> suggestedTags =
            parsed.suggestedTags != null ? parsed.suggestedTags.stream().limit(SUGGESTED_TAGS_MAX).toList() : List.of();

        log.info("AI 元数据解析完成 articleId={} 摘要长度={} 分类={} 已有标签={} 新标签={}", articleId, summary.length(),
            category != null ? category.getName() : "null", matchedTags.size(), suggestedTags.size());

        return new AiArticleMetadataVO().setSummary(summary).setCategory(category).setTags(matchedTags)
            .setSuggestedTags(suggestedTags);
    }

    private ArticleDO getArticleById(Long articleId) {
        ArticleDO article = articleDAO.getByIdAndNotDeleted(articleId);
        if (article == null) {
            throw ResultCode.ARTICLE_NOT_EXISTS.toException();
        }
        return article;
    }

    private ArticleVersionDO getLatestVersion(ArticleDO article) {
        if (article.getLatestVersionId() == null) {
            throw ResultCode.ARTICLE_VERSION_NOT_EXISTS.toException();
        }
        ArticleVersionDO version = articleVersionDAO.getVersionById(article.getLatestVersionId());
        if (version == null) {
            throw ResultCode.ARTICLE_VERSION_NOT_EXISTS.toException();
        }
        return version;
    }

    private void checkOwner(ArticleDO article, Long userId) {
        if (!article.getUserId().equals(userId)) {
            ResultCode.ARTICLE_NO_PERMISSION.throwException();
        }
    }

    @Data
    @NoArgsConstructor
    private static class AiRawMetadata {
        private String summary;
        private Long categoryId;
        private List<Long> tagIds;
        private List<String> suggestedTags;
    }
}
