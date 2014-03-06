/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.shared.django;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author zizhi.zhzzh
 *         Date: 3/3/14
 *         Time: 8:53 PM
 */
@ContextConfiguration(locations = {"classpath:config.xml"})
public class DjangoClientTests extends AbstractJUnit4SpringContextTests {

    @Resource
    private DjangoClient djangoClient;

    @Test
    public void uploadBigFile() throws Exception{
        File file = new File("/opt/cloud/books/originals/Critical.Thinking.pdf");
        FileInputStream fis = new FileInputStream(file);
        long size = fis.getChannel().size();
        String fileid = djangoClient.uploadFile(fis, DjangoClient.MAX_CHUNK_SIZE, size, "Critical.Thinking", "pdf");
        Assert.assertNotNull(fileid);
    }

    @Test
    public void uploadAndGetFile() throws Exception {
        String content = "Hello World";
        DjangoMessage message = djangoClient.uploadFile(content.getBytes(), "hello.txt");
        Assert.assertNotNull(message);
        System.out.println(message);
        Assert.assertTrue(message.isSuccess());
        String fileId = message.getString("id");
        Assert.assertNotNull(fileId);

        byte[] bytes = djangoClient.downloadFile(fileId);
        Assert.assertEquals(content, new String(bytes));
    }


}
