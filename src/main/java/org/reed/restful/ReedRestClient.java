package org.reed.restful;

import org.reed.entity.ReedResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ReedRestClient Create on 2019-08-06
 * 提供RestTemplate的封装，针对平台标准请求进行封装
 * Copyright (c) 2019-08-06 reed, All rights reserved.
 *
 * @author 田广文
 * @version 1.0
 */
@Component
public class ReedRestClient {

    @Autowired
    private DiscoveryClient discoveryClient;

    private final int DEFAULT_CONNECT_TIMEOUT_MILLISECOND = 180000;
    private final int DEFAULT_READ_TIMEOUT_MILLISECOND = 180000;

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT_MILLISECOND;
    private int readTimeout = DEFAULT_READ_TIMEOUT_MILLISECOND;

    /**
     * 构造方法，用于创建请求对象
     */
    public ReedRestClient()
    {
    }

    /**
     * 获取连接超时时间，单位：毫秒
     * @return 连接超时时间的毫秒数
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置连接超时时间，单位：毫秒
     * @param connectTimeout 连接超时的毫秒数
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * 返回读取超时时间，单位：毫秒
     * @return 返回读取的超时时间
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * 设置读取的超时时间，单位：毫秒
     * @param readTimeout 读取超时的毫秒数
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * GET方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult get(@NonNull String serviceName, @NonNull String uri)
    {
        return post(serviceName, uri, null, null, null);
    }

    /**
     * GET方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param headers 请求头参数表，用于设置请求的请求头
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult get(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, String> headers)
    {
        return get(serviceName, uri, null, headers);
    }

    /**
     * GET方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param headers 请求头参数表，用于设置请求的请求头
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult get(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, ?> uriVariables, @Nullable Map<String, String> headers)
    {
        return universalSyncRequest(serviceName, uri, HttpMethod.GET, uriVariables, headers, null);
    }

    /**
     * POST方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult post(@NonNull String serviceName, @NonNull String uri)
    {
        return post(serviceName, uri, null, null, null);
    }

    /**
     * POST方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param body 请求body，要求为json格式或者MultiValueMap
     * @return 平台统一的返回结果封装对象
     */
    public <T> ReedResult post(@NonNull String serviceName, @NonNull String uri, @Nullable T body)
    {
        return post(serviceName, uri, null, null, body);
    }

    /**
     * POST方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param body 请求body，要求为json格式或者MultiValueMap
     * @return 平台统一的返回结果封装对象
     */
    public <T> ReedResult post(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, ?> uriVariables, @Nullable T body)
    {
        return post(serviceName, uri, uriVariables, null, body);
    }

    /**
     * POST方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param headers 请求头参数表，用于设置请求的请求头
     * @param body 请求body，要求为json格式或者MultiValueMap
     * @return 平台统一的返回结果封装对象
     */
    public <T> ReedResult post(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, ?> uriVariables, @Nullable Map<String, String> headers, @Nullable T body)
    {
        return universalSyncRequest(serviceName, uri, HttpMethod.POST, uriVariables, headers, body);
    }

    /**
     * PUT方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult put(@NonNull String serviceName, @NonNull String uri)
    {
        return put(serviceName, uri, null, null, null);
    }

    /**
     * PUT方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param body 请求body，要求为json格式或者MultiValueMap
     * @return 平台统一的返回结果封装对象
     */
    public <T> ReedResult put(@NonNull String serviceName, @NonNull String uri, @Nullable T body)
    {
        return put(serviceName, uri, null, null, body);
    }

    /**
     * PUT方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param body 请求body，要求为json格式或者MultiValueMap
     * @return 平台统一的返回结果封装对象
     */
    public <T> ReedResult put(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, ?> uriVariables, @Nullable T body)
    {
        return put(serviceName, uri, uriVariables, null, body);
    }

    /**
     * PUT方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param headers 请求头参数表，用于设置请求的请求头
     * @param body 请求body，要求为json格式或者MultiValueMap
     * @return 平台统一的返回结果封装对象
     */
    public <T> ReedResult put(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, ?> uriVariables, @Nullable Map<String, String> headers, @Nullable T body)
    {
        return universalSyncRequest(serviceName, uri, HttpMethod.PUT, uriVariables, headers, body);
    }

