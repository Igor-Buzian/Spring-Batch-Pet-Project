package batch.runner;


import batch.event.StartupJobsCompletedEvent;
import batch.interfaces.ScheduledJob;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Getter
@Setter
public class SchedulerRunner implements ApplicationContextAware {
    private final JobLauncher jobLauncher;
    private ApplicationContext context;

    private String timeFormat;
    private Boolean enabled = Boolean.FALSE;

    @EventListener
    public void onStartupJobsCompleted(StartupJobsCompletedEvent event) throws Exception {
        enabled = true;
    }

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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
