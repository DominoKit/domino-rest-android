package org.dominokit.domino.rest.android;

import cz.msebera.android.httpclient.Consts;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.client.methods.RequestBuilder;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import org.dominokit.domino.rest.shared.BaseRestfulRequest;
import org.dominokit.domino.rest.shared.RestfulRequest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

public class AndroidRestfulRequest extends BaseRestfulRequest {

    private final Map<String, String> parameters = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final HttpClient httpClient;
    private final RequestBuilder requestBuilder;

    public AndroidRestfulRequest(String uri, String method) {
        super(uri, method);
        httpClient = HttpClientBuilder.create().build();
        requestBuilder = RequestBuilder.create(method)
                .setUri(uri);
    }

    @Override
    protected String paramsAsString() {
        return parameters.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&"));
    }

    @Override
    public RestfulRequest addQueryParam(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    @Override
    public RestfulRequest setQueryParam(String key, String value) {
        return addQueryParam(key, value);
    }

    @Override
    public RestfulRequest putHeader(String key, String value) {
        requestBuilder.addHeader(key, value);
        headers.put(key, value);
        return this;
    }

    @Override
    public RestfulRequest putHeaders(Map<String, String> headers) {
        if (nonNull(headers))
            headers.forEach(this::putHeader);
        return this;
    }

    @Override
    public RestfulRequest putParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
        return this;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void sendForm(Map<String, String> formData) {
        List<NameValuePair> form = formData.entrySet()
                .stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
        requestBuilder.setEntity(entity);
        send();
    }

    @Override
    public void sendJson(String json) {
        StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        requestBuilder.setEntity(stringEntity);
        send();
    }

    @Override
    public void send(String data) {
        StringEntity stringEntity = new StringEntity(data, ContentType.TEXT_PLAIN);
        requestBuilder.setEntity(stringEntity);
        send();
    }

    @Override
    public void send() {
        HttpUriRequest httpUriRequest = requestBuilder.build();
        try {
            HttpResponse httpResponse = httpClient.execute(httpUriRequest);
            successHandler.onResponseReceived(new AndroidResponse(httpResponse));
        } catch (IOException e) {
            errorHandler.onError(e);
        }
    }
}
