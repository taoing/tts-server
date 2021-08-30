package com.taoing.ttsserver.ws;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * websocket client 管理
 */
@Getter
@Setter
public class WebSocketManager {
    /**
     * 可用websocket client识别编码, 最多3个
     */
    public static final String[] AVAILABLE_CODES = {"xxttsa01", "xxttsa02", "xxttsa03"};

    private TTSConfig ttsConfig;

    private Map<String, WebSocketWrapper> webSocketMap = new HashMap<>();

    public WebSocketManager() {
        webSocketMap = new HashMap<>();
        for (String code : AVAILABLE_CODES) {
            webSocketMap.put(code, null);
        }
    }

    public WebSocketWrapper getClient(String code) {
        boolean isMatch = false;
        for (String item : AVAILABLE_CODES) {
            if (item.equals(code)) {
                isMatch = true;
                break;
            }
        }
        if (!isMatch) {
            return null;
        }
        WebSocketWrapper wrapper = webSocketMap.get(code);
        if (wrapper == null) {
            // TODO
            wrapper = new WebSocketWrapper(code, ttsConfig);
        }
        return wrapper;
    }
}
