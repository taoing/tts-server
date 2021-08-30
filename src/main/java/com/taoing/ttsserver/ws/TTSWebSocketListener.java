package com.taoing.ttsserver.ws;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.Buffer;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class TTSWebSocketListener extends WebSocketListener {

    private boolean isSynthesizing;

    private Buffer mData;

    public TTSWebSocketListener() {
        super();
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosing(webSocket, code, reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        String endTag = "turn.end";
        String startTag = "turn.start";
        int endIndex = text.lastIndexOf(endTag);
        int startIndex = text.lastIndexOf(startTag);
        // 生成开始
        if (startIndex != -1) {
            isSynthesizing = true;
            mData = new Buffer();
        }
        // 生成结束
        if (endIndex != -1) {
            // TODO 读取生成的数据
            mData.readByteString();
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        // 音频数据流标志头
        String audioTag = "Path:audio\r\n";
        String startTag = "Content-Type:";
        String endTag = "\r\nX-StreamId";

        int audioIndex = bytes.lastIndexOf(audioTag.getBytes(StandardCharsets.UTF_8)) + audioTag.length();
        int startIndex = bytes.lastIndexOf(startTag.getBytes(StandardCharsets.UTF_8)) + startTag.length();
        int endIndex = bytes.lastIndexOf(endTag.getBytes(StandardCharsets.UTF_8));
        if (audioIndex != -1) {
            try {
                String currentMime = bytes.substring(startIndex, endIndex).utf8();
                boolean needDecode = false;
                if (needDecode) {
                    if (currentMime.equals("audio/x-wav") && bytes.lastIndexOf("RIFF".getBytes(StandardCharsets.UTF_8)) != -1) {
                        // 去除WAV文件的文件头，解决播放开头时的杂音
                        audioIndex += 44;
                    }
                    bytes.substring(audioIndex);
                    // TODO
                } else {
                    mData.write(bytes.substring(audioIndex));
                }
            } catch (Exception e) {
                isSynthesizing = false;
            }

        }
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
    }
}
