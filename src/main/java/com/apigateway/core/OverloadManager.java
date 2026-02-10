package com.apigateway.core;

import java.util.concurrent.atomic.AtomicBoolean;

public class OverloadManager {
    private final AtomicBoolean overloaded = new AtomicBoolean(false);

    public boolean isOverloaded() {
        return overloaded.get();
    }

    public void setOverloaded(boolean value) {
        overloaded.set(value);
    }
}
