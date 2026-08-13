package top.harrylei.bitlog.article.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.article.service.ArticleImageReferenceService;
import top.harrylei.bitlog.file.service.FileService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

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

    private final FileService fileService;
    private final ArticleImageReferenceService articleImageReferenceService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOrphanImages() {
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(48);
        List<String> trackedKeys = fileService.getOldUndeletedContentKeys(threshold);
        if (trackedKeys.isEmpty()) {
            return;
        }

        Set<String> referencedKeys = articleImageReferenceService.collectReferencedKeys();

        List<String> orphanKeys = trackedKeys.stream().filter(key -> !referencedKeys.contains(key)).toList();

        if (orphanKeys.isEmpty()) {
            return;
        }

        orphanKeys.forEach(fileService::delete);
        fileService.markDeleted(orphanKeys);
        log.info("清理孤儿内容图片 {} 张", orphanKeys.size());
    }
}
