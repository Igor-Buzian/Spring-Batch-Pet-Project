package com.example.spring_batch.batch.runner;

import com.example.spring_batch.batch.event.StartupJobsCompletedEvent;
import com.example.spring_batch.batch.interfaces.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.ApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerRunner {
    private final JobLauncher jobLauncher;
    private final ApplicationContext context;

    @Value("${spring.time.format}")
    private String TimeFormat;

    @Value("${spring.task.scheduling.enabled}")
    private boolean enabled;

    @EventListener
    public void onStartupJobsCompleted(StartupJobsCompletedEvent event) throws Exception {
        enabled = true;
    }

    @Scheduled(fixedRateString = "${spring.time.fixedRate}")
    public void runScheduledJobs() throws Exception {
        if(enabled)  runJobsWithAnnotation();
    }

    private void runJobsWithAnnotation() throws Exception {

        Map<String, Job> jobs = getStartupJobs();

        for (Job job : jobs.values()) {
            JobParameters parameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(job, parameters);
        }
    }

    public Map<String, Job> getStartupJobs() {
        ListableBeanFactory beanFactory = (ListableBeanFactory) context;
        return beanFactory.getBeansOfType(Job.class).entrySet().stream()
                .filter(entry -> beanFactory.findAnnotationOnBean(entry.getKey(), ScheduledJob.class) != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
