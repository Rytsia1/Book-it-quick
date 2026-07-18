package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String tesKoneksi() {
        return "Spring Boot bookkeeping backend is running successfully!";
    }
}
