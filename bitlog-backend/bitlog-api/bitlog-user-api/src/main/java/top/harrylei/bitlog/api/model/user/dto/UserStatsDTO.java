package top.harrylei.bitlog.api.model.user.dto;

import lombok.Data;

/**
 * 用户数量统计 DTO
 *
 * @author harry
 * @since 0.0.1
 */
@Data
public class UserStatsDTO {

    private long total;

    private long enabled;

    private long disabled;

    private long deleted;
}
