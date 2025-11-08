package com.example.lighthousedummydemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DatabaseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get relevant database context based on the user's prompt
     */
    public String getDatabaseContext(String prompt, String databaseConnectionId) {
        try {
            String lowerPrompt = prompt.toLowerCase();

            // Check what tables actually exist in the database
            String tablesSql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                    "ORDER BY table_name";
            List<String> tables = jdbcTemplate.queryForList(tablesSql, String.class);

            if (tables.isEmpty()) {
                return "No tables found in the database.";
            }

            // If prompt mentions specific things, try to query relevant tables
            // But first check what tables actually exist
            String tableToQuery = null;

            // Check if any of the existing tables match what we're looking for
            for (String table : tables) {
                String lowerTable = table.toLowerCase();
                if (lowerPrompt.contains("hospital") && lowerTable.contains("hospital")) {
                    tableToQuery = table;
                    break;
                }
                if ((lowerPrompt.contains("employee") || lowerPrompt.contains("people") ||
                        lowerPrompt.contains("person") || lowerPrompt.contains("name")) &&
                        (lowerTable.contains("employee") || lowerTable.contains("people") ||
                                lowerTable.contains("person") || lowerTable.contains("data"))) {
                    tableToQuery = table;
                    break;
                }
                if (lowerPrompt.contains("department") && lowerTable.contains("department")) {
                    tableToQuery = table;
                    break;
                }
            }

            // If no specific table found, use the first table (likely mock_data)
            if (tableToQuery == null && !tables.isEmpty()) {
                tableToQuery = tables.get(0); // Use first table (probably mock_data)
            }

            if (tableToQuery != null) {
                return queryTable(tableToQuery, 20);
            } else {
                // Fallback: get schema and sample from all tables
                return getSchemaAndSampleData();
            }

        } catch (Exception e) {
            System.err.println("Error getting database context: " + e.getMessage());
            e.printStackTrace();
            return "Error querying database: " + e.getMessage();
        }
    }

    private String queryTable(String tableName, int limit) {
        try {
            // First check if table exists
            String checkTableSql = "SELECT EXISTS (" +
                    "SELECT FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = ?" +
                    ")";
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql, Boolean.class, tableName);

            if (tableExists == null || !tableExists) {
                return "Table '" + tableName + "' does not exist in the database.";
            }

            String sql = "SELECT * FROM " + tableName + " LIMIT " + limit;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            if (rows.isEmpty()) {
                return "Table '" + tableName + "' exists but contains no data.";
            }

            StringBuilder context = new StringBuilder();
            context.append("Data from table '").append(tableName).append("':\n\n");

            // Get column names from first row
            if (!rows.isEmpty()) {
                Map<String, Object> firstRow = rows.get(0);
                context.append("Columns: ").append(String.join(", ", firstRow.keySet())).append("\n\n");
            }

            // Add data rows
            int rowNum = 1;
            for (Map<String, Object> row : rows) {
                context.append("Row ").append(rowNum++).append(": ");
                context.append(row.toString()).append("\n");
            }

            return context.toString();

        } catch (Exception e) {
            System.err.println("Error querying table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
            return "Error querying table '" + tableName + "': " + e.getMessage();
        }
    }

    private String getSchemaAndSampleData() {
        try {
            StringBuilder context = new StringBuilder();
            context.append("Database: mock_data_db\n\n");

            // Get all table names
            String tablesSql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                    "ORDER BY table_name";
            List<String> tables = jdbcTemplate.queryForList(tablesSql, String.class);

            if (tables.isEmpty()) {
                return "No tables found in the database.";
            }

            context.append("Tables: ").append(String.join(", ", tables)).append("\n\n");

            // Get sample data from each table (limit to avoid too much data)
            for (String table : tables) {
                try {
                    String sampleSql = "SELECT * FROM " + table + " LIMIT 5";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sampleSql);

                    if (!rows.isEmpty()) {
                        context.append("Sample data from '").append(table).append("':\n");
                        for (Map<String, Object> row : rows) {
                            context.append("  ").append(row.toString()).append("\n");
                        }
                        context.append("\n");
                    }
                } catch (Exception e) {
                    // Skip tables that can't be queried
                    System.err.println("Error querying table " + table + ": " + e.getMessage());
                }
            }

            return context.toString();

        } catch (Exception e) {
            System.err.println("Error getting schema info: " + e.getMessage());
            e.printStackTrace();
            return "Error getting schema info: " + e.getMessage();
        }
    }
}