package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;


public class HttpURLConnectionBuilder {
    private static Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts =
        ApplicationModel.defaultModel().getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();


    public static HttpURLConnection build(RequestTemplate requestTemplate,
                                          HttpConnectionConfig connectionConfig,
                                          RestMethodMetadata restMethodMetadata
    ) throws Exception {

        HttpConnectionCreateContext httpConnectionCreateContext = createBuildContext(requestTemplate,
            connectionConfig,
            restMethodMetadata);

        for (HttpConnectionPreBuildIntercept httpConnectionPreBuildIntercept : httpConnectionPreBuildIntercepts) {
            // TODO add   attachment
            httpConnectionPreBuildIntercept.intercept(httpConnectionCreateContext);
        }

        // TODO substract  Client  param is requestTemplate,connectionConfig
        HttpURLConnection connection = createBaseConnection(requestTemplate, connectionConfig);


        writeHeaders(requestTemplate, connection);


        return writeBody(requestTemplate, connectionConfig, connection);

    }

    private static HttpConnectionCreateContext createBuildContext(RequestTemplate requestTemplate,
                                                                  HttpConnectionConfig connectionConfig,
                                                                  RestMethodMetadata restMethodMetadata) {
        HttpConnectionCreateContext httpConnectionCreateContext = new HttpConnectionCreateContext();
        httpConnectionCreateContext.setConnectionConfig(connectionConfig);
        httpConnectionCreateContext.setRequestTemplate(requestTemplate);
        httpConnectionCreateContext.setRestMethodMetadata(restMethodMetadata);
        return httpConnectionCreateContext;
    }

    private static HttpURLConnection createBaseConnection(RequestTemplate requestTemplate,
                                                          HttpConnectionConfig connectionConfig) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(requestTemplate.getRequestLine()).openConnection();
        connection.setConnectTimeout(connectionConfig.getConnectTimeout());
        connection.setReadTimeout(connectionConfig.getReadTimeout());
        connection.setAllowUserInteraction(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod(requestTemplate.getHttpMethod());
        return connection;
    }

    private static void writeHeaders(RequestTemplate requestTemplate, HttpURLConnection connection) {
        for (String field : requestTemplate.getAllHeaders().keySet()) {
            for (String value : requestTemplate.getHeader(field)) {
                connection.addRequestProperty(field, value);
            }
        }
    }

    private static HttpURLConnection writeBody(RequestTemplate requestTemplate,
                                               HttpConnectionConfig connectionConfig,
                                               HttpURLConnection connection) throws IOException {
        boolean gzipEncodedRequest = requestTemplate.isGzipEncodedRequest();
        boolean deflateEncodedRequest = requestTemplate.isDeflateEncodedRequest();
        if (requestTemplate.isBodyEmpty()) {
            return connection;
        }
        Integer contentLength = requestTemplate.getContentLength();

        if (contentLength != null) {
            connection.setFixedLengthStreamingMode(contentLength);
        } else {
            connection.setChunkedStreamingMode(connectionConfig.getChunkLength());
        }
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        if (gzipEncodedRequest) {
            out = new GZIPOutputStream(out);
        } else if (deflateEncodedRequest) {
            out = new DeflaterOutputStream(out);
        }
        try {
            out.write(requestTemplate.getSerializedBody());
        } finally {
            try {
                out.close();
            } catch (IOException suppressed) {
            }
        }
        return null;
    }


}
