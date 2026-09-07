package top.harrylei.bitlog.notification.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.SortField;
import top.harrylei.bitlog.notification.model.enums.NotificationSortEnum;

/**
 * 我的通知列表查询参数
 *
 * @author Harry
 * @since 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "我的通知列表查询参数")
public class NotificationPageParam extends BasePage {

    @Override
    protected SortField resolveSortField() {
        return NotificationSortEnum.CREATE_TIME;
    }
}
