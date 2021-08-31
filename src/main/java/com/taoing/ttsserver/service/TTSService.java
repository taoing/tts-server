package com.taoing.ttsserver.service;

import com.taoing.ttsserver.vo.TextTTSReq;
import com.taoing.ttsserver.ws.TTSWebSocket;
import com.taoing.ttsserver.ws.WebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@Service
public class TTSService {

    @Autowired
    private WebSocketManager webSocketManager;

    /**
     * 合成文本语音
     * @param response
     * @param param
     */
    public void synthesisAudio(HttpServletResponse response, TextTTSReq param) {
        TTSWebSocket client = webSocketManager.getClient(param.getKey());
        if (client == null) {
            log.warn("请求key非法");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        client.send(param.getText());
        client.writeAudioStream(response);
    }
}
