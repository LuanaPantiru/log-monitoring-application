package com.example.log_monitoring_application.services;

import com.example.log_monitoring_application.exceptions.NotFoundException;
import com.example.log_monitoring_application.models.enums.MessageType;
import com.example.log_monitoring_application.models.enums.ProcessStatus;
import com.example.log_monitoring_application.models.etities.Log;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.log_monitoring_application.models.enums.ProcessStatus.END;
import static java.util.Objects.isNull;

@Service
public class LogService {

    private final Map<Integer, Log> startedLogs = new HashMap<>();

    public void monitoringLogs(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Log log = convertToLog(line);
                Log corespondentLog = findCorespondentLog(log);
                if (isNull(corespondentLog)) {
                    startedLogs.put(log.getPid(), log);
                } else {
                    System.out.println(log.getPid() + " " + checkTime(corespondentLog, log).toString());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageType checkTime(Log startLog, Log endLog) {
        LocalTime startTime = startLog.getTime();
        LocalTime endTime = endLog.getTime();
        int totalTime = Math.toIntExact(startTime.until(endTime, ChronoUnit.MINUTES));
        if (totalTime > 10) {
            return MessageType.ERROR;
        } else if (totalTime > 5) {
            return MessageType.WARNING;
        } else {
            return MessageType.VALIDATED;
        }
    }

    private Log findCorespondentLog(Log log) {
        if (END.equals(log.getProcessStatus())) {
            return startedLogs.computeIfAbsent(log.getPid(), k -> {
                throw new NotFoundException("Didn't find the start status for job with pid " + log.getPid());
            });
        }
        return null;
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
