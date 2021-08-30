package com.taoing.ttsserver.utils;

public class Tool {

    /**
     * 移除字符串首尾空字符的高效方法(利用ASCII值判断,包括全角空格)
     * @param s
     * @return
     */
    public static String fixTrim(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int start = 0;
        int len = s.length();
        int end = len - 1;
        while (start < end && (s.charAt(start) <= 0x20 || s.charAt(start) == '　')) {
            ++start;
        }
        while (start < end && (s.charAt(end) <= 0x20 || s.charAt(end) == '　')) {
            --end;
        }
        ++end;
        return (start > 0 || end < len) ? s.substring(start, end) : s;

    }
}
