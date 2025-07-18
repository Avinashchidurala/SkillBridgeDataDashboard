package com.example.SkillBridgeDataDashboard.controller;

import com.example.SkillBridgeDataDashboard.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 🔁 Replace "*" with your frontend domain later
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Upload CSV/Excel file
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String result = fileUploadService.processFile(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // 2. Get all table names
    @GetMapping("/tables")
    public List<String> getAllTableNames() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    // 3. Get data from a specific table (with validation)
    @GetMapping("/table/{tableName}")
    public ResponseEntity<?> getTableData(@PathVariable String tableName) {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        List<String> allowedTables = jdbcTemplate.queryForList(sql, String.class);

        if (!allowedTables.contains(tableName)) {
            return ResponseEntity.badRequest().body("Invalid table name: " + tableName);
        }

        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM " + tableName);
        return ResponseEntity.ok(data);
    }
}
