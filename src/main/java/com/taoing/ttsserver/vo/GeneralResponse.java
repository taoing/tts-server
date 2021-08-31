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
        this.code = "1";
        this.message = "操作成功";
        this.success = true;
    }

    public GeneralResponse<T> error(String code, String errorMessage) {
        this.success = false;
        this.code = code;
        this.errorMessage = errorMessage;
        return this;
    }

}
