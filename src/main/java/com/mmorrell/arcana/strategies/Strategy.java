package com.mmorrell.arcana.strategies;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public abstract class Strategy {
    public UUID uuid = UUID.randomUUID();
    public abstract void start();

    @PostConstruct
    public void startupComplete() {
        log.info(this.getClass().getSimpleName() + " strategy instantiated.");
    }

    public abstract void stop();
}
