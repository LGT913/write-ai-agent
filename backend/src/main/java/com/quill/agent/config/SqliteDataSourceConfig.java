package com.quill.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteDataSource;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;

/**
 * SQLite 数据源（03-生产规范.md §1.7）：
 * PRAGMA 由 sqlite-jdbc 官方 SQLiteConfig 在每次连接打开时自动应用，
 * 避免 Hikari connection-init-sql 单条限制与自定义包装器样板。
 */
@Configuration
public class SqliteDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteDataSourceConfig.class);

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(@Value("${writer.data-dir}") String dataDir) {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);        // 外键级联删除生效（每连接）
        config.setBusyTimeout(5000);            // 锁等待（每连接）
        config.setJournalMode(JournalMode.WAL); // 持久属性
        config.setSynchronous(SynchronousMode.NORMAL);

        SQLiteDataSource sqlite = new SQLiteDataSource(config);
        sqlite.setUrl("jdbc:sqlite:" + Path.of(dataDir, "writer.db"));

        HikariDataSource hikari = new HikariDataSource();
        hikari.setDataSource(sqlite);           // 让 Hikari 复用 SQLite 连接
        hikari.setMaximumPoolSize(1);           // SQLite 单写，防 database locked
        log.info("SQLite 数据源就绪：{}/writer.db", dataDir);
        return hikari;
    }
}