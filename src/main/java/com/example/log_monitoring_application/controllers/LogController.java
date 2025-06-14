package com.example.log_monitoring_application.controllers;

import com.example.log_monitoring_application.services.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/upload")
    public void monitoringLogs(@RequestParam("file") MultipartFile file) {
        logService.monitoringLogs(file);
    }
}
