/**
 * File Created at 2011-12-26
 * $Id$
 *
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.protocol.thrift.ext;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.protocol.thrift.ThriftCodec;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class MultiServiceProcessor implements TProcessor {

    private static final Logger logger = LoggerFactory.getLogger( MultiServiceProcessor.class );

    private ConcurrentMap<String, TProcessor> processorMap = new ConcurrentHashMap<String, TProcessor>();

    private TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

    public MultiServiceProcessor() {}

    public boolean process( TProtocol in, TProtocol out ) throws TException {

        short magic = in.readI16();

        if ( magic != ThriftCodec.MAGIC ) {
            logger.error(
                    new StringBuilder( 24 )
                            .append( "Unsupported magic " )
                            .append( magic ).toString() );
            return false;
        }

        in.readI32();
        in.readI16();
        byte version = in.readByte();
        String serviceName = in.readString();
        long id = in.readI64();

        ByteArrayOutputStream bos = new ByteArrayOutputStream( 1024 );

        TIOStreamTransport transport = new TIOStreamTransport( bos );

        TProtocol protocol = protocolFactory.getProtocol( transport );

        TProcessor processor = processorMap.get( serviceName );

        if ( processor == null ) {
            logger.error(
                    new StringBuilder( 32 )
                            .append( "Could not find processor for service " )
                            .append( serviceName )
                            .toString() );
            return false;
        }

        // todo if exception
        boolean result = processor.process( in, protocol );

        ByteArrayOutputStream header = new ByteArrayOutputStream( 512 );

        TIOStreamTransport headerTransport = new TIOStreamTransport( header );

        TProtocol headerProtocol = protocolFactory.getProtocol( headerTransport );

        headerProtocol.writeI16( magic );
        headerProtocol.writeI32( Integer.MAX_VALUE );
        headerProtocol.writeI16( Short.MAX_VALUE );
        headerProtocol.writeByte( version );
        headerProtocol.writeString( serviceName );
        headerProtocol.writeI64( id );
        headerProtocol.getTransport().flush();

        out.writeI16( magic );
        out.writeI32( bos.size() + header.size() );
        out.writeI16( ( short ) ( 0xffff & header.size() ) );
        out.writeByte( version );
        out.writeString( serviceName );
        out.writeI64( id );

        out.getTransport().write( bos.toByteArray() );
        out.getTransport().flush();

        return result;

    }

    public TProcessor addProcessor( Class service, TProcessor processor ) {
        if ( service != null && processor != null ) {
            return processorMap.putIfAbsent( service.getName(), processor );
        }
        return processor;
    }

    public void setProtocolFactory( TProtocolFactory factory ) {
        if ( factory != null ) {
            this.protocolFactory = factory;
        }
    }

}
