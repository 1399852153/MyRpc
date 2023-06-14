package myrpc.common.enums;


import myrpc.common.model.MessageHeader;

/**
 * 消息序列化方式
 * @author shanreng
 */
public enum MessageSerializeType {
    /**
     * 消息序列化方式
     * */
    JSON(transToCode("00001"),"json"),
    HESSIAN(transToCode("00010"),"hessian"), // 暂不支持
    ;

    MessageSerializeType(Boolean[] code, String type) {
        this.code = code;
        this.type = type;
    }

    private Boolean[] code;
    private String type;

    public Boolean[] getCode() {
        return code;
    }

    public String getType() {
        return type;
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
