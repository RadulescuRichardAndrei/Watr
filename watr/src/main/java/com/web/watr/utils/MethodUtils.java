package com.web.watr.utils;

import java.util.List;

public class MethodUtils {

    public static <T> boolean existsAndNotEmpty(List<T> list){
        return  (list !=null && !list.isEmpty());
    }
}
