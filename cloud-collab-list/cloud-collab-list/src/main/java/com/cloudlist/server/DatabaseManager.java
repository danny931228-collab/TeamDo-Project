package com.cloudlist.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.cloudlist.model.TaskItem;

/**
 * SQLite 資料存取層。
 * Server 統一透過此類別新增、更新、刪除與查詢任務，確保任務編號不重複。
 */
public class DatabaseManager implements AutoCloseable {
    private final Connection connection;

    public DatabaseManager(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initSchema();
    }

    private void initSchema() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    room_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    status TEXT NOT NULL,
                    creator TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                );
                """;
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
            st.execute("CREATE INDEX IF NOT EXISTS idx_tasks_room_id ON tasks(room_id)");
        }
    }

    public synchronized TaskItem addTask(String roomId, String content, String creator) throws SQLException {
        String now = TaskItem.nowText();
        String sql = "INSERT INTO tasks(room_id, content, status, creator, created_at, updated_at) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomId);
            ps.setString(2, content);
            ps.setString(3, TaskItem.STATUS_TODO);
            ps.setString(4, creator);
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new TaskItem(id, roomId, content, TaskItem.STATUS_TODO, creator, now, now);
                }
            }
        }
        throw new SQLException("新增任務失敗：無法取得自動產生的任務 ID");
    }

    public synchronized void updateTask(long taskId, String content, String status) throws SQLException {
        String now = TaskItem.nowText();
        String trimmedContent = content == null ? null : content.trim();
        boolean updateContent = trimmedContent != null && !trimmedContent.isEmpty();
        boolean updateStatus = status != null && !status.isBlank();

        if (updateContent && updateStatus) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE tasks SET content=?, status=?, updated_at=? WHERE id=?")) {
                ps.setString(1, trimmedContent);
                ps.setString(2, status);
                ps.setString(3, now);
                ps.setLong(4, taskId);
                ps.executeUpdate();
            }
        } else if (updateContent) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE tasks SET content=?, updated_at=? WHERE id=?")) {
                ps.setString(1, trimmedContent);
                ps.setString(2, now);
                ps.setLong(3, taskId);
                ps.executeUpdate();
            }
        } else if (updateStatus) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE tasks SET status=?, updated_at=? WHERE id=?")) {
                ps.setString(1, status);
                ps.setString(2, now);
                ps.setLong(3, taskId);
                ps.executeUpdate();
            }
        }
    }

    public synchronized void deleteTask(long taskId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
    }

    public synchronized List<TaskItem> getTasks(String roomId) throws SQLException {
        List<TaskItem> tasks = new ArrayList<>();
        String sql = "SELECT id, room_id, content, status, creator, created_at, updated_at " +
                "FROM tasks WHERE room_id=? ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new TaskItem(
                            rs.getLong("id"),
                            rs.getString("room_id"),
                            rs.getString("content"),
                            rs.getString("status"),
                            rs.getString("creator"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    ));
                }
            }
        }
        return tasks;
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
