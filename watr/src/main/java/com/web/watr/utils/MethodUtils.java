package com.web.watr.utils;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MethodUtils {

    public static <T> boolean existsAndNotEmpty(List<T> list){
        return  (list !=null && !list.isEmpty());
    }
    private static String languageTagRegex = "^[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*(?:-[a-zA-Z]{2,3})*(?:-[a-zA-Z0-9]{1,8})*$";
    public static enum TRIPLE_TYPE {
        SUBJECT, PREDICATE, OBJECT;
    }
    public String sanitizeFilterId(String filterId){
        if (filterId == null)
            return null;

        return filterId.replace(":","").replace('.','_').replace("//","_").replace("/","_");
    }

}
