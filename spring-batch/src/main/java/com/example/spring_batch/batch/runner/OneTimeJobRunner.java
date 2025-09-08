package com.example.spring_batch.batch.runner;

import com.example.spring_batch.batch.event.StartupJobsCompletedEvent;
import com.example.spring_batch.batch.interfaces.StartupJob;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;


import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OneTimeJobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final ApplicationContext context;
    private final ApplicationEventPublisher publisher;

    @Override
    public void run(String... args) throws Exception {
        Map<String, Job> jobs = getStartupJobs();
        for (Job job : jobs.values()) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("run_", String.valueOf(System.currentTimeMillis()))
                    .toJobParameters();
            jobLauncher.run(job, jobParameters);
        }
        // call event for call 'onStartupJobsCompleted' method in SchedulerRunner class
        publisher.publishEvent(new StartupJobsCompletedEvent(this));
    }

    public Map<String, Job> getStartupJobs() {
        ListableBeanFactory beanFactory = (ListableBeanFactory) context;
        return beanFactory.getBeansOfType(Job.class).entrySet().stream()
                .filter(entry -> beanFactory.findAnnotationOnBean(entry.getKey(), StartupJob.class) != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
