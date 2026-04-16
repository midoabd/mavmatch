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
            jdbcTemplate.execute("ALTER TABLE meeting_requests DROP COLUMN IF EXISTS match_id");
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS partner_name VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS course_code VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS meeting_date VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS day_of_week VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS meeting_time VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE meeting_requests ADD COLUMN IF NOT EXISTS duration VARCHAR(255)");
            System.out.println("✅ Meeting table migration completed successfully");
        } catch (Exception e) {
            System.out.println("Migration note: " + e.getMessage());
        }
    }
}