    /**
     * DELETE方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult delete(@NonNull String serviceName, @NonNull String uri)
    {
        return delete(serviceName, uri, null, null);
    }

    /**
     * DELETE方法，针对RestTemplate进行封装
     * @param serviceName 微服务名称，经实际测试不区分大小写
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param headers 请求头参数表，用于设置请求的请求头
     * @return 平台统一的返回结果封装对象
     */
    public ReedResult delete(@NonNull String serviceName, @NonNull String uri, @Nullable Map<String, ?> uriVariables, @Nullable Map<String, String> headers)
    {
        return universalSyncRequest(serviceName, uri, HttpMethod.DELETE, uriVariables, headers, null);
    }

    /*
     * 统一的HTTP请求方法，针对RestTemplate的exchange方法进行封装，据说支持PUT/POST/DELETE/GET四种请求方式
     * 后续如果需要实现不基于RestTemplate的封装则需要将universal的两个方法提取出来封装为Engine，本类通过组合不同的Engine对各个Http的封装进行适配
     * @param serviceName 微服务名称
     * @param uri   微服务中服务的uri，其中的path参数通过{参数名}的方式进行拼接
     * @param method 请求方法，据说是支持GET,POST,PUT,DELETE
     * @param uriVariables 路径参数表，key为uri中的参数名，value为具体的参数值
     * @param headers 请求头参数表，用于设置请求的请求头
     * @param body 请求body，要求为json格式
     * @return
     */
    protected <T> ReedResult universalSyncRequest(@NonNull String serviceName, @NonNull String uri, @NonNull HttpMethod method, @Nullable Map<String, ?> uriVariables, @Nullable Map<String, String> headers, @Nullable T body)
    {
        HttpHeaders httpHeaders = parseHeaders(new HttpHeaders(), headers, body);
        if (method == HttpMethod.GET || method == HttpMethod.HEAD)
        {
            body = null;
        }
        HttpEntity<T> httpEntity = new HttpEntity<>(body, httpHeaders);
        RestTemplate restTemplate = restTemplate();
        String requestUrl = null;
        if (uri.toLowerCase().startsWith("http"))
        {
            requestUrl = uri;
        } else {
            requestUrl = serviceNameToHost(serviceName) + "/" + uri;
        }
        if (uriVariables == null)
        {
            uriVariables = new HashMap<>();
        }
        ResponseEntity<ReedResult> responseEntity = restTemplate.exchange(requestUrl, method, httpEntity, ReedResult.class, uriVariables);
        ReedResult result = null;
        // 返回状态处理部分，后续补充错误处理方案
        if (responseEntity.getStatusCode() != HttpStatus.OK)
        {
            result = new ReedResult();
            result.setCode(responseEntity.getStatusCodeValue());
            result.setMessage("");
        } else {
            result = responseEntity.getBody();
        }
        return result;
    }

    /*
        为同步及异步方法提供统一进行请求头的封装处理，
        该方法目前仅在universalSyncRequest和universalAsyncRequest调用。
        子类可以针对该方法进行重写，用于对请求头的通用处理
     */
    protected <T> HttpHeaders parseHeaders(@NonNull HttpHeaders httpHeaders, @Nullable Map<String, String> headers,@Nullable T body)
    {
        // UTF-8编码的JSON格式
        if (body != null && (body instanceof MultiValueMap))
        {
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        } else {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
        if (headers != null)
        {
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                httpHeaders.set(entry.getKey(), entry.getValue());
            }
        }
        return httpHeaders;
    }

    /*
        提供统一的HTTP相关参数配置，目前仅设置了两个超时时间，
        该方法目前仅在universalSyncRequest和universalAsyncRequest调用。
        子类可以针对该方法进行重写，用于处理某些特殊的请求属性，比如缓存等
    */
    protected RestTemplate restTemplate()
    {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(this.connectTimeout);// 设置超时
        requestFactory.setReadTimeout(this.readTimeout);
        return new RestTemplate(requestFactory);
    }

    /*
        将服务名称转换为服务的IP和端口号，用于远程调用
        目前没有使用Ribbon的负载均衡，仅使用平均负载的方式进行服务的发现和负载，
        未来此处应该通过接口的方式动态注入负载均衡策略
     */
    private String serviceNameToHost(String serviceName)
    {
        List<ServiceInstance> services = discoveryClient.getInstances(serviceName);
        if (services != null && services.size() > 0)
        {
            ServiceInstance service = services.get(new Random().nextInt(services.size()));
            return service.getUri().toString();
        }
        return null;
    }
}
