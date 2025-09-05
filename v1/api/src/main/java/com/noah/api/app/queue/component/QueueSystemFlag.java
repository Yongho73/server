package com.noah.api.app.queue.component;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component
public class QueueSystemFlag {
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    public boolean isEnabled() { return enabled.get(); }
    public void setEnabled(boolean v) { enabled.set(v); }
}