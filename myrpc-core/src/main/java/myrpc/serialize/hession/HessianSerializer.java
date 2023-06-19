package myrpc.serialize.hession;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import myrpc.exception.MyRpcException;
import myrpc.serialize.MyRpcSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessianSerializer implements MyRpcSerializer {
    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){
            Hessian2Output hessian2Output = new Hessian2Output(byteArrayOutputStream);
            hessian2Output.writeObject(obj);

            hessian2Output.flush();
            return byteArrayOutputStream.toByteArray();
        }catch (IOException e) {
            throw new MyRpcException("HessianSerializer serialize error",e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)){
            Hessian2Input hessian2Input = new Hessian2Input(byteArrayInputStream);
            return hessian2Input.readObject();
        } catch (Exception e) {
            throw new MyRpcException("HessianSerializer deserialize error",e);
        }
    }
}
