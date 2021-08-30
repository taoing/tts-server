package com.taoing.ttsserver.ws;

import com.taoing.ttsserver.utils.Tool;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.util.UUID;

/**
 * websocket client包装管理
 */
@Getter
@Setter
public class WebSocketWrapper {

    /**
     * websocket client
     */
    private WebSocket webSocket;

    /**
     * 编码(自定义取值, 用以识别websocket client)
     */
    private String code;

    /**
     * 上次访问时间戳
     */
    private long lastAccessTimeStamp;

    /**
     * tts配置
     */
    private TTSConfig ttsConfig;

    private boolean synthesizeDone;

    public WebSocketWrapper() {
    }

    public WebSocketWrapper(String code, TTSConfig ttsConfig) {
        this.ttsConfig = ttsConfig;
        this.code = code;
        this.webSocket = this.createWs();
        this.lastAccessTimeStamp = System.currentTimeMillis();
    }

    public void send(String originText) {
        String text = Tool.fixTrim(originText);
        // 替换一些会导致不返回数据的特殊字符\p{N}
        String temp = text.replaceAll("[\\s\\p{P}\\p{Z}\\p{S}]", "");
        if (temp.length() < 1) {
            // 长度为0
            synthesizeDone = true;
            return;
        }

        text = text.replace("]]>", "");
        text = this.makeSsmlMsg(text);
        webSocket.send(text);
        synthesizeDone = false;
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

        text = "<![CDATA[" + text + "]]>";

        String str = "X-RequestId:" + connectId() + "\r\n" +
                "Content-Type:application/ssml+xml\r\n" +
                "X-Timestamp:" + ttsConfig.getTime() + "Z\r\n" +
                "Path:ssml\r\n\r\n" +
                "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"" + ttsConfig.getLocal() + "\">" +
                "<voice  name=\"" + ttsConfig.getVoiceName() + "\">" +
                "<prosody pitch=\"" + pitchStr + "\" " +
                "rate =\"" + rateStr + "\" " +
                "volume=\"+" + volumnStr + "\">"
                + text
                + "</prosody></voice></speak>";
        return str;
    }

    private String connectId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

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
        WebSocket webSocket = new OkHttpClient().newWebSocket(request, new TTSWebSocketListener());
        webSocket.send(ttsConfig.getSynthesizeConfig());
        return webSocket;
    }
}
