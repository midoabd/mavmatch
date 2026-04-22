package com.mavmatch.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigration implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS receiver_id BIGINT");
            System.out.println("✅ Migration: receiver_id column added");
        } catch (Exception e) {
            System.out.println("Migration note: " + e.getMessage());
        }
    }
}