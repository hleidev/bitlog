package top.harrylei.bitlog.article.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 草稿保存结果，供编辑器绑定本次保存的版本。
 *
 * @author Harry
 * @since 2026-09-11
 */
@Data
@Accessors(chain = true)
public class ArticleSaveVO {

    @Schema(description = "文章 ID")
    private Long id;

    @Schema(description = "本次保存的版本 ID")
    private Long versionId;
}
