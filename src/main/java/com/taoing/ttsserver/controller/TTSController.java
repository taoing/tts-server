package com.taoing.ttsserver.controller;

import com.taoing.ttsserver.service.TTSService;
import com.taoing.ttsserver.vo.GeneralResponse;
import com.taoing.ttsserver.vo.TextTTSReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequestMapping(value = "/tests")
@RestController
public class TTSController {

    @Autowired
    private TTSService service;

    /**
     * 合成文本语音
     * 接口不支持多线程, 因此, 一个key只支持一个用户调用
     * 可用key: "xxttsa01", "xxttsa02", "xxttsa03"
     * @param response
     * @param param
     */
    @PostMapping(value = "/text2Speech")
    public void text2Speech(HttpServletResponse response, TextTTSReq param) {
        if (log.isDebugEnabled()) {
            log.debug("req: {}", param.toString());
        }
        if (!StringUtils.hasLength(param.getKey())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!StringUtils.hasLength(param.getText())) {
            return;
        }
        this.service.synthesisAudio(response, param);
    }

    @GetMapping(value = "/test")
    public GeneralResponse<Void> apiTest(HttpServletRequest request) {
        log.info("request uri: {}", request.getRequestURI() + "?" + request.getQueryString());
        return new GeneralResponse<>();
    }
}
