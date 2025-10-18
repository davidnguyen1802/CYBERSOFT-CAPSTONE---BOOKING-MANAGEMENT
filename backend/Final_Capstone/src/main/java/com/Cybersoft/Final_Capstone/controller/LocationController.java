package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<?> getAllLocationNames() {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all location names successfully");
        response.setData(locationService.getAllLocationNames());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/city/{cityName}")
    public ResponseEntity<BaseResponse> getLocationsByCityName(@PathVariable String cityName) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get locations by city name successfully");
        response.setData(locationService.getLocationsByCityName(cityName));
        return ResponseEntity.ok(response);
    }
}

