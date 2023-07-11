package com.mmorrell.arcana.strategies;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public abstract class Strategy {
    // TODO: provide to superclasses: rpcClient, backgroundCache, privateKey
    // so they don't have to implement these themselves

    public UUID uuid = UUID.randomUUID();
    public abstract void start();

    @PostConstruct
    public void startupComplete() {
        log.info(this.getClass().getSimpleName() + " strategy instantiated.");
    }

}
