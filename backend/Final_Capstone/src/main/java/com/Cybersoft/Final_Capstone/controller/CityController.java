package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<BaseResponse> getAllCityNames() {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all city names successfully");
        response.setData(cityService.getAllCityNames());
        return ResponseEntity.ok(response);
    }
}

