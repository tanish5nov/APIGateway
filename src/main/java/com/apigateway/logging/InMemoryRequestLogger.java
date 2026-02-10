package com.apigateway.logging;

import com.apigateway.model.LogEntry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class InMemoryRequestLogger implements RequestLogger {
    private final int capacity;
    private final LinkedList<LogEntry> entries = new LinkedList<>();

    public InMemoryRequestLogger(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public synchronized void log(LogEntry entry) {
        entries.addLast(entry);
        if (entries.size() > capacity) {
            entries.removeFirst();
        }
        System.out.println("[Gateway] " + entry.getStatus() + " " + entry.getMethod() + " " + entry.getPath()
                + " decision=" + entry.getDecision() + " reason=" + entry.getReason()
                + " backend=" + entry.getBackendId());
    }

    @Override
    public synchronized List<LogEntry> recent() {
        return new ArrayList<>(entries);
    }
}
