package top.harrylei.bitlog.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * 需要登录才能访问
 *
 * @author harry
 * @since 2026-04-09
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("isAuthenticated()")
public @interface RequiresLogin {
}
