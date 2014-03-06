/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.shared.django;

/**
 * Created with IntelliJ IDEA.
 *
 * @author zizhi.zhzzh
 *         Date: 3/3/14
 *         Time: 7:29 PM
 */
public class DjangoRequestException extends RuntimeException {

    public DjangoRequestException() {
    }

    public DjangoRequestException(String message) {
        super(message);
    }

    public DjangoRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public DjangoRequestException(Throwable cause) {
        super(cause);
    }

    public DjangoRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
