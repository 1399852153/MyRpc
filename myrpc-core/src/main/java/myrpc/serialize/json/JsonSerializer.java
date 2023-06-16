package myrpc.serialize.json;

import myrpc.common.util.JsonUtil;
import myrpc.serialize.MyRpcSerializer;

import java.nio.charset.StandardCharsets;

/**
 * json序列化(便于看问题，但是对异常序列化不友好，不推荐用)
 * */
public class JsonSerializer implements MyRpcSerializer {

    @Override
    public byte[] serialize(Object obj) {
        return JsonUtil.obj2Str(obj).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        return JsonUtil.json2Obj(new String(bytes,StandardCharsets.UTF_8),clazz);
    }
}
