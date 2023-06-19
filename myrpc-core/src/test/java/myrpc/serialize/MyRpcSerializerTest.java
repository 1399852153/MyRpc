package myrpc.serialize;

import myrpc.serialize.hession.HessianSerializer;
import myrpc.serialize.jdk.JdkSerializer;
import myrpc.serialize.json.JsonSerializer;
import myrpc.serialize.model.SerializerTestModel;
import org.junit.Assert;
import org.junit.Test;

public class MyRpcSerializerTest {

    public static final SerializerTestModel targetObj;

    static {
        SerializerTestModel serializerTestModel = new SerializerTestModel();
        serializerTestModel.setString("string1");
        serializerTestModel.setInteger(100);
        serializerTestModel.setSerializerTestModel(new SerializerTestModel("string2",200,null));
        targetObj = serializerTestModel;
    }

    @Test
    public void testHessianSerializer(){
        HessianSerializer hessianSerializer = new HessianSerializer();

        byte[] bytes = hessianSerializer.serialize(targetObj);
        SerializerTestModel serializerTestModel = (SerializerTestModel) hessianSerializer.deserialize(bytes,SerializerTestModel.class);

        Assert.assertEquals(targetObj,serializerTestModel);
    }

    @Test
    public void testJdkSerializer(){
        JdkSerializer jdkSerializer = new JdkSerializer();

        byte[] bytes = jdkSerializer.serialize(targetObj);
        SerializerTestModel serializerTestModel = (SerializerTestModel) jdkSerializer.deserialize(bytes,SerializerTestModel.class);

        Assert.assertEquals(targetObj,serializerTestModel);
    }

    @Test
    public void testJsonSerializer(){
        JsonSerializer jsonSerializer = new JsonSerializer();

        byte[] bytes = jsonSerializer.serialize(targetObj);
        SerializerTestModel serializerTestModel = (SerializerTestModel) jsonSerializer.deserialize(bytes,SerializerTestModel.class);

        Assert.assertEquals(targetObj,serializerTestModel);
    }
}
