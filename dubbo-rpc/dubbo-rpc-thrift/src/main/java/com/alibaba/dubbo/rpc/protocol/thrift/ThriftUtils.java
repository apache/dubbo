/**
 * File Created at 2011-12-05
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
package com.alibaba.dubbo.rpc.protocol.thrift;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">gang.lvg</a>
 */
public class ThriftUtils {

    /**
     * Generate class name which represents service arguments.
     *
     * @param serviceName service name
     * @param methodName method name
     * @return method args class name or null
     */
    public static String generateMethodArgsClassName( String serviceName, String methodName ) {

        int index = serviceName.lastIndexOf( "." );

        if ( index > 0 ) {

            return new StringBuilder( 32 )
                    .append( serviceName.substring( 0, index + 1 ) )
                    .append( "$__" )
                    .append( serviceName.substring( index + 1 ) )
                    .append( "Stub$" )
                    .append( methodName )
                    .append( "_args" )
                    .toString();

        } else {
            return new StringBuffer( 32 )
                    .append( "$__" )
                    .append( serviceName )
                    .append( "Stub$" )
                    .append( methodName )
                    .append( "_args" )
                    .toString();
        }

    }

    public static String generateMethodResultClassName( String serviceName, String method ) {

        int index = serviceName.lastIndexOf( "." );

        if ( index > 0 ) {

            return new StringBuilder( 32 )
                    .append( serviceName.substring( 0, index + 1 ) )
                    .append( "$__" )
                    .append( serviceName.substring( index + 1 ) )
                    .append( "Stub$" )
                    .append( method )
                    .append( "_result" )
                    .toString();

        } else {
            return new StringBuilder( 32 )
                    .append( "$__" )
                    .append( serviceName )
                    .append( "Stub$" )
                    .append( method )
                    .append( "_result" )
                    .toString();
        }

    }

    public static String generateSetMethodName( String fieldName ) {

        return new StringBuilder( 16 )
                .append( "set" )
                .append( Character.toUpperCase( fieldName.charAt( 0 ) ) )
                .append( fieldName.substring( 1 ) )
                .toString();

    }

    public static String generateGetMethodName( String fieldName ) {
        return new StringBuffer( 16 )
                .append( "get" )
                .append( Character.toUpperCase( fieldName.charAt( 0 ) ) )
                .append( fieldName.substring( 1 ) )
                .toString();
    }

    public static String generateMethodArgsClassNameThrift( String serviceName, String methodName ) {

        int index = serviceName.indexOf( "$" );

        if ( index > 0 ) {
            return new StringBuilder( 32 )
                    .append( serviceName.substring( 0, index + 1 ) )
                    .append( methodName )
                    .append( "_args" )
                    .toString();
        }

        return null;

    }

    public static String generateMethodResultClassNameThrift( String serviceName, String methodName ) {

        int index = serviceName.indexOf( "$" );

        if ( index > 0 ) {
            return new StringBuilder( 32 )
                    .append( serviceName.substring( 0, index + 1 ) )
                    .append( methodName )
                    .append( "_result" )
                    .toString();
        }

        return null;

    }

}
