package myrpc.serialize;

import myrpc.common.enums.MessageSerializeType;
import myrpc.exception.MyRpcException;
import myrpc.serialize.hession.HessianSerializer;
import myrpc.serialize.jdk.JdkSerializer;
import myrpc.serialize.json.JsonSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyRpcSerializerManager {

    public static final Map<MessageSerializeType, MyRpcSerializer> serializerMap = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();

    public static MyRpcSerializer getSerializer(Boolean[] messageSerializeTypeCode){
        MessageSerializeType messageSerializeType = MessageSerializeType.getByCode(messageSerializeTypeCode);
        MyRpcSerializer serializer = serializerMap.get(messageSerializeType);
        if(serializer != null){
            return serializer;
        }else{
            synchronized (LOCK){
                // 双重检查
                if(serializerMap.get(messageSerializeType) != null){
                    return serializerMap.get(messageSerializeType);
                }

                MyRpcSerializer newSerializer = createSerializer(messageSerializeType);
                serializerMap.put(messageSerializeType,newSerializer);
                return newSerializer;
            }
        }
    }

    private static MyRpcSerializer createSerializer(MessageSerializeType messageSerializeType){
        switch (messageSerializeType){
            case JSON:
                return new JsonSerializer();
            case JDK:
                return new JdkSerializer();
            case HESSIAN:
                return new HessianSerializer();
            default:
                throw new MyRpcException("un support MessageSerializeType=" + messageSerializeType);
        }
    }

}
