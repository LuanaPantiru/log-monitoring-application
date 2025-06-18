package com.example.log_monitoring_application.controllers;

import com.example.log_monitoring_application.exceptions.ReadFileException;
import com.example.log_monitoring_application.services.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.log_monitoring_application.factory.FileFactory.createMockFile;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoSpyBean
    private LogService logService;

    @Test
    @DisplayName("For an empty file, it should send BAD REQUEST response")
    void monitoringLogs_emptyFileException() throws Exception {
        MockMultipartFile file = createMockFile("");

        mockMvc.perform(multipart("/api/logs/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bad Request: The file is empty."));
    }

    @Test
    @DisplayName("For an unknown exception, it should send INTERNAL SERVER ERROR response")
    void monitoringLogs_unknownException() throws Exception {
        MockMultipartFile file = createMockFile("notTime, scheduled task 032, END,37980");

        mockMvc.perform(multipart("/api/logs/upload")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Unknow error: ")));
    }

    @Test
    @DisplayName("For when the file can't be read, it should send INTERNAL SERVER ERROR response")
    void monitoringLogs_readingFileException() throws Exception {
        MockMultipartFile file = createMockFile("11:35:56, scheduled task 032, END,37980");

        doThrow(new ReadFileException("I/O failure")).when(logService).monitoringLogs(any());

        mockMvc.perform(multipart("/api/logs/upload")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File processing error: I/O failure"));
    }

    @Test
    @DisplayName("For END jobs which don't have a corespondent START job, it should send NOT FOUND response")
    void monitoringLogs_notFoundException() throws Exception {
        MockMultipartFile file = createMockFile("11:35:56, scheduled task 032, END,37980");

        mockMvc.perform(multipart("/api/logs/upload")
                        .file(file))
                .andExpect(status().isNotFound())
                .andExpect(content().string("File incorrect. Didn't find the start status for job with pid 37980"));
    }

    @Test
    @DisplayName("When everything works perfect, it should send OK response")
    void monitoringLogs() throws Exception {
        MockMultipartFile file = createMockFile(
                """
                        11:36:58,background job wmy, START,81258
                        11:37:00,scheduled task 032, START,37980
                        11:37:10,scheduled task 034, START,37972
                        11:37:14,scheduled task 032, END,37980
                        11:43:10,scheduled task 034, END,37972
                        11:51:44,background job wmy, END,81258""");

        mockMvc.perform(multipart("/api/logs/upload")
                        .file(file))
                .andExpect(status().isOk());
    }
}