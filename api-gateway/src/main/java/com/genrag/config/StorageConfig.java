package com.genrag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.genrag.document.internal.storage.StorageService;
import com.genrag.document.internal.storage.LocalStorageService;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(
        name = "storage.provider",
        havingValue = "LOCAL",
        matchIfMissing = true
    )
    public StorageService localStorageService() {
        return new LocalStorageService();
    }
}
