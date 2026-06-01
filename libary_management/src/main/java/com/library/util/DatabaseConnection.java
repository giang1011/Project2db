package com.library.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            String jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=PublicLibraryDB;encrypt=true;trustServerCertificate=true;";
            config.setJdbcUrl(jdbcUrl);
            config.setUsername("sa");
            config.setPassword("123456");

            config.setConnectionTimeout(3000);
            config.setPoolName("LibraryHikariPool");

            dataSource = new HikariDataSource(config);

            logger.info("HikariCP DataSource initialized.");
        } catch (Exception e) {

            System.err.println("==================================================");
            System.err.println(" Errol connect SQL Server!");
            System.err.println("==================================================");
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized!");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP Pool has been closed.");
        }
    }
}