package top.harrylei.bitlog.link.model.dto;

import lombok.Data;

/**
 * 友链数量统计传输对象
 *
 * @author Harry
 * @since 2026-08-16
 */
@Data
public class FriendLinkStatsDTO {

    private long total;

    private long pending;

    private long approved;

    private long rejected;
}
