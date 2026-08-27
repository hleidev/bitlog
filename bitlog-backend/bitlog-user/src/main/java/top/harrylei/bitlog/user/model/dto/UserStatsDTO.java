package top.harrylei.bitlog.user.model.dto;

import lombok.Data;

/**
 * 用户数量统计 DTO
 *
 * @author Harry
 * @since 2026-04-10
 */
@Data
public class UserStatsDTO {

    private long total;

    private long enabled;

    private long disabled;

    private long deactivated;
}
