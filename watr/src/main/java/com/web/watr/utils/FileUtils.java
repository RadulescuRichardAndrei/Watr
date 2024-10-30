package com.web.watr.utils;

public class FileUtils {

    public static boolean isValidFileType(String fileExtension) {
        return fileExtension.equalsIgnoreCase(".ttl")
                || fileExtension.equalsIgnoreCase(".rdf")
                || fileExtension.equalsIgnoreCase(".json")
                || fileExtension.equalsIgnoreCase(".nd")
                || fileExtension.equalsIgnoreCase(".nq")
                || fileExtension.equalsIgnoreCase(".jsonld");
    }
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
