package myrpc.common.util;

public class ArrayUtil {

    public static boolean equals(Boolean[] bytes1, Boolean[] bytes2){
        if(bytes1.length != bytes2.length){
            return false;
        }

        for(int i=0; i<bytes1.length; i++){
            if(bytes1[i] != bytes2[i]){
                return false;
            }
        }

        return true;
    }
}
