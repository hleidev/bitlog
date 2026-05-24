package top.harrylei.bitlog.ai.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 能力类型，对应 bitlog.ai.capabilities 配置中的 key
 *
 * @author Harry
 * @since 2026-05-24
 */
@Getter
@RequiredArgsConstructor
public enum AiCapability {

    TEXT("text"), IMAGE("image"), CODE("code");

    /** 与 YAML 配置 key 保持一致 */
    private final String key;
}
