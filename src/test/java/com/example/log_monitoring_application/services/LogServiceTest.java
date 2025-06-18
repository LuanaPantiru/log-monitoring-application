package com.example.log_monitoring_application.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.log_monitoring_application.exceptions.NotFoundException;
import com.example.log_monitoring_application.exceptions.ReadFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.WARN;
import static com.example.log_monitoring_application.factory.FileFactory.createMockFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @InjectMocks
    private LogService logService;
    private final Logger logger = (Logger) LoggerFactory.getLogger(LogService.class);
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void beforeEach() {
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("For an empty file, method will throw IllegalArgumentException")
    void monitoringLogs_emptyFile() {
        MockMultipartFile file = createMockFile("");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> logService.monitoringLogs(file));

        assertEquals("The file is empty.", exception.getMessage());
        assertTrue(listAppender.list.isEmpty());
    }

    @Test
    @DisplayName("For the moment when the file can't be read, method will throw ReadFileException")
    void monitoringLogs_errorReadingFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("I/O failure"));

        ReadFileException exception = assertThrows(ReadFileException.class,
                () -> logService.monitoringLogs(file));

        assertEquals("I/O failure", exception.getMessage());
        assertTrue(listAppender.list.isEmpty());
    }

    @Test
    @DisplayName("For END jobs which don't have a corespondent START job, method will throw NotFoundException")
    void monitoringLogs_startJobNotFound() {
        MockMultipartFile file = createMockFile("11:35:56,scheduled task 032, END,37980");
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> logService.monitoringLogs(file));

        assertEquals("File incorrect. Didn't find the start status for job with pid 37980", exception.getMessage());
        assertTrue(listAppender.list.isEmpty());
    }

    @Test
    @DisplayName("For no exception found, the method will log WARN logs for job which take longer than 5 min, " +
            "and ERROR logs for job which take longer than 10 min")
    void monitoringLogs() {
        List<String> logsMessagesExpected = List.of(
                "The job with pid 37972 takes longer than 5 min, but less than 10 min",
                "The job with pid 81258 takes longer than 10 min");
        MockMultipartFile file = createMockFile(
                """
                        11:36:58,background job wmy, START,81258
                        11:37:00,scheduled task 032, START,37980
                        11:37:10,scheduled task 034, START,37972
                        11:37:14,scheduled task 032, END,37980
                        11:43:10,scheduled task 034, END,37972
                        11:51:44,background job wmy, END,81258""");

        logService.monitoringLogs(file);
        long nrErrorLogs = listAppender.list.stream().filter(event -> ERROR.equals(event.getLevel())).count();
        Long nrWarningLogs = listAppender.list.stream().filter(event -> WARN.equals(event.getLevel())).count();
        List<String> logsMessages = listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

        assertEquals(1L, nrErrorLogs);
        assertEquals(1L, nrWarningLogs);
        assertThat(logsMessages).hasSize(2);
        assertThat(logsMessages).usingRecursiveComparison().isEqualTo(logsMessagesExpected);
    }
}