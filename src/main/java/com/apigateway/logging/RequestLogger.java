package com.apigateway.logging;

import com.apigateway.model.LogEntry;

import java.util.List;

public interface RequestLogger {
    void log(LogEntry entry);
    List<LogEntry> recent();
}
