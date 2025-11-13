package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.request.ImageRequest;
import com.Cybersoft.Final_Capstone.payload.request.ImageUpdateRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.Imp.ImageServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
public class ImageController {

    @Autowired
    private ImageServiceImp imageService;

    // Note: Use POST /property/complete to add images when creating a property
    // This controller is for managing images after property creation

    @DeleteMapping("/property")
    public ResponseEntity<?> removeImageOfProperty(@RequestBody ImageRequest request) {
        imageService.removeImageOfProperty(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Remove image from property successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<?> updateImage(@ModelAttribute ImageUpdateRequest request) {
        imageService.updateImage(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Update image successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }
}
