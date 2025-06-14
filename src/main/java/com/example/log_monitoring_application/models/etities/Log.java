package com.example.log_monitoring_application.models.etities;

import com.example.log_monitoring_application.models.enums.ProcessStatus;
import lombok.*;

import java.time.LocalTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Log {
    private LocalTime time;
    private String description;
    private ProcessStatus processStatus;
    private Integer pid;
}
