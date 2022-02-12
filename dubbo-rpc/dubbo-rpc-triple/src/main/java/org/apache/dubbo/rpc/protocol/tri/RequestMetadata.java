package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;

import io.netty.util.AsciiString;

import java.util.List;
import java.util.Map;

public class RequestMetadata {
    public long requestId;
    public AsciiString scheme;
    public String application;
    public String service;
    public String version;
    public String group;
    public String address;
    public String acceptEncoding;
    public String timeout;
    public Compressor compressor;
    public MethodDescriptor method;
    public Object[] arguments;
    public List<String> argumentTypes;
    public Map<String, Object> attachments;
    public GenericPack genericPack;
    public GenericUnpack genericUnpack;
}
