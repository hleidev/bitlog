package top.harrylei.bitlog.article.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 更新草稿时携带编辑基准，拒绝覆盖其他编辑页已保存的版本。
 *
 * @author Harry
 * @since 2026-09-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ArticleUpdateParam extends ArticleSaveParam {

    @NotNull(message = "编辑基准版本不能为空")
    @Schema(description = "编辑器加载或上次保存得到的版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long expectedVersionId;
}
