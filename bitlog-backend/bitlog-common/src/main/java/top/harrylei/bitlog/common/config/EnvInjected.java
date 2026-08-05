package top.harrylei.bitlog.common.config;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验配置值不是未解析的 {@code ${VAR}} 占位符字面量。空值放行，供 dev 环境省略可选配置。
 *
 * @author Harry
 * @since 2026-08-05
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Pattern(regexp = "^(?!\\$\\{).*")
public @interface EnvInjected {

    String message() default "环境变量未注入，配置读到的是占位符字面量";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
