package top.harrylei.bitlog.common.handler;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.JdbcType;

/**
 * jsonb 列的通用类型处理器
 * <p>
 * MyBatis-Plus 的 JacksonTypeHandler 绑定参数走 PreparedStatement#setString，PG 驱动对 jsonb 列拒绝 varchar 参数（"column ... is of
 * type jsonb but expression is of type character varying"），所以只覆写参数绑定方式，改用 Types.OTHER 让驱动按目标列类型自行解释。
 * </p>
 *
 * @author Harry
 * @since 2026-09-06
 */
public class JsonbTypeHandler extends JacksonTypeHandler {

    public JsonbTypeHandler(Class<?> type) {
        super(type);
    }

    public JsonbTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, toJson(parameter), Types.OTHER);
    }
}
