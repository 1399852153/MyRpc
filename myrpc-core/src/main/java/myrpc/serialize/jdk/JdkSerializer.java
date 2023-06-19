package myrpc.serialize.jdk;

import myrpc.exception.MyRpcException;
import myrpc.serialize.MyRpcSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * jdk序列化
 * */
public class JdkSerializer implements MyRpcSerializer {

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)){

            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new MyRpcException("JdkSerializer serialize error",e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)){

            return objectInputStream.readObject();
        } catch (Exception e) {
            throw new MyRpcException("JdkSerializer deserialize error",e);
        }
    }
}
