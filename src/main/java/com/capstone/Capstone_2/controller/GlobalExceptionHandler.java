package com.capstone.Capstone_2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 회원가입 중복 에러 등 처리
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // 그 외 모든 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다: " + e.getMessage());
    }
}
