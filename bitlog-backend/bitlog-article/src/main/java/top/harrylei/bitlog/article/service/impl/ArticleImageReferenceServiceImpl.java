package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.service.ArticleImageReferenceService;
import top.harrylei.bitlog.file.util.FileUrlHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章正文图片引用查询服务实现
 *
 * @author Harry
 * @since 2026-08-13
 */
@Service
@RequiredArgsConstructor
public class ArticleImageReferenceServiceImpl implements ArticleImageReferenceService {

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[.*?]\\(([^\\s)]+)");

    private final ArticleVersionDAO articleVersionDAO;
    private final FileUrlHelper fileUrlHelper;

    /**
     * 事务不可省：连接处于自动提交状态时驱动会把结果集整个取回内存，流式读取失去意义
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> collectReferencedKeys() {
        Set<String> keys = new HashSet<>();
        articleVersionDAO.forEachActiveContent(content -> extractImageKeys(content, keys));
        return keys;
    }

    private void extractImageKeys(String content, Set<String> keys) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        Matcher matcher = MD_IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = fileUrlHelper.extractKey(matcher.group(1));
            if (StringUtils.hasText(key) && !key.contains("://")) {
                keys.add(key);
            }
        }
    }
}
