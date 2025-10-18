package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;

@Service
public class FileStorageServiceImp implements FileStorageService {
    @Value("${file.root-path}")
    private String rootPath;

    @Override
    public void saveFile(MultipartFile file) {
        Path root = Paths.get(rootPath);
        try {
            if(!Files.exists(root)) {
                Files.createDirectories(root);
            }
            Files.copy(file.getInputStream(), root.resolve(Objects.requireNonNull(file.getOriginalFilename())),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            if (e instanceof FileAlreadyExistsException) {
                throw new RuntimeException("A file of that name already exists.");
            }

            throw new RuntimeException(e.getMessage());
        }
    }
    
    @Override
    public String saveFileWithCustomName(MultipartFile file, String customFileName) {
        Path root = Paths.get(rootPath);
        try {
            if(!Files.exists(root)) {
                Files.createDirectories(root);
            }
            
            // Get original file extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Create full filename with extension
            String fullFileName = customFileName + extension;
            
            // Save file
            Files.copy(file.getInputStream(), root.resolve(fullFileName),
                    StandardCopyOption.REPLACE_EXISTING);
            
            // Return the relative path
            return "/files/" + fullFileName;
        } catch (Exception e) {
            throw new RuntimeException("Could not save file: " + e.getMessage());
        }
    }

    @Override
    public Resource loadFile(String filename) {

        try {
            Path root = Paths.get(rootPath);
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String ImagePath) {
        Path root = Paths.get(rootPath);
        String fileName = ImagePath.replace("/files/", "");
        try {
            Files.deleteIfExists(root.resolve(fileName));
        } catch (Exception e) {
            throw new RuntimeException("Could not delete the file. Error: " + e.getMessage());
        }
    }

}
