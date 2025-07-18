package com.example.SkillBridgeDataDashboard.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class FileUploadService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String processFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("File name is invalid or missing.");
        }

        if (fileName.endsWith(".csv")) {
            return processCSV(file);
        } else if (fileName.endsWith(".xlsx")) {
            return processExcel(file);
        } else {
            throw new IOException("Unsupported file type: " + fileName);
        }
    }

    private String processCSV(MultipartFile file) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) return "CSV file is empty.";

            String[] rawHeaders = rows.get(0);
            String[] headers = sanitizeHeaders(rawHeaders);
            String[] filteredHeaders = filterOutId(headers);

            String tableName = generateTableName(filteredHeaders);
            createTableIfNotExists(tableName, filteredHeaders);
            String insertSql = buildInsertSQL(tableName, filteredHeaders);

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                List<Object> values = new ArrayList<>();
                for (int j = 0; j < headers.length; j++) {
                    if (!headers[j].equalsIgnoreCase("id")) {
                        values.add(row.length > j ? row[j] : null);
                    }
                }
                jdbcTemplate.update(insertSql, values.toArray());
            }

            return "CSV uploaded to table: " + tableName;
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV", e);
        }
    }

    private String processExcel(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();

            if (!iterator.hasNext()) return "Excel file is empty.";

            Row headerRow = iterator.next();
            List<String> rawHeaders = new ArrayList<>();
            for (Cell cell : headerRow) {
                rawHeaders.add(cell.getStringCellValue());
            }

            String[] headers = sanitizeHeaders(rawHeaders.toArray(new String[0]));
            String[] filteredHeaders = filterOutId(headers);

            String tableName = generateTableName(filteredHeaders);
            createTableIfNotExists(tableName, filteredHeaders);
            String insertSql = buildInsertSQL(tableName, Arrays.asList(filteredHeaders));

            while (iterator.hasNext()) {
                Row row = iterator.next();
                List<Object> values = new ArrayList<>();
                for (int i = 0; i < headers.length; i++) {
                    if (!headers[i].equalsIgnoreCase("id")) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        values.add(cell.toString().trim());
                    }
                }
                jdbcTemplate.update(insertSql, values.toArray());
            }

            return "Excel uploaded to table: " + tableName;
        }
    }

    private String[] sanitizeHeaders(String[] headers) {
        Set<String> seen = new HashSet<>();
        List<String> sanitized = new ArrayList<>();
        for (String h : headers) {
            String clean = sanitize(h);
            String unique = clean;
            int count = 1;
            while (seen.contains(unique)) {
                unique = clean + "_" + count++;
            }
            seen.add(unique);
            sanitized.add(unique);
        }
        return sanitized.toArray(new String[0]);
    }

    private String[] filterOutId(String[] headers) {
        return Arrays.stream(headers)
                .filter(h -> !h.equalsIgnoreCase("id"))
                .toArray(String[]::new);
    }

    private String generateTableName(String[] headers) {
        String base = String.join("_", headers).toLowerCase();
        return "table_" + Integer.toHexString(base.hashCode()).replace("-", "x");
    }

    private void createTableIfNotExists(String tableName, String[] headers) {
        String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema='public' AND table_name = ?)";
        boolean exists = Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, tableName));

        if (!exists) {
            StringBuilder sb = new StringBuilder("CREATE TABLE ").append(tableName).append(" (id SERIAL PRIMARY KEY");
            for (String header : headers) {
                sb.append(", ").append(header).append(" TEXT");
            }
            sb.append(")");
            jdbcTemplate.execute(sb.toString());
        }
    }

    private String buildInsertSQL(String tableName, String[] headers) {
        String columns = String.join(", ", headers);
        String placeholders = String.join(", ", Collections.nCopies(headers.length, "?"));
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    private String buildInsertSQL(String tableName, List<String> headers) {
        return buildInsertSQL(tableName, headers.toArray(new String[0]));
    }

    private String sanitize(String col) {
        return col.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }
}
