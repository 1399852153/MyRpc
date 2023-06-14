package myrpc.serialize.json;

/**
 * copy自spring
 * */
public class NullValue {

    public static final Object INSTANCE = new NullValue();

    private static final long serialVersionUID = 1L;

    private NullValue() {
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
