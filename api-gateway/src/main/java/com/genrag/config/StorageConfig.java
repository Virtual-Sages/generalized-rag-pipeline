package com.genrag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.genrag.document.internal.storage.StorageService;
import com.genrag.document.internal.storage.LocalStorageService;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(
        name = "storage.provider",
        havingValue = "LOCAL",
        matchIfMissing = true
    )
    public StorageService localStorageService(
        @Value("${storage.location}") String storageLocation
    ) {
        return new LocalStorageService(storageLocation);
    }
}