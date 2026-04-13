package top.harrylei.bitlog.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsonUtil 工具类测试")
class JsonUtilTest {

    @Test
    @DisplayName("toJson_正常对象_返回JSON字符串")
    void toJson_withValidObject_returnsJsonString() {
        Map<String, Object> obj = Map.of("name", "harry", "age", 25);

        String json = JsonUtil.toJson(obj);

        assertThat(json).isNotNull();
        assertThat(json).contains("harry");
        assertThat(json).contains("25");
    }

    @Test
    @DisplayName("toJson_null对象_返回null")
    void toJson_withNull_returnsNull() {
        String result = JsonUtil.toJson(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromJson_正常JSON字符串_返回对象")
    void fromJson_withValidJson_returnsObject() {
        String json = "{\"name\":\"harry\",\"value\":42}";

        TestData result = JsonUtil.fromJson(json, TestData.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("harry");
        assertThat(result.getValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("fromJson_空字符串_返回null")
    void fromJson_withBlankString_returnsNull() {
        assertThat(JsonUtil.fromJson("", TestData.class)).isNull();
        assertThat(JsonUtil.fromJson("   ", TestData.class)).isNull();
        assertThat(JsonUtil.fromJson(null, TestData.class)).isNull();
    }

    @Test
    @DisplayName("fromJson_null类型参数_返回null")
    void fromJson_withNullClass_returnsNull() {
        assertThat(JsonUtil.fromJson("{\"name\":\"x\"}", (Class<TestData>) null)).isNull();
    }

    @Test
    @DisplayName("fromJson_非法JSON_返回null")
    void fromJson_withMalformedJson_returnsNull() {
        String malformed = "not-a-json";

        TestData result = JsonUtil.fromJson(malformed, TestData.class);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromJson_TypeReference_泛型集合反序列化成功")
    void fromJson_withTypeReference_returnsGenericList() {
        String json = "[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]";

        List<TestData> result = JsonUtil.fromJson(json, new TypeReference<List<TestData>>() {});

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("a");
        assertThat(result.get(1).getValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("fromJson_TypeReference_null引用_返回null")
    void fromJson_withNullTypeReference_returnsNull() {
        assertThat(JsonUtil.fromJson("{}", (TypeReference<TestData>) null)).isNull();
    }

    @Test
    @DisplayName("toBytes_正常对象_返回非空字节数组")
    void toBytes_withValidObject_returnsByteArray() {
        TestData data = new TestData("test", 99);

        byte[] bytes = JsonUtil.toBytes(data);

        assertThat(bytes).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("toBytes_null对象_返回null")
    void toBytes_withNull_returnsNull() {
        assertThat(JsonUtil.toBytes(null)).isNull();
    }

    @Test
    @DisplayName("fromBytes_字节数组_反序列化成功")
    void fromBytes_withByteArray_returnsObject() {
        TestData original = new TestData("byteTest", 7);
        byte[] bytes = JsonUtil.toBytes(original);

        TestData result = JsonUtil.fromBytes(bytes, TestData.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("byteTest");
        assertThat(result.getValue()).isEqualTo(7);
    }

    @Test
    @DisplayName("fromBytes_空字节数组_返回null")
    void fromBytes_withEmptyBytes_returnsNull() {
        assertThat(JsonUtil.fromBytes(new byte[0], TestData.class)).isNull();
        assertThat(JsonUtil.fromBytes(null, TestData.class)).isNull();
    }

    @Test
    @DisplayName("parseToNode_正常JSON_返回JsonNode")
    void parseToNode_withValidJson_returnsJsonNode() {
        String json = "{\"key\":\"value\"}";

        var node = JsonUtil.parseToNode(json);

        assertThat(node).isNotNull();
        assertThat(node.get("key").asText()).isEqualTo("value");
    }

    @Test
    @DisplayName("parseToNode_空字符串_返回null")
    void parseToNode_withBlank_returnsNull() {
        assertThat(JsonUtil.parseToNode("")).isNull();
        assertThat(JsonUtil.parseToNode(null)).isNull();
    }

    @Test
    @DisplayName("序列化再反序列化_数据一致")
    void serialize_thenDeserialize_dataConsistent() {
        TestData original = new TestData("roundTrip", 100);

        String json = JsonUtil.toJson(original);
        TestData result = JsonUtil.fromJson(json, TestData.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getValue()).isEqualTo(original.getValue());
    }

    static class TestData {
        private String name;
        private int value;

        public TestData() {}

        public TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
