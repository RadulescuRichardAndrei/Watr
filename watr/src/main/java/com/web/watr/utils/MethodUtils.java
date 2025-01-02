package com.web.watr.utils;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MethodUtils {

    public static <T> boolean existsAndNotEmpty(List<T> list){
        return  (list !=null && !list.isEmpty());
    }

    public String sanitizeFilterId(String filterId){
        if (filterId == null)
            return null;

        return filterId.replace(":","").replace('.','_').replace("//","_").replace("/","_");
    }
}
