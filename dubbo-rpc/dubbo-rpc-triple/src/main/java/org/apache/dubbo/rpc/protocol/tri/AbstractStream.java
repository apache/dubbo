package org.apache.dubbo.rpc.protocol.tri;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.Constants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public abstract class AbstractStream implements Stream {
    public static final boolean ENABLE_ATTACHMENT_WRAP = Boolean.parseBoolean(ConfigUtils.getProperty("triple.attachment", "false"));
    private static final GrpcStatus TOO_MANY_DATA = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Too many data");
    private final boolean needWrap;
    private final ChannelHandlerContext ctx;
    private final MultipleSerialization multipleSerialization;
    private final URL url;
    private Http2Headers headers;
    private Http2Headers te;
    private InputStream data;
    private String serializeType;

    protected AbstractStream(URL url, ChannelHandlerContext ctx, boolean needWrap) {
        this.ctx = ctx;
        this.url = url;
        this.needWrap = needWrap;
        if (needWrap) {
            this.multipleSerialization = loadFromURL(url);
        } else {
            this.multipleSerialization = null;
        }
    }

    public static MultipleSerialization loadFromURL(URL url) {
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, "default");
        return ExtensionLoader.getExtensionLoader(MultipleSerialization.class).getExtension(value);
    }

    public URL getUrl() {
        return url;
    }

    public String getSerializeType() {
        return serializeType;
    }

    protected void setSerializeType(String serializeType) {
        this.serializeType = serializeType;
    }

    public boolean isNeedWrap() {
        return needWrap;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public Http2Headers getTe() {
        return te;
    }

    public InputStream getData() {
        return data;
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    @Override
    public void onData(InputStream in) {
        if (data != null) {
            responseErr(ctx, TOO_MANY_DATA);
            return;
        }

        this.data = in;
    }

    public void onHeaders(Http2Headers headers) {
        if (this.headers == null) {
            this.headers = headers;
        } else if (te == null) {
            this.te = headers;
        }
    }

    protected void convertAttachment(Http2Headers trailers, Map<String, Object> attachments) throws IOException {
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            final Object v = entry.getValue();
            if (!ENABLE_ATTACHMENT_WRAP) {
                if (v instanceof String) {
                    trailers.addObject(key, v);
                }
            } else {
                if (v instanceof String || serializeType == null) {
                    trailers.addObject(key, v);
                } else {
                    String encoded = TripleUtil.encodeWrapper(url, v, this.serializeType, getMultipleSerialization());
                    trailers.add(key + "-tw-bin", encoded);
                }
            }
        }
    }
}
