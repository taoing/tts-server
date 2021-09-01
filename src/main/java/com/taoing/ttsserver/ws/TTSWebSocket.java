package com.taoing.ttsserver.ws;

import com.taoing.ttsserver.utils.Tool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.Buffer;
import okio.ByteString;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * websocket client包装
 */
@Slf4j
@Getter
@Setter
public class TTSWebSocket {

    private static int serialNum = 1;

    private Integer id;

    /**
     * 编码(自定义取值, 用以识别websocket client)
     */
    private String code;

    /**
     * websocket client
     */
    private WebSocket webSocket;

    /**
     * websocket是否可用
     */
    private boolean available;

    /**
     * tts配置
     */
    private TTSConfig ttsConfig;

    /**
     * 是否正在合成语音
     */
    private boolean synthesizing;

    /**
     * 音频的mime类型
     */
    private String mime;

    /**
     * 音频数据缓存
     */
    private Buffer buffer;

    public TTSWebSocket() {
    }

    public TTSWebSocket(String code, TTSConfig ttsConfig) {
        this.id = this.getSerialNum();
        this.code = code;
        this.available = true;
        this.ttsConfig = ttsConfig;
        this.webSocket = this.createWs();
    }

    @Override
    public String toString() {
        return "TTSWebSocket{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", available=" + available +
                ", synthesizing=" + synthesizing +
                ", mime='" + mime + '\'' +
                '}';
    }

    /**
     * 部分属性初始化
     */
    public void init() {
        this.mime = null;
        this.buffer = new Buffer();
    }

    public void send(String originText) {
        this.init();
        String text = Tool.fixTrim(originText);
        // 替换一些会导致不返回数据的特殊字符\p{N}
        /**
         * \s matches any whitespace character (equivalent to [\r\n\t\f\v ])
         * \p{P} matches any kind of punctuation character
         * \p{Z} matches any kind of whitespace or invisible separator
         * \p{S} matches any math symbols, currency signs, dingbats, box-drawing characters, etc
         */
        String temp = text.replaceAll("[\\s\\p{P}\\p{Z}\\p{S}]", "");
        if (temp.length() < 1) {
            // 可识别长度为0
            this.synthesizing = false;
            return;
        }
        text = this.escapeXmlChar(text);
        text = this.makeSsmlMsg(text);

        this.synthesizing = true;
        this.webSocket.send(text);
    }

    /**
     * 等待语音合成完成, 写入response
     * @param response
     */
    public void writeAudioStream(HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        while (this.synthesizing) {
            try {
                Thread.sleep(100);
                long time = System.currentTimeMillis() - startTime;
                if (time > 30 * 1000) {
                    // 超时30s
                    this.synthesizing = false;
                }
            } catch (InterruptedException ex) {
                log.error("等待合成语音流异常", ex);
            }
        }

        byte[] bytes = buffer.readByteArray();
        if (bytes.length > 0) {
            response.setHeader("Content-Type", this.mime);
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes, 0, bytes.length);
            } catch (IOException ex) {
                log.error("语音流转储响应流失败: {}", ex.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        // 释放资源
        this.buffer = null;
    }

    /**
     * 音频流写入缓存
     * @param byteString
     */
    public void writeBuffer(ByteString byteString) {
        this.buffer.write(byteString);
    }

    /**
     * websocket不可再使用, 丢弃
     */
    public void drop() {
        this.available = false;
        // 关闭websocket, 释放资源
        this.webSocket.close(1000, null);
        this.webSocket = null;
    }

    /**
     * 构造格式化消息
     * @param text
     * @return
     */
    private String makeSsmlMsg(String text) {
        // 默认 "+0Hz"
        int pitch = 0;
        // 语速+, 默认 "+0%"
        int rate = 0;
        // 音量+, 默认 "+0%"
        int volumn = 0;
        String pitchStr = pitch >= 0 ? "+" + pitch + "Hz" : pitch + "Hz";
        String rateStr = rate >= 0 ? "+" + rate + "%" : rate + "%";
        String volumnStr = volumn >= 0 ? "+" + volumn + "%" : volumn + "%";

        String str = "X-RequestId:" + connectId() + "\r\n" +
                "Content-Type:application/ssml+xml\r\n" +
                "X-Timestamp:" + ttsConfig.getTime() + "Z\r\n" +
                "Path:ssml\r\n\r\n" +
                "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\" xml:lang=\"" + "en-US" + "\">" +
                "<voice  name=\"" + ttsConfig.getVoiceShortName() + "\">" +
                "<prosody pitch=\"" + pitchStr + "\" " +
                "rate =\"" + rateStr + "\" " +
                "volume=\"" + volumnStr + "\">"
                + text
                + "</prosody></voice></speak>";
        if (log.isDebugEnabled()) {
            log.debug("send:\n{}", str);
        }
        return str;
    }

    /**
     * get uuid
     * @return
     */
    private String connectId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建连接服务的websocket
     * @return
     */
    private WebSocket createWs() {
        String url = ttsConfig.getWssUrl() + "?TrustedClientToken=" + ttsConfig.getTrustedClientToken() +
                "&ConnectionId=" + connectId();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36 Edg/92.0.902.84")
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .build();
        // 可以使用匿名内部类
        WebSocket webSocket = new OkHttpClient().newWebSocket(request, new TTSWebSocketListener(this));
        String configMsg = ttsConfig.getSynthesizeConfig();
        if (log.isDebugEnabled()) {
            log.debug("send:\n{}", configMsg);
        }
        webSocket.send(configMsg);
        return webSocket;
    }

    /**
     * 转义xml特殊字符
     * @param str
     * @return
     */
    private String escapeXmlChar(String str) {
        str = str.replace("&", "&amp;");
        str = str.replace(">", "&gt;");
        str = str.replace("<", "&lt;");
        return str;
    }

    private int getSerialNum() {
        int n = serialNum++;
        if (serialNum == Integer.MAX_VALUE) {
            serialNum = 1;
        }
        return n;
    }
}
