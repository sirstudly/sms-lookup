package com.macbackpackers.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DatabaseService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final List<JdbcTemplate> jdbcTemplates;

    @Autowired
    public DatabaseService(List<DataSource> datasources) {
        jdbcTemplates = datasources.stream()
                .map(JdbcTemplate::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOption(String optionName, String optionValue) {
        LOGGER.info("Updating {} to {}", optionName, optionValue);
        jdbcTemplates.stream().map(t -> t.update(
                "INSERT INTO wp_options(option_name, option_value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE option_value = ?", optionName, optionValue, optionValue))
                .forEach(rowsUpdated -> LOGGER.info("{} rows inserted/updated in wp_options", rowsUpdated));
    }
}
