package com.web.watr.utils;

public class FileUtils {

    public static boolean isValidFileType(String fileExtension) {
        return fileExtension.equalsIgnoreCase(".ttl")
                || fileExtension.equalsIgnoreCase(".rdf")
                || fileExtension.equalsIgnoreCase(".nt")
                || fileExtension.equalsIgnoreCase(".nq")
                || fileExtension.equalsIgnoreCase(".jsonld")
                || fileExtension.equalsIgnoreCase(".json")
                ;
    }
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
