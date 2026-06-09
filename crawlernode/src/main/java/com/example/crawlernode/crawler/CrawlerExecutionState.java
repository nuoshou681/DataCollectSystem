package com.example.crawlernode.crawler;

import java.util.concurrent.atomic.AtomicInteger;

public final class CrawlerExecutionState {
    // 并发包 Java原子锁
    private static final AtomicInteger CURRENT_LOAD = new AtomicInteger(0);

    private CrawlerExecutionState() {
    }

    public static int incrementLoad() {
        return CURRENT_LOAD.incrementAndGet();
    }

    public static int decrementLoad() {
        return Math.max(0, CURRENT_LOAD.updateAndGet(value -> Math.max(0, value - 1)));
    }

    public static int currentLoad() {
        return CURRENT_LOAD.get();
    }
}
