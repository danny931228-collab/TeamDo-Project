package com.cloudlist.net;

import com.cloudlist.model.TaskItem;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-Server 共用訊息封包。
 * 使用 Java 內建序列化可以避免額外 JSON 函式庫，適合期末專題展示 TCP 雙向通訊。
 */
public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Type {
        JOIN,
        LEAVE,
        ADD_TASK,
        UPDATE_TASK,
        DELETE_TASK,
        SNAPSHOT,
        USERS,
        SYSTEM,
        ERROR
    }

    private Type type;
    private String username;
    private String roomId;
    private String content;
    private String status;
    private String text;
    private long taskId;
    private TaskItem task;
    private List<TaskItem> tasks = new ArrayList<>();
    private Set<String> users = new LinkedHashSet<>();

    public Message() {
    }

    public Message(Type type) {
        this.type = type;
    }

    public static Message join(String username, String roomId) {
        Message msg = new Message(Type.JOIN);
        msg.username = username;
        msg.roomId = roomId;
        return msg;
    }

    public static Message system(String text) {
        Message msg = new Message(Type.SYSTEM);
        msg.text = text;
        return msg;
    }

    public static Message error(String text) {
        Message msg = new Message(Type.ERROR);
        msg.text = text;
        return msg;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public TaskItem getTask() {
        return task;
    }

    public void setTask(TaskItem task) {
        this.task = task;
    }

    public List<TaskItem> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }
}
