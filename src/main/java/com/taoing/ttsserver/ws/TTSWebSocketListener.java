package com.taoing.ttsserver.ws;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

@Slf4j
public class TTSWebSocketListener extends WebSocketListener {

    /**
     * websocket client
     */
    private TTSWebSocket client;


    public TTSWebSocketListener() {
    }

    public TTSWebSocketListener(TTSWebSocket client) {
        this.client = client;
    }

    /**
     * websocket完全关闭
     * @param webSocket
     * @param code
     * @param reason
     */
    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.info("onClosed: {}", reason);
    }

    /**
     * 服务端关闭连接, 会触发该方法
     * @param webSocket
     * @param code
     * @param reason
     */
    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.info("onClosing: {}", reason);
        // 设置websocket不可用, 进一步关闭该websocket以释放资源
        client.setAvailable(false);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        // 每发送一次消息收到回复后, 都会触发一次该方法(大概在接收完成后30s). 但websocket可继续使用
        // 可能意味者本次语音合成调用结束
        String message = t.getMessage();
        if (response != null) {
            message += "-" + response.message();
        }
        log.warn("onFailure: {}", message);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
//        log.debug("recv:\n{}", text);
        String startTag = "turn.start";
        String endTag = "turn.end";
        int startIndex = text.indexOf(startTag);
        int endIndex = text.indexOf(endTag);
        // 生成开始
        if (startIndex != -1) {
            log.debug("turn.start");
        } else if (endIndex != -1) {
            // 生成结束
            client.setSynthesizing(false);
            log.debug("turn.end");
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
//        log.debug("recv:\n{}", bytes.utf8());
        // 音频数据流标志头
        String audioTag = "Path:audio\r\n";
        String startTag = "Content-Type:";
        String endTag = "\r\nX-StreamId";

        int audioIndex = bytes.indexOf(audioTag.getBytes(StandardCharsets.UTF_8));
        int startIndex = bytes.indexOf(startTag.getBytes(StandardCharsets.UTF_8));
        int endIndex = bytes.indexOf(endTag.getBytes(StandardCharsets.UTF_8));
        if (audioIndex != -1) {
            audioIndex += audioTag.length();
            if (client.getMime() == null) {
                client.setMime(bytes.substring(startIndex + startTag.length(), endIndex).utf8());
            }
            if (client.getMime().equals("audio/x-wav") && bytes.indexOf("RIFF".getBytes(StandardCharsets.UTF_8)) != -1) {
                // 去除WAV文件的文件头，解决播放开头时的杂音
                audioIndex += 44;
            }
            client.writeBuffer(bytes.substring(audioIndex));
        }
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
    }
}
