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

    public static class CategorizedObjects {
        private List<String> numericalLiterals;
        private List<String> otherObjects;

        public CategorizedObjects(List<String> numericalLiterals, List<String> otherObjects) {
            this.numericalLiterals = numericalLiterals;
            this.otherObjects = otherObjects;
        }

        public List<String> getNumericalLiterals() {
            return numericalLiterals;
        }

        public List<String> getOtherObjects() {
            return otherObjects;
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
