package myrpc.common.enums;

public enum MessageFlagEnums {

    REQUEST(false,"rpc request"),
    RESPONSE(true,"rpc response"),
    ;

    MessageFlagEnums(boolean code, String type) {
        this.code = code;
        this.type = type;
    }

    private Boolean code;
    private String type;

    public Boolean getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

}
