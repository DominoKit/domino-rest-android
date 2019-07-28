package org.dominokit.domino.rest.android;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;
import org.dominokit.domino.rest.shared.Response;

import java.io.IOException;
import java.util.Map;

import static java.util.Objects.isNull;

public class AndroidResponse implements Response {

    private final HttpResponse response;

    public AndroidResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public String getHeader(String header) {
        return response.getFirstHeader(header).getValue();
    }

    @Override
    public Map<String, String> getHeaders() {
        return null;
    }

    @Override
    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public String getStatusText() {
        return response.getStatusLine().getReasonPhrase();
    }

    @Override
    public String getBodyAsString() {
        if (isNull(response.getEntity()))
            return null;
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            return null;
        }
    }
}
