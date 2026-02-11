package com.arsiwooqq.paymentservice.config;

import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Value("${spring.liquibase.change-log}")
    private String changeLogFile;

    @PostConstruct
    public void init() throws LiquibaseException {
        try (Database database = DatabaseFactory.getInstance().openDatabase(
                uri,
                null,
                null,
                null,
                new ClassLoaderResourceAccessor()
        )) {
            Liquibase liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database);
            log.info("Starting Liquibase migration...");
            liquibase.update(new Contexts(), new LabelExpression());
            log.info("Liquibase migration completed.");
        } catch (Exception e) {
            log.error("Error during Liquibase migration: {}", e.getMessage());
            throw e;
        }
    }
}
