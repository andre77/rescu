package si.mazi.rescu.clients;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import si.mazi.rescu.HttpMethod;

public class ApacheConnection implements HttpConnection {
    
    private final String url;
    private final Proxy proxy;
    private final HttpClientBuilder builder = HttpClients.custom();
    private final RequestConfig.Builder requestConfig = RequestConfig.custom();
            
    private boolean executed = false;
    private CloseableHttpResponse res;
    private HttpMethod method = HttpMethod.GET;
    private ByteArrayOutputStream out;
    private final Map<String, String> headers = new HashMap<>();
    
    private CloseableHttpResponse response;

    private ApacheConnection(String url, Proxy proxy) {
        super();
        this.url = url;
        this.proxy = proxy;
    }
    
    public static HttpConnection create(String url, Proxy proxy) throws MalformedURLException, IOException {
        return new ApacheConnection(url, proxy);
    }
    
    private void exec() {
        if (executed) {
            return;
        }
        
        CloseableHttpClient client = builder
                .setDefaultRequestConfig(requestConfig.build())
                .build();
        
        HttpRequestBase request = createRequest(method, url);
        
        if (request instanceof HttpEntityEnclosingRequestBase && out != null) {
            HttpEntityEnclosingRequestBase req = (HttpEntityEnclosingRequestBase) request;
            HttpEntity entity = req.getEntity();
            if (entity == null) {
                entity = new ByteArrayEntity(out.toByteArray());
                req.setEntity(entity);
                headers.remove("Content-Length");   // need to drop this header here, otherwise we get "Content-Length header already present"
            }
        }
        headers.forEach((name, value) -> request.addHeader(name, value));
        try {
            response = client.execute(request);
            
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        executed = true;
    }

    private static HttpRequestBase createRequest(HttpMethod method, String url) {
        switch(method) {
            case GET: return new HttpGet(url); 
            case POST: return new HttpPost(url);
            case PUT: return new HttpPut(url);
            case DELETE: return new HttpDelete(url);
            case HEAD: return new HttpHead(url); 
            case OPTIONS: return new HttpOptions(url);
            case PATCH: return new HttpPatch(url);
            default: throw new RuntimeException("Not supported http method: " + method);
        }
    }

    @Override
    public String getHeaderField(String name) {
        try {
            return res.getFirstHeader(name).getValue();
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public void setRequestMethod(HttpMethod method) throws ProtocolException {
        this.method = method;
    }

    @Override
    public String getRequestMethod() {
        return method.name();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if(out == null) {
            out = new ByteArrayOutputStream();
        }
        return out;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return response.getEntity().getContent();
    }
    @Override
    public InputStream getErrorStream() throws IOException {
        return getInputStream();
    }
    
    @Override
    public void setDoOutput(boolean dooutput) {
     // @TODO ???
    }

    @Override
    public void setDoInput(boolean doinput) {
     // @TODO ???
    }
    
    @Override
    public int getResponseCode() throws IOException {
        exec();
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        
        Map<String, List<String>> result = new HashMap<>();
        Stream.of(response.getAllHeaders()).forEach(h -> {
            List<String> list = result.get(h.getName());
            if (list == null) {
                list = new ArrayList<>();
                result.put(h.getName(), list);
            }
            list.add(h.getValue());
        });
        
        return result;
    }

    @Override
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    /**
     * @param timeout an {@code int} that specifies the timeout value to be used in milliseconds
     */
    public void setReadTimeout(int readTimeout) {
     // @TODO ???
    }

    @Override
    /**
     * @param timeout an {@code int} that specifies the connect timeout value in milliseconds
     */
    public void setConnectTimeout(int connTimeout) {
        requestConfig.setConnectTimeout(connTimeout);
    }

    @Override
    public boolean ssl() {
        return url.startsWith("https");
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
            // @TODO ???
    }

    @Override
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        builder.setSSLHostnameVerifier(hostnameVerifier);
    }

}
