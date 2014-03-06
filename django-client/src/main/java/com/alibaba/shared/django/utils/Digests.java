/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.shared.django.utils;

import com.alibaba.shared.django.DjangoRequestException;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author zizhi.zhzzh
 *         Date: 3/3/14
 *         Time: 7:09 PM
 */
public final class Digests {
    private Digests(){}

    public static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    public static final String UTF8 = "UTF-8";

    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA1";

    public static MessageDigest defaultDigest(){
        try {
            return MessageDigest.getInstance(MD5);
        } catch (NoSuchAlgorithmException e) {
            //Never happen.
            throw new DjangoRequestException(e.getMessage(), e);
        }
    }

    public static String md5(String content) {
        return hexDigest(content, MD5);
    }


    public static String md5(byte[] bytes) {
        return hexDigest(bytes, MD5);
    }


    public static String sha1(String content) {
        return hexDigest(content, SHA1);
    }


    public static String sha1(byte[] bytes) {
        return hexDigest(bytes, SHA1);
    }


    public static String hexDigest(String content, String algorithm) {
        return hexDigest(content, algorithm, UTF8);
    }

    public static String hexDigest(String content, String algorithm, String encoding) {
        try {
            byte[] bytes = content.getBytes(encoding);
            return hexDigest(bytes, algorithm);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String hexDigest(byte[] bytes, String algorithm) {
        try {
            MessageDigest md5 = MessageDigest.getInstance(algorithm);
            md5.update(bytes);
            return toHexDigest(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String toHexDigest(byte[] buf){
        int len = buf.length;
        StringBuilder sb = new StringBuilder(len * 2);
        for (byte b : buf) {
            sb.append(HEX_DIGITS[(b >> 4) & 0x0f]).append(HEX_DIGITS[b & 0x0f]);
        }
        return sb.toString();
    }
}
