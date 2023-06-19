package myrpc.serialize.model;

import java.io.Serializable;
import java.util.Objects;

public class SerializerTestModel implements Serializable {

    private String string;
    private Integer integer;
    private SerializerTestModel serializerTestModel;

    public SerializerTestModel() {
    }

    public SerializerTestModel(String string, Integer integer, SerializerTestModel serializerTestModel) {
        this.string = string;
        this.integer = integer;
        this.serializerTestModel = serializerTestModel;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public SerializerTestModel getSerializerTestModel() {
        return serializerTestModel;
    }

    public void setSerializerTestModel(SerializerTestModel serializerTestModel) {
        this.serializerTestModel = serializerTestModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializerTestModel that = (SerializerTestModel) o;
        return Objects.equals(string, that.string) && Objects.equals(integer, that.integer) && Objects.equals(serializerTestModel, that.serializerTestModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, integer, serializerTestModel);
    }

    @Override
    public String toString() {
        return "SerializerTestModel{" +
            "string='" + string + '\'' +
            ", integer=" + integer +
            ", serializerTestModel=" + serializerTestModel +
            '}';
    }
}
