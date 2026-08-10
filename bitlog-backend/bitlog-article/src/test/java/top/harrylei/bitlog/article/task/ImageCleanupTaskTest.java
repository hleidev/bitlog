package top.harrylei.bitlog.article.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.file.service.FileService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 文章正文图片孤儿清理定时任务单元测试
 *
 * @author Harry
 * @since 2026-05-20
 */
@ExtendWith(MockitoExtension.class)
class ImageCleanupTaskTest {

    private static final String PUBLIC_URL = "http://localhost:8001/files";
    private static final String CONTENT_PREFIX = "article/";

    @Mock
    private FileService fileService;

    @Mock
    private ArticleVersionDAO articleVersionDAO;

    @Mock
    private FileUrlHelper fileUrlHelper;

    @InjectMocks
    private ImageCleanupTask imageCleanupTask;

    /** Returns an answer that strips PUBLIC_URL prefix, mirroring the real FileUrlHelper. */
    private void stubExtractKey() {
        when(fileUrlHelper.extractKey(anyString())).thenAnswer(inv -> {
            String urlOrKey = inv.getArgument(0);
            String prefix = PUBLIC_URL + "/";
            if (urlOrKey != null && urlOrKey.startsWith(prefix)) {
                return urlOrKey.substring(prefix.length());
            }
            return urlOrKey;
        });
    }

    // -----------------------------------------------------------------------
    // Happy path: no tracked keys → early return, nothing is deleted
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_noTrackedKeys_returnsEarlyWithNoDeletes() {
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of());

        imageCleanupTask.cleanOrphanImages();

        verify(articleVersionDAO, never()).forEachActiveContent(any());
        verify(fileService, never()).delete(anyString());
        verify(fileService, never()).markDeleted(anyCollection());
    }

    // -----------------------------------------------------------------------
    // All tracked keys are referenced in article content → no orphans
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_allTrackedKeysReferenced_noOrphansDeleted() {
        String key = CONTENT_PREFIX + "1/2026/05/image1.png";
        String imageUrl = PUBLIC_URL + "/" + key;

        stubExtractKey();
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(key));
        stubContents(List.of("![image](" + imageUrl + ")"));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, never()).delete(anyString());
        verify(fileService, never()).markDeleted(anyCollection());
    }

    // -----------------------------------------------------------------------
    // Some tracked keys are not referenced → those are deleted and marked
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_unreferencedKeys_areDeletedAndMarked() {
        String referencedKey = CONTENT_PREFIX + "1/2026/05/referenced.png";
        String orphanKey = CONTENT_PREFIX + "1/2026/05/orphan.png";
        String referencedUrl = PUBLIC_URL + "/" + referencedKey;

        stubExtractKey();
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class)))
            .thenReturn(List.of(referencedKey, orphanKey));
        stubContents(List.of("![image](" + referencedUrl + ")"));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(1)).delete(orphanKey);
        verify(fileService, never()).delete(referencedKey);
        verify(fileService, times(1)).markDeleted(argThat(col -> col.size() == 1 && col.contains(orphanKey)));
    }

    // -----------------------------------------------------------------------
    // Markdown content with multiple images → all referenced keys extracted
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_multipleImagesInContent_allReferencedKeysExtracted() {
        String key1 = CONTENT_PREFIX + "1/2026/05/img1.png";
        String key2 = CONTENT_PREFIX + "1/2026/05/img2.jpg";
        String url1 = PUBLIC_URL + "/" + key1;
        String url2 = PUBLIC_URL + "/" + key2;
        String content = "intro\n\n![first image](" + url1 + ")\n\ntext\n\n![second](" + url2 + ")";

        stubExtractKey();
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(key1, key2));
        stubContents(List.of(content));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, never()).delete(anyString());
        verify(fileService, never()).markDeleted(anyCollection());
    }

    // -----------------------------------------------------------------------
    // Keys not matching the content prefix are ignored (external images)
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_externalImageUrlsIgnored_notTreatedAsReferences() {
        String orphanKey = CONTENT_PREFIX + "1/2026/05/orphan.png";
        String externalUrl = "https://external.example.com/photo.jpg";

        stubExtractKey();
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(orphanKey));
        stubContents(List.of("![external](" + externalUrl + ")"));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(1)).delete(orphanKey);
        verify(fileService, times(1)).markDeleted(argThat(col -> col.size() == 1 && col.contains(orphanKey)));
    }

    // -----------------------------------------------------------------------
    // Empty/null article content entries are handled without NullPointerException
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_emptyOrNullContentEntries_handledGracefully() {
        String orphanKey = CONTENT_PREFIX + "1/2026/05/orphan.png";

        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(orphanKey));
        stubContents(List.of("", "   "));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(1)).delete(orphanKey);
        verify(fileService, times(1)).markDeleted(argThat(col -> col.size() == 1 && col.contains(orphanKey)));
    }

    // -----------------------------------------------------------------------
    // All tracked keys are orphans → all deleted and marked
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_allTrackedKeysAreOrphans_allDeletedAndMarked() {
        String key1 = CONTENT_PREFIX + "1/2026/05/orphan1.png";
        String key2 = CONTENT_PREFIX + "1/2026/05/orphan2.jpg";

        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(key1, key2));
        stubContents(List.of());

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(1)).delete(key1);
        verify(fileService, times(1)).delete(key2);
        verify(fileService, times(1))
            .markDeleted(argThat(col -> col.size() == 2 && col.contains(key1) && col.contains(key2)));
    }

    // -----------------------------------------------------------------------
    // Image referenced in multiple articles is still not treated as orphan
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_keyReferencedInMultipleArticles_notDeleted() {
        String sharedKey = CONTENT_PREFIX + "1/2026/05/shared.png";
        String sharedUrl = PUBLIC_URL + "/" + sharedKey;

        stubExtractKey();
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(sharedKey));
        stubContents(List.of("![img](" + sharedUrl + ")", "![img](" + sharedUrl + ")"));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, never()).delete(anyString());
        verify(fileService, never()).markDeleted(anyCollection());
    }

    // -----------------------------------------------------------------------
    // markDeleted is called exactly once (batch call, not per-key)
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_markDeletedCalledOnceForAllOrphans() {
        String key1 = CONTENT_PREFIX + "1/2026/05/o1.png";
        String key2 = CONTENT_PREFIX + "1/2026/05/o2.png";

        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(key1, key2));
        stubContents(List.of());

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(2)).delete(anyString());
        verify(fileService, times(1)).markDeleted(anyCollection());
    }

    /** DAO 改为逐行回调，桩把给定正文依次喂给消费者，替代原先的返回列表 */
    @SuppressWarnings("unchecked")
    private void stubContents(List<String> contents) {
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(0);
            contents.forEach(consumer);
            return null;
        }).when(articleVersionDAO).forEachActiveContent(any());
    }
}
