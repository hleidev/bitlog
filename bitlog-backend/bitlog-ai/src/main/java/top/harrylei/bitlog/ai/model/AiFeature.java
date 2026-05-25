package top.harrylei.bitlog.ai.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 功能标识，每个功能携带其所属的能力类型
 *
 * @author Harry
 * @since 2026-05-24
 */
@Getter
@RequiredArgsConstructor
public enum AiFeature {

    ARTICLE_SUGGESTIONS(AiCapability.TEXT_JSON);

    private final AiCapability capability;
}
