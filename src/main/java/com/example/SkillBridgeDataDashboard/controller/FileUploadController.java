package com.example.SkillBridgeDataDashboard.controller;

import com.example.SkillBridgeDataDashboard.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Upload a CSV or Excel file
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return fileUploadService.processFile(file);
    }

    // 2. Get all table names
    @GetMapping("/tables")
    public List<String> getAllTableNames() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    // 3. Get data from a specific table
    @GetMapping("/table/{tableName}")
    public List<Map<String, Object>> getTableData(@PathVariable String tableName) {
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
    }
}
