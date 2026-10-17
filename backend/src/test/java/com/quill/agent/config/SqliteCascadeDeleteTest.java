package com.quill.agent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Path;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQLite 级联删除集成测试：
 * 复用应用真实的 SqliteDataSourceConfig（SQLiteConfig.enforceForeignKeys=true）验证：
 * 1) 每连接 PRAGMA foreign_keys 已开启（这是 ON DELETE CASCADE 生效的前提）；
 * 2) WAL 持久模式已启用；
 * 3) 删除 document 后 doc_version 被级联删除（核心验收点）。
 * 使用 @TempDir 临时库，不污染 C:\Users\lenovo\.writer\writer.db。
 */
class SqliteCascadeDeleteTest {

    @TempDir
    Path tempDir;

    private HikariDataSource dataSource;

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /** 与应用启动时完全相同的数据源配置（同一 Bean） */
    private HikariDataSource newDataSource() {
        dataSource = new SqliteDataSourceConfig().dataSource(tempDir.toString());
        return dataSource;
    }

    private JdbcTemplate initSchema(HikariDataSource ds) throws Exception {
        try (Connection conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("schema.sql"));
        }
        return new JdbcTemplate(ds);
    }

    @Test
    void 每连接外键约束已开启() throws Exception {
        JdbcTemplate jdbc = initSchema(newDataSource());
        Integer fk = jdbc.queryForObject("PRAGMA foreign_keys", Integer.class);
        assertThat(fk).isEqualTo(1);
    }

    @Test
    void WAL持久模式已启用() throws Exception {
        JdbcTemplate jdbc = initSchema(newDataSource());
        String mode = jdbc.queryForObject("PRAGMA journal_mode", String.class);
        assertThat(mode).isEqualToIgnoringCase("wal");
    }

    @Test
    void 删除document级联删除doc_version() throws Exception {
        JdbcTemplate jdbc = initSchema(newDataSource());

        // 插入一篇文档
        jdbc.update("INSERT INTO document (title, genre, content, char_count, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?)",
                "测试文档", "NOVEL", "内容", 2, "2026-10-17 10:00:00", "2026-10-17 10:00:00");
        Integer docId = jdbc.queryForObject("SELECT last_insert_rowid()", Integer.class);

        // 插入两个版本
        jdbc.update("INSERT INTO doc_version (document_id, content_hash, content, char_count, created_at) "
                        + "VALUES (?,?,?,?,?)",
                docId, "hash-1", "内容", 2, "2026-10-17 10:00:00");
        jdbc.update("INSERT INTO doc_version (document_id, content_hash, content, char_count, created_at) "
                        + "VALUES (?,?,?,?,?)",
                docId, "hash-2", "内容2", 3, "2026-10-17 10:00:01");

        // 前置断言：两个版本都在
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM doc_version WHERE document_id=?",
                Integer.class, docId)).isEqualTo(2);

        // 核心操作：删除主表
        jdbc.update("DELETE FROM document WHERE id=?", docId);

        // 级联断言：doc_version 已随主表删除，document 本身也已删除
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM doc_version WHERE document_id=?",
                Integer.class, docId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE id=?",
                Integer.class, docId)).isZero();
    }
}