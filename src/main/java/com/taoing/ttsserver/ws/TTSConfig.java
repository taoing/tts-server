package com.taoing.ttsserver.ws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Getter
@Setter
@ConfigurationProperties(prefix = "tts.edge")
@Configuration
public class TTSConfig {

    /**
     * 微软可用声音tts列表查询url
     */
    private String voiceListUrl;

    /**
     * 语音合成websocket服务
     */
    private String wssUrl;

    /**
     * token
     */
    private String trustedClientToken;

    /**
     * 默认使用的声音tts(微软晓晓)
     */
    private String voiceName;

    /**
     * 声音编码
     */
    private String codec;

    /**
     * 区域
     */
    private String local;

    private boolean sentenceBoundaryEnabled;

    private boolean wordBoundaryEnabled;

    /**
     * 语音合成配置
     * @return
     */
    public String getSynthesizeConfig() {
        String prefix = "X-Timestamp:+" + getTime() + "\r\n" +
                "Content-Type:application/json; charset=utf-8\r\n" +
                "Path:speech.config\r\n\r\n";
        String config = "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"%s\",\"wordBoundaryEnabled\":\"%s\"},\"outputFormat\":\"%s\"}}}}\r\n";
        config = String.format(config, sentenceBoundaryEnabled ? "true" : "false",
                wordBoundaryEnabled ? "true" : "false", codec);
        return prefix + config;
    }

    public String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (中国标准时间)", Locale.ENGLISH);
        Date date = new Date();
        return sdf.format(date);
    }
}
