package com.alibaba.shared;

import com.alibaba.shared.django.DjangoClient;
import com.alibaba.shared.django.DjangoMessage;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) throws Exception{
        DjangoClient dc = new DjangoClient();
        byte[] bytes = dc.downloadFile("6pdnFIbyRmCuXoxRoqcgKgoUlIMAAgEA");
        System.out.println(new String(bytes));
    }
}
