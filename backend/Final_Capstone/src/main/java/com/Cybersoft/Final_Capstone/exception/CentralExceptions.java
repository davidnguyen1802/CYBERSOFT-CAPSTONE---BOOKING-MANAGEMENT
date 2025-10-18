package com.Cybersoft.Final_Capstone.exception;

import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CentralExceptions {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(500);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData(null);
        return ResponseEntity.ok(baseResponse);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<?> handleDataNotFoundException(DataNotFoundException e) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(404);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData(null);
        return ResponseEntity.ok(baseResponse);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<?> handleDuplicateException(DuplicateException e) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(400);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData(null);
        return ResponseEntity.ok(baseResponse);
    }

    @ExceptionHandler(InvalidException.class)
    public ResponseEntity<?> handleInvalidException(InvalidException e) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(402);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData(null);
        return ResponseEntity.ok(baseResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(405);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData(null);
        return ResponseEntity.ok(baseResponse);
    }

    @ExceptionHandler(EmailDuplicateException.class)
    public ResponseEntity<?> handleEmailDuplicateException(EmailDuplicateException e) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(502);
        baseResponse.setMessage(e.getMessage());
        baseResponse.setData(null);
        return ResponseEntity.ok(baseResponse);
    }

}
