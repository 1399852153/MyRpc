package myrpc.serialize.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import myrpc.util.StringUtils;

import java.io.IOException;

/**
 * 带泛型的json工具类（模仿spring的GenericJackson2JsonRedisSerializer）
 * */
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER;

    static{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(
            new SimpleModule().addSerializer(new NullValueSerializer(null)));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //  ignore tips
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        OBJECT_MAPPER = objectMapper;
    }

    public static String obj2Str(Object obj){
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("obj2Str error",e);
        }
    }

    public static <T> T json2Obj(String jsonStr, Class<T> objClass) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, objClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json2Obj error jsonStr="+jsonStr+",objClass=" + objClass,e);
        }
    }

    public static <T> T json2Obj(String jsonStr, TypeReference<T> valueTypeRef) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, valueTypeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json2Obj error jsonStr="+jsonStr+",valueTypeRef=" + valueTypeRef,e);
        }
    }

    /**
     * copy自spring
     */
    private static class NullValueSerializer extends StdSerializer<NullValue> {

        private static final long serialVersionUID = 1999052150548658808L;
        private final String classIdentifier;

        NullValueSerializer(String classIdentifier) {

            super(NullValue.class);
            this.classIdentifier =
                StringUtils.hasText(classIdentifier) ? classIdentifier : "@class";
        }

        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

            jgen.writeStartObject();
            jgen.writeStringField(classIdentifier, NullValue.class.getName());
            jgen.writeEndObject();
        }
    }
}
