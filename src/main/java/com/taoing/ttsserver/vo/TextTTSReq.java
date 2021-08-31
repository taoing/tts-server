package com.taoing.ttsserver.vo;

import lombok.Data;

@Data
public class TextTTSReq {

    /**
     * 文本
     */
    private String text;

    /**
     * tts服务key
     */
    private String key;
}
