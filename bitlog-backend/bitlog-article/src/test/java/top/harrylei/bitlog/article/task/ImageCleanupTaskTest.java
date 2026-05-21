package top.harrylei.bitlog.article.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.file.service.FileService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图片孤儿清理定时任务单元测试
 *
 * @author Harry
 * @since 2026-05-20
 */
@ExtendWith(MockitoExtension.class)
class ImageCleanupTaskTest {

    private static final String PUBLIC_URL = "http://localhost:8001/files";
    private static final String CONTENT_PREFIX = "bitlog/article_content/";

    @Mock
    private FileService fileService;

    @Mock
    private ArticleVersionDAO articleVersionDAO;

    @Mock
    private ArticleDAO articleDAO;

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

        verify(articleVersionDAO, never()).listAllContentFromActiveArticles();
        verify(articleDAO, never()).listAllActiveCoverKeys();
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
        when(articleVersionDAO.listAllContentFromActiveArticles())
            .thenReturn(List.of("<img src=\"" + imageUrl + "\" />"));
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

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
        when(articleVersionDAO.listAllContentFromActiveArticles())
            .thenReturn(List.of("<img src=\"" + referencedUrl + "\" />"));
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(1)).delete(orphanKey);
        verify(fileService, never()).delete(referencedKey);
        verify(fileService, times(1)).markDeleted(argThat(col -> col.size() == 1 && col.contains(orphanKey)));
    }

    // -----------------------------------------------------------------------
    // Cover keys are excluded from the orphan set
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_coverKeyExcludedFromOrphans_notDeleted() {
        String coverKey = CONTENT_PREFIX + "1/2026/05/cover.jpg";
        String orphanKey = CONTENT_PREFIX + "1/2026/05/orphan.png";

        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(coverKey, orphanKey));
        when(articleVersionDAO.listAllContentFromActiveArticles()).thenReturn(List.of());
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of(coverKey));

        imageCleanupTask.cleanOrphanImages();

        verify(fileService, times(1)).delete(orphanKey);
        verify(fileService, never()).delete(coverKey);
        verify(fileService, times(1)).markDeleted(argThat(col -> col.size() == 1 && col.contains(orphanKey)));
    }

    // -----------------------------------------------------------------------
    // HTML parsing: multiple img tags in one content body
    // -----------------------------------------------------------------------

    @Test
    void cleanOrphanImages_htmlWithMultipleImgTags_allReferencedKeysExtracted() {
        String key1 = CONTENT_PREFIX + "1/2026/05/img1.png";
        String key2 = CONTENT_PREFIX + "1/2026/05/img2.jpg";
        String url1 = PUBLIC_URL + "/" + key1;
        String url2 = PUBLIC_URL + "/" + key2;
        String html = "<p>text</p><img src=\"" + url1 + "\" /><img src=\"" + url2 + "\" alt=\"x\"/>";

        stubExtractKey();
        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(key1, key2));
        when(articleVersionDAO.listAllContentFromActiveArticles()).thenReturn(List.of(html));
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

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

        when(fileService.getOldUndeletedContentKeys(any(LocalDateTime.class))).thenReturn(List.of(orphanKey));
        // Override extractKey so that the external URL is returned as-is (no prefix match)
        when(fileUrlHelper.extractKey(externalUrl)).thenReturn(externalUrl);
        when(articleVersionDAO.listAllContentFromActiveArticles())
            .thenReturn(List.of("<img src=\"" + externalUrl + "\" />"));
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

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
        when(articleVersionDAO.listAllContentFromActiveArticles()).thenReturn(List.of("", "   "));
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

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
        when(articleVersionDAO.listAllContentFromActiveArticles()).thenReturn(List.of());
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

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
        when(articleVersionDAO.listAllContentFromActiveArticles())
            .thenReturn(List.of("<img src=\"" + sharedUrl + "\" />", "<img src=\"" + sharedUrl + "\" />"));
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

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
        when(articleVersionDAO.listAllContentFromActiveArticles()).thenReturn(List.of());
        when(articleDAO.listAllActiveCoverKeys()).thenReturn(List.of());

        imageCleanupTask.cleanOrphanImages();

        // delete is called once per key, markDeleted is called only once with the full list
        verify(fileService, times(2)).delete(anyString());
        verify(fileService, times(1)).markDeleted(anyCollection());
    }
}
