package com.macbackpackers.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Repository
public class DatabaseService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseService(DataSource datasource) {
        jdbcTemplate = new JdbcTemplate(datasource);
    }

    @Transactional
    public void updateOption(String optionName, String optionValue) {
        LOGGER.info("Updating {} to {}", optionName, optionValue);
        int rowsUpdated = jdbcTemplate.update("INSERT INTO wp_options(option_name, option_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE option_value = ?", optionName, optionValue, optionValue);
        LOGGER.info("{} rows inserted/updated in wp_options", rowsUpdated);
    }
}
