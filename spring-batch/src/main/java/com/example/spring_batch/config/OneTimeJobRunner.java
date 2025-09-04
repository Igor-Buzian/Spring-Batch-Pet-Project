package com.example.spring_batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OneTimeJobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job oneTimeJob;

    public OneTimeJobRunner(JobLauncher jobLauncher, @Qualifier("importCsvToDbJob") Job oneTimeJob) {
        this.jobLauncher = jobLauncher;
        this.oneTimeJob = oneTimeJob;
    }

    @Override
    public void run(String... args) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "oneTimeJobRun")
                .toJobParameters();
        jobLauncher.run(oneTimeJob, jobParameters);
    }
}
