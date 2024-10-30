package com.web.watr.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class DatasetFileService {
    @Value("${fuseki.dataset.path}")
    private String datasetPath;
    private long retentionPeriodDays= 30;
    @Scheduled(fixedDelay = 86400000)
    public void deleteDatasetFilesScheduledTask() {

        File datasetDir= new File(datasetPath);
        if (!datasetDir.exists() || !datasetDir.isDirectory()) {
            System.out.println("Dataset directory does not exist or is not a directory.");
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long retentionPeriodMillis = TimeUnit.DAYS.toMillis(retentionPeriodDays);

        File[] files = datasetDir.listFiles();
        if (files != null){
            for (var file: files){
                if (file.isFile() && (currentTimeMillis - file.lastModified()) > retentionPeriodMillis) {
                    if (file.delete()) {
                        System.out.println("Deleted old dataset: " + file.getName());
                    } else {
                        System.out.println("Failed to delete dataset: " + file.getName());
                    }
                }
            }
        }


    }
}
