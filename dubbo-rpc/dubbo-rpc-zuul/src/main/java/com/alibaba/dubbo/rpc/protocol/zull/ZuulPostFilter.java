package com.alibaba.dubbo.rpc.protocol.zull;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.RequestContext;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 * Created by wuyu on 2017/2/13.
 */
public class ZuulPostFilter extends ZuulFilter {


    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 1000;
    }

    @Override
    public boolean shouldFilter() {
        return !RequestContext.getCurrentContext().getZuulResponseHeaders().isEmpty() ||
                RequestContext.getCurrentContext().getResponseDataStream() != null ||
                RequestContext.getCurrentContext().getResponseBody() != null;
    }

    @Override
    public Object run() {
        addResponseHeaders();
        writeResponse();
        return null;
    }

    void writeResponse() {
        RequestContext context = RequestContext.getCurrentContext();

        if (context.getResponseBody() == null && context.getResponseDataStream() == null) {
            return;
        }

        HttpServletResponse servletResponse = context.getResponse();
        servletResponse.setCharacterEncoding("UTF-8");

        OutputStream outStream = null;
        InputStream is = null;
        try {
            outStream = servletResponse.getOutputStream();
            if (RequestContext.getCurrentContext().getResponseBody() != null) {
                String body = RequestContext.getCurrentContext().getResponseBody();
                writeResponse(new ByteArrayInputStream(body.getBytes(Charset.forName("UTF-8"))), outStream);
                return;
            }

            boolean isGzipRequested = false;
            final String requestEncoding = context.getRequest().getHeader(ZuulHeaders.ACCEPT_ENCODING);
            if (requestEncoding != null && requestEncoding.equals("gzip"))
                isGzipRequested = true;

            is = context.getResponseDataStream();
            InputStream inputStream = is;
            if (is != null) {
                if (context.sendZuulResponse()) {
                    if (context.getResponseGZipped() && !isGzipRequested)
                        try {
                            inputStream = new GZIPInputStream(is);
                        } catch (ZipException e) {
                            inputStream = is;
                        }
                    else if (context.getResponseGZipped() && isGzipRequested) {
                        servletResponse.setHeader(ZuulHeaders.CONTENT_ENCODING, "gzip");
                    }
                    writeResponse(inputStream, outStream);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

                if (outStream != null) {
                    outStream.flush();
                    outStream.close();
                }

            } catch (IOException e) {
            }
        }
    }

    void writeResponse(InputStream zin, OutputStream out) throws IOException {
        byte[] bytes = new byte[1024];
        int bytesRead = -1;
        while ((bytesRead = zin.read(bytes)) != -1) {

            try {
                out.write(bytes, 0, bytesRead);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // doubles buffer size if previous read filled it
            if (bytesRead == bytes.length) {
                bytes = new byte[bytes.length * 2];
            }
        }
    }

    private void addResponseHeaders() {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletResponse servletResponse = context.getResponse();
        List<com.netflix.util.Pair<String, String>> zuulResponseHeaders = context.getZuulResponseHeaders();
        HttpServletResponse response = context.getResponse();

    }
}
