package com.web.watr.controllers;

import com.web.watr.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DatasetController {

    @Value("${fuseki.dataset.path}")
    private String datasetPath;
    private static final int pageSize = 10;
    @GetMapping("/upload")
    public String uploadDatasetPage(Model model){
        return "/content/upload-dataset";
    }
    @GetMapping("/save")
    public String saveDatasetPage(Model model){
        return "/content/save-dataset";
    }

    @PostMapping(value = "upload-dataset", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> uploadDatasetFile(@RequestParam("file")MultipartFile file) {
        try{

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("<div>File is empty</div>");
            }
            String fileName= file.getOriginalFilename();
            Path filePath = Paths.get(datasetPath, fileName);

            if (!FileUtils.isValidFileType(FileUtils.getFileExtension(fileName))){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("<div>Invalid file type. Please upload a .ttl, .rdf, .json, .nq, .nd file.</div>");
            }

            Files.write(filePath, file.getBytes());

            return  ResponseEntity.status(HttpStatus.CREATED).body("<div>File uploaded successfully</div>");
        } catch (IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<div>Failed to upload file " + e.getMessage() + "</div>");

        }
    }

    @GetMapping("/dataset-names")
    public String getFileNames(@RequestParam(defaultValue = "0") int page, Model model){
        File datasetDirectory = new File(datasetPath);
        File[] files = datasetDirectory.listFiles();

        // Check if the directory is valid and contains files
        if (files != null) {
            // Filter the files based on valid file types
            List<String> validFileNames = new ArrayList<>();
            for (File file : files) {
                if (FileUtils.isValidFileType(FileUtils.getFileExtension(file.getName()))) {
                    validFileNames.add(file.getName());
                    System.out.println(file.getName());
                }
            }

            // Implement pagination
            int totalFiles = validFileNames.size();
            int start = page * pageSize;
            int end = Math.min(start + pageSize, totalFiles);
            List<String> paginatedFiles = validFileNames.subList(start, end);

            // Prepare the model for the view
            model.addAttribute("fileNames", paginatedFiles);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", (totalFiles + pageSize - 1) / pageSize); // Calculate total pages
        } else {
            System.out.println("WTF");
            // Handle case where directory is empty or does not exist
            model.addAttribute("fileNames", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
        }

        return "/search/dataset-names";

    }
}
