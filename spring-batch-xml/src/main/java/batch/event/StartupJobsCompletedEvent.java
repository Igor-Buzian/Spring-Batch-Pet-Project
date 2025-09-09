package batch.event;

import org.springframework.context.ApplicationEvent;

public class StartupJobsCompletedEvent extends ApplicationEvent {
    public StartupJobsCompletedEvent(Object source) {
        super(source);
    }
}
