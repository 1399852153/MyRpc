package myrpc.common.enums;


import myrpc.exchange.model.MessageHeader;
import myrpc.common.util.ArrayUtil;
import myrpc.exception.MyRpcException;

import java.util.Arrays;

/**
 * 消息序列化方式
 */
public enum MessageSerializeType {
    /**
     * 消息序列化方式
     * */
    JSON("00001","json"),
    HESSIAN("00010","hessian"),
    JDK("00011","jdk"),
    ;

    MessageSerializeType(String codeStr, String type) {
        this.code = transToCode(codeStr);
        this.type = type;
        this.codeStr = codeStr;
    }

    private final Boolean[] code;
    private final String type;
    private final String codeStr;

    public Boolean[] getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public String getCodeStr() {
        return codeStr;
    }

    public static MessageSerializeType getByCode(Boolean[] code){
        for(MessageSerializeType item : values()){
            if(ArrayUtil.equals(code,item.getCode())){
                return item;
            }
        }

        throw new MyRpcException("un support MessageSerializeType=" + Arrays.toString(code));
    }

    private static Boolean[] transToCode(String code){
        char[] chars = code.toCharArray();

        if(chars.length != MessageHeader.MESSAGE_SERIALIZE_TYPE_LENGTH){
            throw new RuntimeException("MessageSerializeType code must has " + MessageHeader.MESSAGE_SERIALIZE_TYPE_LENGTH + "bit");
        }

        Boolean[] result = new Boolean[MessageHeader.MESSAGE_SERIALIZE_TYPE_LENGTH];
        for(int i=0; i<chars.length; i++){
            if(chars[i] == '0'){
                result[i] = false;
            }else if(chars[i] == '1'){
                result[i] = true;
            }else{
                throw new RuntimeException("code item must be 0 or 1");
            }
        }

        return result;
    }
}
