package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("")
public class CheckConnect {

    @GetMapping("/")
    public ResponseEntity<?> checkConnect() {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Backend connection successful");
        response.setData("Connected");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkConnectHead() {
        // HEAD request - trả về status 200 OK nhưng không có body
        // Điều này sẽ làm cho response.ok = true và response.status = 200
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        // Handle CORS preflight requests - nhưng CorsConfig đã handle rồi
        return ResponseEntity.ok().build();
    }
}
