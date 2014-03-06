/*
 * Copyright 1999-2004 Alibaba.com All right reserved. This software is the confidential and proprietary information of
 * Alibaba.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with Alibaba.com.
 */
package com.alibaba.shared.django;

import com.alibaba.fastjson.JSON;
import com.alibaba.shared.django.utils.Digests;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created with IntelliJ IDEA.
 *
 * @author zizhi.zhzzh
 *         Date: 3/3/14
 *         Time: 12:25 PM
 */
@Component
public class DjangoClient {

    public static final int MAX_CHUNK_SIZE = 6 * 1024 * 1024;

    public static final String ACCESS_TOKEN_KEY = "token";
    public static final String FILE_KEY = "file";

    private static final Logger LOGGER = LoggerFactory.getLogger("django");

    @Value("#{django.appKey}")
    private String appKey;
    @Value("#{django.appSecret}")
    private String appSecret;

    @Value("#{django.tokenUrl}")
    private String tokenUrl;
    @Value("#{django.uploadFileUrl}")
    private String uploadFileUrl;
    @Value("#{django.downloadFileUrl}")
    private String downloadFileUrl;
    @Value("#{django.transactionUrl}")
    private String transactionUrl;
    @Value("#{django.chunkUrl}")
    private String chunkUrl;
    @Value("#{django.metaUrl}")
    private String metaUrl;

    private String accessToken;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private HttpClient httpClient = HttpClientBuilder.create().build();

    public String accessToken() {
        Lock rl = lock.readLock();
        rl.lock(); // Waiting for refresh!
        try {
            if (accessToken != null) {
                return accessToken;
            }
        } finally {
            rl.unlock();
        }
        if (accessToken == null) {
            try {
                refreshToken();
            } catch (URISyntaxException e) {
                LOGGER.error("refreshTokenError", e);
            } catch (IOException e) {
                LOGGER.error("refreshTokenError", e);
            }
        }
        return accessToken;
    }

    public DjangoMessage fileMetas(final String... fileIds) throws URISyntaxException, IOException {
        return executeRequest(new Supplier<HttpUriRequest>() {
            public HttpUriRequest get() {
                Map<String, String> params = Maps.newHashMap();
                params.put(ACCESS_TOKEN_KEY, accessToken());
                params.put("fileIds", join("|", fileIds));
                return new HttpGet(buildURI(metaUrl, params));
            }
        });
    }


    public byte[] downloadFile(String fileId) throws URISyntaxException, IOException {
        URI uri = new URIBuilder(downloadFileUrl)
                .addParameter(ACCESS_TOKEN_KEY, accessToken())
                .addParameter("fileIds", fileId)
                .build();
        HttpGet req = new HttpGet(uri);
        HttpResponse resp = getHttpClient().execute(req);
        if (resp.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toByteArray(resp.getEntity());
        }
        return null;
    }

    public DjangoMessage uploadFile(final byte[] bytes, final String filename) throws IOException, URISyntaxException {
        return executeRequest(new Supplier<HttpUriRequest>() {
            public HttpUriRequest get() {
                HttpPost post = new HttpPost(uploadFileUrl);
                MultipartEntityBuilder meb = MultipartEntityBuilder.create();
                meb.addTextBody(ACCESS_TOKEN_KEY, accessToken()).addTextBody("md5", Digests.md5(bytes))
                        .addBinaryBody(FILE_KEY, bytes, ContentType.APPLICATION_XML, filename);
                post.setEntity(meb.build());
                return post;
            }
        });
    }

