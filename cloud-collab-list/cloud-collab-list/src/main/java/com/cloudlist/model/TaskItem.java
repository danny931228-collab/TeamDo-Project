package com.cloudlist.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 任務資料模型。
 * 這個類別會透過 ObjectStream 在 Client 與 Server 之間傳輸，因此必須實作 Serializable。
 */
public class TaskItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS_TODO = "待辦";
    public static final String STATUS_DOING = "進行中";
    public static final String STATUS_DONE = "已完成";

    private long id;
    private String roomId;
    private String content;
    private String status;
    private String creator;
    private String createdAt;
    private String updatedAt;

    public TaskItem() {
    }

    public TaskItem(long id, String roomId, String content, String status,
                    String creator, String createdAt, String updatedAt) {
        this.id = id;
        this.roomId = roomId;
        this.content = content;
        this.status = status;
        this.creator = creator;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static String nowText() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public boolean isDone() {
        return STATUS_DONE.equals(status);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TaskItem other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "#" + id + " [" + status + "] " + content;
    }
}
