package com.example.log_monitoring_application.factory;

import org.springframework.mock.web.MockMultipartFile;

public class FileFactory {

    public static MockMultipartFile createMockFile(String content) {
        return new MockMultipartFile(
                "file",
                "input.log",
                "text/plain",
                content.getBytes());
    }
}