    public String uploadFile(InputStream is, int maxChunkSize, long size, String filename, String ext) throws IOException, URISyntaxException {
        DjangoMessage message = beginUploadTransaction(size, ((int)size / maxChunkSize + 1), ext, null);
        String fileId = null;
        try {
            if (message.isSuccess()) {
                fileId = message.getString("id");
                if (fileId != null) {
                    List<DjangoMessage> djangoMessages = new ArrayList<DjangoMessage>();
                    uploadFileChunks(is, maxChunkSize, fileId, 1, filename, djangoMessages);
                    //TODO check djangoMessages
                }
            }
        } finally {
            if (fileId != null) {
                endUploadTransaction(fileId);
            }
        }
        return fileId;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getUploadFileUrl() {
        return uploadFileUrl;
    }

    public void setUploadFileUrl(String uploadFileUrl) {
        this.uploadFileUrl = uploadFileUrl;
    }

    public String getDownloadFileUrl() {
        return downloadFileUrl;
    }

    public void setDownloadFileUrl(String downloadFileUrl) {
        this.downloadFileUrl = downloadFileUrl;
    }

    public String getTransactionUrl() {
        return transactionUrl;
    }

    public void setTransactionUrl(String transactionUrl) {
        this.transactionUrl = transactionUrl;
    }

    public String getChunkUrl() {
        return chunkUrl;
    }

    public void setChunkUrl(String chunkUrl) {
        this.chunkUrl = chunkUrl;
    }

    public String getMetaUrl() {
        return metaUrl;
    }

    public void setMetaUrl(String metaUrl) {
        this.metaUrl = metaUrl;
    }



    protected void uploadFileChunks(InputStream is,
                                    int maxChunkSize,
                                    final String fileId,
                                    final int sequence,
                                    final String fillename,
                                    List<DjangoMessage> holder) throws IOException, URISyntaxException {
        final MessageDigest digest = Digests.defaultDigest();
        byte[] buf = new byte[8000];
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len = is.read(buf), processCount = len;
        while(len >= 0 && processCount < maxChunkSize) {
            baos.write(buf, 0, len);
            digest.update(buf, 0, len);
            processCount += len;
            if (maxChunkSize <= processCount) {
                break;
            }
            len = is.read(buf);
        }
        if (processCount > 0) {
            holder.add(executeRequest(new Supplier<HttpUriRequest>() {
                public HttpUriRequest get() {
                    MultipartEntityBuilder meb = MultipartEntityBuilder.create();
                    meb.addTextBody(ACCESS_TOKEN_KEY, accessToken())
                        .addTextBody("fileId", fileId)
                        .addTextBody("sequence", String.valueOf(sequence))
                        .addTextBody("md5", Digests.toHexDigest(digest.digest()))
                        .addBinaryBody(FILE_KEY, baos.toByteArray(), ContentType.APPLICATION_OCTET_STREAM, fillename);
                    HttpPost post = new HttpPost(chunkUrl);
                    post.setEntity(meb.build());
                    return post;
                }
            }));
            uploadFileChunks(is, maxChunkSize, fileId, sequence + 1, fillename, holder);
        }
    }


    protected void refreshToken() throws URISyntaxException, IOException {
        Lock wl = lock.writeLock();
        wl.lock();
        try {
            final String timestamp = String.valueOf(new Date().getTime());
            DjangoMessage msg = executeRequest(new Supplier<HttpUriRequest>() {
                public HttpUriRequest get() {
                    Map<String, String> params = Maps.newHashMap();
                    params.put("appKey", appKey);
                    params.put("timestamp", timestamp);
                    params.put("signature", buildSignature(timestamp));
                    return new HttpGet(buildURI(tokenUrl, params, false));
                }
            }, false);
            if (msg != null && msg.isSuccess()) {
                accessToken = msg.getString(ACCESS_TOKEN_KEY);
                LOGGER.info("Received accessToken {}", accessToken);
            }
        } finally {
            wl.unlock();
        }
    }


    protected DjangoMessage beginUploadTransaction(final long size,
                                                   final int blockCount,
                                                   final String ext,
                                                   final String digest) throws URISyntaxException, IOException {
        return executeRequest(new Supplier<HttpUriRequest>() {
            public HttpUriRequest get() {
                Map<String, String> params = Maps.newHashMap();
                params.put("size", String.valueOf(size));
                params.put("number", String.valueOf(blockCount));
                params.put("ext", ext);
                if (!Strings.isNullOrEmpty(digest)) {
                    params.put("md5", digest);
                }
                return new HttpGet(buildURI(transactionUrl, params));
            }
        });
    }

    protected DjangoMessage endUploadTransaction(final String fileId) throws URISyntaxException, IOException {
        return executeRequest(new Supplier<HttpUriRequest>() {
            public HttpUriRequest get() {
                Map<String, String> params = Maps.newHashMap();
                params.put("fileId", fileId);
                URI uri = buildURI(transactionUrl, params);
                return new HttpGet(uri);
            }
        });
    }

    protected String buildSignature(String timestamp){
        StringBuilder sb = new StringBuilder();
        sb.append(appKey).append(timestamp).append(appSecret);
        return Digests.md5(sb.toString());
    }

    protected DjangoMessage executeRequest(Supplier<HttpUriRequest> requestSupplier) throws IOException, URISyntaxException {
        return executeRequest(requestSupplier, true);
    }

    protected DjangoMessage executeRequest(Supplier<HttpUriRequest> requestSupplier, boolean canRetry) throws IOException, URISyntaxException {
        DjangoMessage message = null;
        try {
            HttpResponse response = getHttpClient().execute(requestSupplier.get());
            if (response.getStatusLine().getStatusCode() == 200) {
                message = JSON.parseObject(EntityUtils.toByteArray(response.getEntity()), DjangoMessage.class);
                if (canRetry && message != null && message.isTokenExpired()) {
                    refreshToken();
                    message = executeRequest(requestSupplier, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    protected URI buildURI(String baseUrl, Map<String, String> params){
        return buildURI(baseUrl, params, true);
    }

    protected URI buildURI(String baseUrl, Map<String, String> params, boolean hasAccessToken){
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            if(hasAccessToken) {
                builder.addParameter(ACCESS_TOKEN_KEY, accessToken());
            }
            for (String paramName : params.keySet()) {
                builder.addParameter(paramName, params.get(paramName));
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new DjangoRequestException(e.getMessage(), e);
        }
    }

    protected static String join(String splitter, String... strings) {
        return Joiner.on(splitter).join(Arrays.asList(strings));
    }

    private HttpClient getHttpClient(){
        return httpClient;
    }


}
