package com.taoing.ttsserver.ws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * websocket client 管理
 */
@Component
@Getter
@Setter
public class WebSocketManager {
    /**
     * 可用websocket client识别编码, 最多3个
     */
    public static final String[] AVAILABLE_CODES = {"xxttsa01", "xxttsa02", "xxttsa03"};

    @Autowired
    private TTSConfig ttsConfig;

    private Map<String, TTSWebSocket> webSocketMap;

    public WebSocketManager() {
        webSocketMap = new HashMap<>();
        for (String code : AVAILABLE_CODES) {
            webSocketMap.put(code, null);
        }
    }

    public TTSWebSocket getClient(String code) {
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
        TTSWebSocket webSocket = webSocketMap.get(code);
        if (webSocket == null) {
            webSocket = new TTSWebSocket(code, ttsConfig);
        }
        return webSocket;
    }
}
