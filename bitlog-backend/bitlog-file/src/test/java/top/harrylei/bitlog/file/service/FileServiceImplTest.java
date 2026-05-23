package top.harrylei.bitlog.file.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.file.config.StorageProperties;
import top.harrylei.bitlog.file.repository.dao.ImageRecordDAO;
import top.harrylei.bitlog.file.repository.entity.ImageRecordDO;
import top.harrylei.bitlog.file.service.impl.FileServiceImpl;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileServiceImpl 单元测试
 *
 * @author Harry
 * @since 2026-05-20
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageProperties props;

    @Mock
    private FileUrlHelper fileUrlHelper;

    @Mock
    private ImageRecordDAO imageRecordDAO;

    @InjectMocks
    private FileServiceImpl fileService;

    // -----------------------------------------------------------------------
    // recordUpload — saves a NOT_DELETED ImageRecordDO with correct fields
    // -----------------------------------------------------------------------

    @Test
    void recordUpload_savesImageRecordWithCorrectFields() {
        Long userId = 42L;
        String fileKey = "article/42/2026/05/abc.png";
        when(imageRecordDAO.save(any(ImageRecordDO.class))).thenReturn(true);

        fileService.recordUpload(userId, fileKey);

        ArgumentCaptor<ImageRecordDO> captor = ArgumentCaptor.forClass(ImageRecordDO.class);
        verify(imageRecordDAO, times(1)).save(captor.capture());
        ImageRecordDO saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getFileKey()).isEqualTo(fileKey);
        assertThat(saved.getDeleted()).isEqualTo(DeleteStatusEnum.NOT_DELETED);
    }

    // -----------------------------------------------------------------------
    // getOldUndeletedContentKeys — delegates to DAO and returns its result
    // -----------------------------------------------------------------------

    @Test
    void getOldUndeletedContentKeys_delegatesToDAOAndReturnsResult() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        List<String> expected = List.of("article/1/2026/05/img.png");
        when(imageRecordDAO.getKeysOlderThan(threshold)).thenReturn(expected);

        List<String> result = fileService.getOldUndeletedContentKeys(threshold);

        assertThat(result).isEqualTo(expected);
        verify(imageRecordDAO, times(1)).getKeysOlderThan(threshold);
    }

    @Test
    void getOldUndeletedContentKeys_whenNoRecords_returnsEmptyList() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        when(imageRecordDAO.getKeysOlderThan(threshold)).thenReturn(List.of());

        List<String> result = fileService.getOldUndeletedContentKeys(threshold);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // markDeleted — delegates collection to DAO
    // -----------------------------------------------------------------------

    @Test
    void markDeleted_delegatesCollectionToDAO() {
        List<String> keys = List.of("article/1/2026/05/a.png", "article/1/2026/05/b.png");

        fileService.markDeleted(keys);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(imageRecordDAO, times(1)).markDeleted(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrderElementsOf(keys);
    }

    @Test
    void markDeleted_withEmptyCollection_stillCallsDAO() {
        fileService.markDeleted(List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(imageRecordDAO, times(1)).markDeleted(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }
}
