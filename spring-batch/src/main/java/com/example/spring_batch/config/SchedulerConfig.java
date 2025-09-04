package com.example.spring_batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    private final JobLauncher jobLauncher;
    private final Job job;

    @Value("${spring.time.format}")
    private  String TimeFormat;

    public SchedulerConfig(JobLauncher jobLauncher,@Qualifier("exportDbToCsvJob") Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    @Scheduled(fixedRateString = "${spring.time.fixedRate}")
    public void runJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters parameters = new JobParametersBuilder()
                .addLong("Time: ", System.currentTimeMillis())
                .addString("outputFileName", "users-" + new SimpleDateFormat(TimeFormat).format(new Date()) + ".csv")
                .toJobParameters();

        jobLauncher.run(job, parameters);
    }
}
