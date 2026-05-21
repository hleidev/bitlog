package top.harrylei.bitlog.article.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.service.FileService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章内容图片孤儿清理定时任务
 *
 * @author Harry
 * @since 2026-05-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupTask {

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[.*?]\\(([^\\s)]+)");
    private static final String CONTENT_KEY_PREFIX = "bitlog/" + UploadScene.article_content.name() + "/";

    private final FileService fileService;
    private final ArticleVersionDAO articleVersionDAO;
    private final ArticleDAO articleDAO;
    private final FileUrlHelper fileUrlHelper;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOrphanImages() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        List<String> trackedKeys = fileService.getOldUndeletedContentKeys(threshold);
        if (trackedKeys.isEmpty()) {
            return;
        }

        Set<String> referencedKeys = new HashSet<>();
        List<String> allContent = articleVersionDAO.listAllContentFromActiveArticles();
        for (String content : allContent) {
            extractImageKeys(content, referencedKeys);
        }
        referencedKeys.addAll(articleDAO.listAllActiveCoverKeys());

        List<String> orphanKeys = trackedKeys.stream().filter(key -> !referencedKeys.contains(key)).toList();

        if (orphanKeys.isEmpty()) {
            return;
        }

        orphanKeys.forEach(fileService::delete);
        fileService.markDeleted(orphanKeys);
        log.info("清理孤儿内容图片 {} 张", orphanKeys.size());
    }

    private void extractImageKeys(String content, Set<String> keys) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        Matcher matcher = ImageCleanupTask.MD_IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = fileUrlHelper.extractKey(matcher.group(1));
            if (StringUtils.hasText(key) && key.startsWith(CONTENT_KEY_PREFIX)) {
                keys.add(key);
            }
        }
    }
}
