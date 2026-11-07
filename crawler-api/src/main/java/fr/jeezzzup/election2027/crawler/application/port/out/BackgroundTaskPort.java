package fr.jeezzzup.election2027.crawler.application.port.out;

public interface BackgroundTaskPort {

    void submit(Runnable task);
}
