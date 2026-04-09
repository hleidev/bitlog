package top.harrylei.bitlog.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * 需要管理员权限才能访问
 *
 * @author harry
 * @since 0.0.1
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('ADMIN')")
public @interface RequiresAdmin {
}
