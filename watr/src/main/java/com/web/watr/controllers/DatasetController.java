package com.web.watr.controllers;

import com.web.watr.utils.FileUploadException;
import com.web.watr.utils.FileUtils;
import com.web.watr.utils.SuccessDeleteException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.concurrent.CompletableFuture;

@Controller
public class DatasetController {

    @Value("${fuseki.dataset.path}")
    private String datasetPath;

    @Value("${accepted.file.extensions}")
    private String allowedExtensions;
    private static final int pageSize = 10;

    @Operation(summary = "Upload Dataset Form", description = "Returns asynchronous the template containing a form for selecting a dataset to upload.")
    @ApiResponse(responseCode = "200", description = "Upload Dataset Form returned successfully")
    @GetMapping("/upload")
    @Async
    public CompletableFuture<String> uploadDatasetPage(Model model){
        model.addAttribute("allowedExtensions",allowedExtensions);
        return CompletableFuture.completedFuture("/content/upload-dataset");
    }

    @Operation(summary = "Save Dataset Form", description = "Returns asynchronously the template for saving a dataset.")
    @ApiResponse(responseCode = "200", description = "Save Dataset Form returned successfully")
    @GetMapping("/save")
    @Async
    public CompletableFuture<String> saveDatasetPage(Model model){

        return CompletableFuture.completedFuture("/content/save-dataset");
    }

    @Operation(summary = "Delete Dataset Form", description = "Returns asynchronously the template for deleting a dataset.")
    @ApiResponse(responseCode = "200", description = "Delete Dataset Form returned successfully")
    @GetMapping("/delete")
    @Async
    public CompletableFuture<String> deleteDatasetPage(Model model){

        return CompletableFuture.completedFuture("/content/delete-dataset");
    }

    @Operation(summary = "Upload Dataset File", description = "Handles file upload and saves it to the dataset path.")
    @ApiResponse(responseCode = "201", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file type or empty file")
    @ApiResponse(responseCode = "500", description = "Server error while uploading file")
    @PostMapping(value = "upload-dataset", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> uploadDatasetFile(
            @Parameter(description = "File to be uploaded") @RequestParam("file")MultipartFile file) {
        try{

            if (file.isEmpty()) {
                throw new FileUploadException("File is empty", HttpStatus.BAD_REQUEST);
            }

            String fileName = file.getOriginalFilename();
            Path filePath = Paths.get(datasetPath, fileName);

            if (!FileUtils.isValidFileType(FileUtils.getFileExtension(fileName))) {
                throw new FileUploadException("Invalid file type. Please upload a "+ allowedExtensions +" file.", HttpStatus.BAD_REQUEST);
            }

            Files.write(filePath, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("<div class=" + '"' + "alert alert-info" + '"' + ">File " + fileName + " uploaded successfully</div>");
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get Dataset Names", description = "Returns a paginated list of dataset names available in the system.")
    @ApiResponse(responseCode = "200", description = "Dataset names retrieved successfully")
    @GetMapping("/dataset-names")
    @Async
    public CompletableFuture<String> getFileNames(
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Optional name filter") @RequestParam(required = false) String name,
            @Parameter(description = "Action type: 'item' for listing, 'save' for downloading or 'delete' for deleting") @RequestParam(defaultValue = "item", required = false) String action,
            @Parameter(description = "Action URL for dataset interaction") @RequestParam() String actionUrl,
            @Parameter(description = "Target for dataset display") @RequestParam() String target,
                               Model model){
        File datasetDirectory = new File(datasetPath);
        File[] files = datasetDirectory.listFiles();

        //action can be item for show items, or save for download files, or delete for delete files
        if(!action.equals("save") && !action.equals("delete")) {
            action = "item";
        }

        // Check if the directory is valid and contains files
        if (files != null) {
            // Filter the files based on valid file types
            List<String> validFileNames = new ArrayList<>();
            for (File file : files) {
                String fileName = file.getName();
                if (FileUtils.isValidFileType(FileUtils.getFileExtension(fileName)) &&
                        (name == null || fileName.toLowerCase().startsWith(name.toLowerCase()))) {
                    validFileNames.add(fileName);
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
            // Handle case where directory is empty or does not exist
            model.addAttribute("fileNames", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
        }
        model.addAttribute("actionUrl", actionUrl);
        model.addAttribute("target",target);
        String viewFile = "/search/dataset-names-" + action;
        model.addAttribute("viewFile",viewFile);

        if (name != null) {
            return CompletableFuture.completedFuture(viewFile);
        }else {
            return CompletableFuture.completedFuture("/search/dataset-names");
        }
    }

    @Operation(summary = "Download Dataset File", description = "Downloads a dataset file by its name.")
    @ApiResponse(responseCode = "200", description = "File downloaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @ApiResponse(responseCode = "404", description = "File not found")
    @ApiResponse(responseCode = "500", description = "Server error while downloading file")
    @GetMapping("/download-file")
    public ResponseEntity<byte[]> downloadDatasetFile(
            @Parameter(description = "Name of the file to download") @RequestParam("fileName") String fileName) {
        try {
            String fileExtension = FileUtils.getFileExtension(fileName);
            if (!FileUtils.isValidFileType(fileExtension)) {
                throw new FileUploadException("Invalid file type. Only files with extensions: "+ allowedExtensions, HttpStatus.BAD_REQUEST);
            }

            Path filePath = Paths.get(datasetPath, fileName);

            if (!Files.exists(filePath)) {
                throw new FileUploadException("File not found: " + fileName, HttpStatus.NOT_FOUND);
            }

            byte[] fileContent = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);

        } catch (IOException e) {
            throw new FileUploadException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Delete Dataset File", description = "Deletes a dataset file from Fuseki.")
    @ApiResponse(responseCode = "200", description = "File deleted successfully")
    @ApiResponse(responseCode = "404", description = "File not found")
    @ApiResponse(responseCode = "500", description = "Server error while deleting file")
    @DeleteMapping("/delete-file")
    public ResponseEntity<String> deleteDatasetFile(
            @Parameter(description = "Name of the file to delete") @RequestParam("fileName") String fileName) {
        try {
            // Construim calea către fișier
            Path filePath = Paths.get(datasetPath, fileName);

            // Verificăm dacă fișierul există
            if (!Files.exists(filePath)) {
                throw new FileUploadException("File not found: " + fileName, HttpStatus.NOT_FOUND);
            }

            // Ștergem fișierul
            Files.delete(filePath);

            //return ResponseEntity.ok("File deleted successfully: " + fileName);
            throw new SuccessDeleteException("File deleted successfully: " + fileName, HttpStatus.OK);
        } catch (IOException e) {
            throw new FileUploadException("Error deleting file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
