package me.luis.blockseeker.utils.group;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.CompletableFuture;

public class GameSearch {

    private CompletableFuture<?> completableFuture;
    private StopWatch stopWatch;

    public GameSearch(CompletableFuture<?> completableFuture, StopWatch stopWatch) {
        this.completableFuture = completableFuture;
        this.stopWatch = stopWatch;
    }

    public void stop() {
        stopWatch.stop();

//        completableFuture.cancel(true);
        completableFuture.completeExceptionally(new RuntimeException("Forced to end"));
    }

    public boolean isDone() {
        return completableFuture.isDone();
    }

    public void setCompletableFuture(CompletableFuture<?> completableFuture) {
        this.completableFuture = completableFuture;
    }

    public CompletableFuture<?> getCompletableFuture() {
        return completableFuture;
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }
}
