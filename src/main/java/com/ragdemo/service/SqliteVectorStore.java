package com.ragdemo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于SQLite + 关键字搜索的文档存储
 * 无需嵌入模型，轻量可用
 */
@Service
public class SqliteVectorStore {

    private final JdbcTemplate jdbc;

    public SqliteVectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        initTable();
    }

    private void initTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    public void storeDocument(String content) {
        jdbc.update("INSERT INTO documents (content) VALUES (?)", content);
    }

    public List<String> searchSimilar(String query, int topK) {
        // SQLite FTS5 全文搜索（如果支持）或简单LIKE匹配
        var keywords = query.toLowerCase().split("\\s+");
        var conditions = new ArrayList<String>();
        for (var kw : keywords) {
            if (kw.length() > 1) {
                conditions.add("LOWER(content) LIKE '%" + kw.replace("'", "''") + "%'");
            }
        }

        String sql;
        if (conditions.isEmpty()) {
            sql = "SELECT content FROM documents ORDER BY created_at DESC LIMIT " + topK;
        } else {
            sql = "SELECT content FROM documents WHERE " + String.join(" OR ", conditions)
                    + " ORDER BY created_at DESC LIMIT " + topK;
        }

        return jdbc.query(sql, (rs, row) -> rs.getString("content"));
    }
}
