package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.harrylei.bitlog.common.config.WebMvcConfig;
import top.harrylei.bitlog.common.enums.CodedEnum;
import top.harrylei.bitlog.common.model.BasePage;

/**
 * 枚举查询参数绑定契约测试
 * <p>
 * Spring MVC 默认只按枚举名绑定，响应却用 code 表示枚举；两者不一致时按 code 传参会直接 400
 * </p>
 *
 * @author Harry
 * @since 2026-09-09
 */
@SpringJUnitWebConfig(classes = {EnableWebMvcConfig.class, WebMvcConfig.class})
class EnumQueryBindingContractTest {

    private static final String BASE_PACKAGE = "top.harrylei.bitlog";

    @Autowired
    private ConversionService mvcConversionService;

    private static List<Class<?>> queryParamEnums() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));

        List<Class<?>> enums = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            try {
                Class<?> param = Class.forName(definition.getBeanClassName());
                for (Field field : param.getDeclaredFields()) {
                    if (field.getType().isEnum() && !enums.contains(field.getType())) {
                        enums.add(field.getType());
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("无法加载查询参数类: " + definition.getBeanClassName(), e);
            }
        }
        return enums;
    }

    private static boolean hasIntegerCode(Class<?> type) {
        for (Method method : type.getMethods()) {
            if ("getCode".equals(method.getName())
                    && method.getParameterCount() == 0
                    && (Integer.class.equals(method.getReturnType()) || int.class.equals(method.getReturnType()))) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("扫描得到的查询参数枚举覆盖全部业务模块")
    void queryParamEnums_scanned_coversEveryModule() {
        assertThat(queryParamEnums()).isNotEmpty();
    }

    @Test
    @DisplayName("带整数code的查询参数枚举必须实现CodedEnum，否则按code传参会绑定失败")
    void queryParamEnums_withIntegerCode_implementCodedEnum() {
        List<Class<?>> missing = queryParamEnums().stream()
                .filter(EnumQueryBindingContractTest::hasIntegerCode)
                .filter(type -> !CodedEnum.class.isAssignableFrom(type))
                .toList();

        assertThat(missing).as("这些枚举对外用 code 表示却没实现 CodedEnum").isEmpty();
    }

    @Test
    @DisplayName("每个查询参数枚举都能按枚举名绑定")
    void queryParamEnums_boundByName_succeed() {
        for (Class<?> type : queryParamEnums()) {
            for (Object constant : type.getEnumConstants()) {
                String name = ((Enum<?>) constant).name();
                assertThat(mvcConversionService.convert(name, type))
                        .as("%s 按名绑定 %s", type.getSimpleName(), name)
                        .isEqualTo(constant);
            }
        }
    }

    @Test
    @DisplayName("带code的查询参数枚举都能按code绑定")
    void queryParamEnums_boundByCode_succeed() {
        for (Class<?> type : queryParamEnums()) {
            if (!CodedEnum.class.isAssignableFrom(type)) {
                continue;
            }
            for (Object constant : type.getEnumConstants()) {
                String code = String.valueOf(((CodedEnum) constant).getCode());
                assertThat(mvcConversionService.convert(code, type))
                        .as("%s 按 code 绑定 %s", type.getSimpleName(), code)
                        .isEqualTo(constant);
            }
        }
    }
}

@org.springframework.context.annotation.Configuration
@EnableWebMvc
class EnableWebMvcConfig {}
