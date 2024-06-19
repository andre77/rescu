package si.mazi.rescu.clients;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import si.mazi.rescu.HttpMethod;

public interface HttpConnection {

    HttpConnectionType getHttpConnectionType();
    String getHeaderField(String name);
    String getRequestMethod();
    OutputStream getOutputStream() throws IOException;
    int getResponseCode() throws IOException;
    Map<String,List<String>> getHeaderFields();
    InputStream getInputStream() throws IOException;
    InputStream getErrorStream() throws IOException;
    void setRequestMethod(HttpMethod method) throws ProtocolException;
    void addHeader(String key, String value);
    void setDoOutput(boolean b);
    void setDoInput(boolean b);
    void setReadTimeout(int readTimeout);
    void setConnectTimeout(int connTimeout);
    
    boolean ssl();
    void setSSLSocketFactory(SSLSocketFactory sslSocketFactory);
    void setHostnameVerifier(HostnameVerifier hostnameVerifier);
    
    /**
     * Determine the response encoding if specified
     * @return The response encoding as a string (taken from "Content-Type")
     */
    default String getResponseEncoding() {

        String charset = null;

        String contentType = getHeaderField("Content-Type");
        if (contentType != null) {
            for (String param : contentType.replace(" ", "").split(";")) {
                if (param.startsWith("charset=")) {
                    charset = param.split("=", 2)[1];
                    break;
                }
            }
        }
        return charset;
    }

}
