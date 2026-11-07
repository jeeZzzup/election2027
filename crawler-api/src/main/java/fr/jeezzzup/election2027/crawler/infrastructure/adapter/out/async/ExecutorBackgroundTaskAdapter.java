package fr.jeezzzup.election2027.crawler.infrastructure.adapter.out.async;

import fr.jeezzzup.election2027.crawler.application.port.out.BackgroundTaskPort;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ExecutorBackgroundTaskAdapter implements BackgroundTaskPort {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void submit(Runnable task) {
        executor.submit(task);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
