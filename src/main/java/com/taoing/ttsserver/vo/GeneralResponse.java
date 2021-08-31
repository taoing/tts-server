package com.taoing.ttsserver.vo;

import lombok.Data;

@Data
public class GeneralResponse<T> {
    private boolean success;

    private String code;

    private String message;

    private String errorMessage;

    private T data;


    public GeneralResponse() {
        this.success = true;
        this.code = "1";
        this.message = "请求成功";
    }

    public GeneralResponse<T> error(String code, String errorMessage) {
        this.success = false;
        this.code = code;
        this.message = "请求失败";
        this.errorMessage = errorMessage;
        return this;
    }

}
