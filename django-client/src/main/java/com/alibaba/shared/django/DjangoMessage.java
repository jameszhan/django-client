/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.shared.django;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author zizhi.zhzzh
 *         Date: 3/3/14
 *         Time: 3:35 PM
 */
public class DjangoMessage {

    public final static int SUCCESS = 0;
    public final static int TOKEN_EXPIRED = 402;

    private int code = -1;
    private String msg;
    private Map<String, Object> data = new HashMap<String, Object>();

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, Object> getData() {
        if (data == null) {
            data = new HashMap<String, Object>();
        }
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return SUCCESS == code;
    }

    public boolean isTokenExpired(){
        return TOKEN_EXPIRED == code;
    }

    public String getString(String key) {
        Object value = getData().get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    @Override
    public String toString() {
        return "DjangoMessage{" +
            "code='" + code + '\'' +
            ", msg='" + msg + '\'' +
            ", data=" + data +
        '}';
    }
}
