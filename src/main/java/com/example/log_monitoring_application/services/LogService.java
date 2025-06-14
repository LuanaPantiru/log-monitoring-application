package com.example.log_monitoring_application.services;

import com.example.log_monitoring_application.models.enums.ProcessStatus;
import com.example.log_monitoring_application.models.etities.Log;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class LogService {

    public void monitoringLogs(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(convertToLog(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Log convertToLog(String input) {
        List<String> valuesForInput = Arrays.stream(input.split(",")).toList();
        return Log.builder()
                .time(toLocalTime(valuesForInput.get(0)))
                .description(valuesForInput.get(1))
                .processStatus(ProcessStatus.valueOf(valuesForInput.get(2).trim()))
                .pid(Integer.valueOf(valuesForInput.get(3)))
                .build();
    }

    private LocalTime toLocalTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalTime.parse(time, formatter);
    }
}
