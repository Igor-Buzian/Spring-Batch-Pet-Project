package batch.runner;


import batch.event.StartupJobsCompletedEvent;
import batch.interfaces.StartupJob;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;
@Component
public class OneTimeJobRunner implements ApplicationListener<ContextRefreshedEvent> {

    private final JobLauncher jobLauncher;
    @Autowired
    private  ApplicationContext context;

    public OneTimeJobRunner(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event)  {
        Map<String, Job> jobs = getStartupJobs();
        for (Job job : jobs.values()) {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("run_", String.valueOf(System.currentTimeMillis()))
                    .toJobParameters();
            jobLauncher.run(job, jobParameters);
        }
        // используем context как ApplicationEventPublisher
        context.publishEvent(new StartupJobsCompletedEvent(this));
    }

    public Map<String, Job> getStartupJobs() {
        ListableBeanFactory beanFactory = (ListableBeanFactory) context;
        return beanFactory.getBeansOfType(Job.class).entrySet().stream()
                .filter(entry -> beanFactory.findAnnotationOnBean(entry.getKey(), StartupJob.class) != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
