package com.example.log_monitoring_application.services;

import com.example.log_monitoring_application.exceptions.NotFoundException;
import com.example.log_monitoring_application.exceptions.ReadFileException;
import com.example.log_monitoring_application.models.enums.ProcessStatus;
import com.example.log_monitoring_application.models.etities.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.log_monitoring_application.models.enums.ProcessStatus.END;
import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private static final Integer ERROR_MINUTES = 10;
    private static final Integer WARNING_MINUTES = 5;

    public void monitoringLogs(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("The file is empty.");
        }

        final Map<Integer, Log> startedLogs = new LinkedHashMap<>(); // using linked list to maintain the order of START jobs

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) { //read line by line the file
                Log log = convertToLog(line); //every line is converted into a Log object
                Log corespondentLog = findCorespondentLog(log, startedLogs);  // for an END job, search the START one
                if (isNull(corespondentLog)) {
                    startedLogs.put(log.getPid(), log);
                } else {
                    checkTime(corespondentLog, log); // check what type of log should be used base on how long the job took
                }
            }
        } catch (IOException e) {
            throw new ReadFileException(e.getMessage());
        }
    }

    private void checkTime(Log startLog, Log endLog) {
        LocalTime startTime = startLog.getTime();
        LocalTime endTime = endLog.getTime();
        int totalTime = Math.toIntExact(startTime.until(endTime, ChronoUnit.MINUTES));  // calculate the minutes between startTime and endTime
        if (totalTime > ERROR_MINUTES) {
            log.error("The job with pid {} takes longer than {} min", startLog.getPid(), ERROR_MINUTES);
        } else if (totalTime > WARNING_MINUTES) {
            log.warn("The job with pid {} takes longer than {} min, but less than {} min", startLog.getPid(), WARNING_MINUTES, ERROR_MINUTES);
        }
    }

    private Log findCorespondentLog(Log log, Map<Integer, Log> startedLogs) {
        if (END.equals(log.getProcessStatus())) {
            return startedLogs.computeIfAbsent(log.getPid(), k -> {
                throw new NotFoundException("File incorrect. Didn't find the start status for job with pid " + log.getPid());
            });
        }
        return null; // return null for the START job
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
