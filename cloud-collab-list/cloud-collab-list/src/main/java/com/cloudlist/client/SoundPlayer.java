package com.cloudlist.client;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作音效控制器。
 * 若使用者電腦音效裝置或資源不存在，系統會安靜忽略，避免影響主要功能。
 */
public class SoundPlayer {
    private final Map<String, AudioClip> clips = new HashMap<>();
    private boolean enabled = true;

    public SoundPlayer() {
        load("add", "/sounds/add.wav");
        load("done", "/sounds/done.wav");
        load("delete", "/sounds/delete.wav");
        load("system", "/sounds/system.wav");
    }

    private void load(String name, String path) {
        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                clips.put(name, new AudioClip(url.toExternalForm()));
            }
        } catch (Exception ignored) {
            // 部分環境不支援 JavaFX Media，忽略即可。
        }
    }

    public void play(String name) {
        if (!enabled) return;
        try {
            AudioClip clip = clips.get(name);
            if (clip != null) {
                clip.play();
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
