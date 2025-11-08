package com.example.lighthousedummydemo;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
public class LighthouseDummyDemoApplication {

    @Autowired(required = false)
    private DataSource dataSource;

    @PostConstruct
    public void testConnection() {
        if (dataSource != null) {
            System.out.println("Testing database connection...");
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("✅ Database connection successful!");
                System.out.println("   Database: " + conn.getCatalog());
                System.out.println("   URL: " + conn.getMetaData().getURL());
                System.out.println("   Driver: " + conn.getMetaData().getDriverName());
            } catch (SQLException e) {
                System.err.println("❌ Database connection failed!");
                System.err.println("   Error Code: " + e.getErrorCode());
                System.err.println("   SQL State: " + e.getSQLState());
                System.err.println("   Message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ DataSource is null - datasource not configured!");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(LighthouseDummyDemoApplication.class, args);
    }
}