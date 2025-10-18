package com.Cybersoft.Final_Capstone.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    void saveFile(MultipartFile file);
    String saveFileWithCustomName(MultipartFile file, String customFileName);
    Resource loadFile(String fileName);
    void deleteFile(String imagePath);
}
