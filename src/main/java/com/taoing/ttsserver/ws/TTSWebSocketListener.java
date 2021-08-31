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
        // 触发onFailure后, 再调用close方法, 也不会再触发该方法
        log.info("onClosed: {}-{}", code, reason);
        if (log.isDebugEnabled()) {
            log.debug("websocket完全关闭: {}", client.toString());
        }
        client.setAvailable(false);
    }

    /**
     * 服务端关闭连接, 会触发该方法
     * @param webSocket
     * @param code
     * @param reason
     */
    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.info("onClosing: {}-{}", code, reason);
        if (log.isDebugEnabled()) {
            log.debug("websocket服务端关闭: {}", client.toString());
        }
        // 设置websocket不可用, 进一步关闭该websocket以释放资源
        client.setAvailable(false);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        // websocket连接空闲30s后, 服务端主动断开连接, 触发了该方法, 后续该websocket不再可用
        client.drop();

        String message = t.getMessage();
        if (response != null) {
            message += "-" + response.message();
        }
        log.warn("websocket与服务端中断连接: {}, onFailure: {}", client.toString(), message);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        if (log.isDebugEnabled()) {
            log.debug("recv:\n{}", text);
        }
        String startTag = "turn.start";
        String endTag = "turn.end";
        int startIndex = text.indexOf(startTag);
        int endIndex = text.indexOf(endTag);
        // 生成开始
        if (startIndex != -1) {
            if (log.isDebugEnabled()) {
                log.debug("turn.start");
            }
        } else if (endIndex != -1) {
            // 生成结束
            client.setSynthesizing(false);
            if (log.isDebugEnabled()) {
                log.debug("turn.end");
            }
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        if (log.isDebugEnabled()) {
            log.debug("recv:\n{}", bytes.utf8());
        }
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
            if ("audio/x-wav".equals(client.getMime()) && bytes.indexOf("RIFF".getBytes(StandardCharsets.UTF_8)) != -1) {
                // 去除WAV文件的文件头，解决播放开头时的杂音
                audioIndex += 44;
            }
            client.writeBuffer(bytes.substring(audioIndex));
        }
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        if (log.isDebugEnabled()) {
            log.debug("微软tts websocket服务已连接: {}", client.toString());
        }
    }
}
