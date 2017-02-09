package com.alibaba.dubbo.rpc.protocol.redis2;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.IOUtils;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuyu on 2017/2/8.
 */
public class Redis2Connection extends Connection {

    public static final byte ASTERISK_BYTE = '*';

    public static final byte DOLLAR_BYTE = '$';

    private static final byte[][] EMPTY_ARGS = new byte[0][];

    private String host = Protocol.DEFAULT_HOST;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private int pipelinedCommands = 0;
    private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;
    private boolean broken = false;
    private boolean ssl;
    private SSLSocketFactory sslSocketFactory;
    private SSLParameters sslParameters;
    private HostnameVerifier hostnameVerifier;

    public Redis2Connection() {
    }

    public Redis2Connection(final String host) {
        this.host = host;
    }

    public Redis2Connection(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public Redis2Connection(final String host, final int port, final int connectionTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
    }

    public Redis2Connection(final String host, final int port, final int connectionTimeout, int soTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
    }

    public Redis2Connection(final String host, final int port, final boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    public Redis2Connection(final String host, final int port, final boolean ssl,
                            SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
                            HostnameVerifier hostnameVerifier) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.hostnameVerifier = hostnameVerifier;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public void setTimeoutInfinite() {
        try {
            if (!isConnected()) {
                connect();
            }
            socket.setSoTimeout(0);
        } catch (SocketException ex) {
            broken = true;
            throw new JedisConnectionException(ex);
        }
    }

    public void rollbackTimeout() {
        try {
            socket.setSoTimeout(soTimeout);
        } catch (SocketException ex) {
            broken = true;
            throw new JedisConnectionException(ex);
        }
    }

    protected Connection sendCommand(final Protocol.Command cmd, final String... args) {
        final byte[][] bargs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            bargs[i] = SafeEncoder.encode(args[i]);
        }
        return sendCommand(cmd, bargs);
    }

    protected Connection sendCommand(final Protocol.Command cmd) {
        return sendCommand(cmd, EMPTY_ARGS);
    }

    public Connection sendCommand(String command, final String... args) {
        final byte[][] bargs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            bargs[i] = SafeEncoder.encode(args[i]);
        }
        return sendCommand(SafeEncoder.encode(command), bargs);
    }

    public Connection sendCommand(byte[] command, final byte[]... args) {
        try {
            connect();
            sendCommand(outputStream, command, args);
            pipelinedCommands++;
            return this;
        } catch (JedisConnectionException ex) {
      /*
       * When client send request which formed by invalid protocol, Redis send back error message
       * before close connection. We try to read it to provide reason of failure.
       */
            try {
                String errorMessage = Protocol.readErrorLineIfPossible(inputStream);
                if (errorMessage != null && errorMessage.length() > 0) {
                    ex = new JedisConnectionException(errorMessage, ex.getCause());
                }
            } catch (Exception e) {
        /*
         * Catch any IOException or JedisConnectionException occurred from InputStream#read and just
         * ignore. This approach is safe because reading error message is optional and connection
         * will eventually be closed.
         */
            }
            // Any other exceptions related to connection?
            broken = true;
            throw ex;
        }
    }

    protected Connection sendCommand(final Protocol.Command cmd, final byte[]... args) {
        try {
            connect();
            Protocol.sendCommand(outputStream, cmd, args);
            pipelinedCommands++;
            return this;
        } catch (JedisConnectionException ex) {
      /*
       * When client send request which formed by invalid protocol, Redis send back error message
       * before close connection. We try to read it to provide reason of failure.
       */
            try {
                String errorMessage = Protocol.readErrorLineIfPossible(inputStream);
                if (errorMessage != null && errorMessage.length() > 0) {
                    ex = new JedisConnectionException(errorMessage, ex.getCause());
                }
            } catch (Exception e) {
        /*
         * Catch any IOException or JedisConnectionException occurred from InputStream#read and just
         * ignore. This approach is safe because reading error message is optional and connection
         * will eventually be closed.
         */
            }
            // Any other exceptions related to connection?
            broken = true;
            throw ex;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void connect() {
        if (!isConnected()) {
            try {
                socket = new Socket();
                // ->@wjw_add
                socket.setReuseAddress(true);
                socket.setKeepAlive(true); // Will monitor the TCP connection is
                // valid
                socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to
                // ensure timely delivery of data
                socket.setSoLinger(true, 0); // Control calls close () method,
                // the underlying socket is closed
                // immediately
                // <-@wjw_add

                socket.connect(new InetSocketAddress(host, port), connectionTimeout);
                socket.setSoTimeout(soTimeout);

                if (ssl) {
                    if (null == sslSocketFactory) {
                        sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    }
                    socket = (SSLSocket) sslSocketFactory.createSocket(socket, host, port, true);
                    if (null != sslParameters) {
                        ((SSLSocket) socket).setSSLParameters(sslParameters);
                    }
                    if ((null != hostnameVerifier) &&
                            (!hostnameVerifier.verify(host, ((SSLSocket) socket).getSession()))) {
                        String message = String.format(
                                "The connection to '%s' failed ssl/tls hostname verification.", host);
                        throw new JedisConnectionException(message);
                    }
                }

                outputStream = new RedisOutputStream(socket.getOutputStream());
                inputStream = new RedisInputStream(socket.getInputStream());
            } catch (IOException ex) {
                broken = true;
                throw new JedisConnectionException(ex);
            }
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                outputStream.flush();
                socket.close();
            } catch (IOException ex) {
                broken = true;
                throw new JedisConnectionException(ex);
            } finally {
                IOUtils.closeQuietly(socket);
            }
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isBound() && !socket.isClosed() && socket.isConnected()
                && !socket.isInputShutdown() && !socket.isOutputShutdown();
    }

    public String getStatusCodeReply() {
        flush();
        pipelinedCommands--;
        final byte[] resp = (byte[]) readProtocolWithCheckingBroken();
        if (null == resp) {
            return null;
        } else {
            return SafeEncoder.encode(resp);
        }
    }

    public String getBulkReply() {
        final byte[] result = getBinaryBulkReply();
        if (null != result) {
            return SafeEncoder.encode(result);
        } else {
            return null;
        }
    }

    public byte[] getBinaryBulkReply() {
        flush();
        pipelinedCommands--;
        return (byte[]) readProtocolWithCheckingBroken();
    }

    public Long getIntegerReply() {
        flush();
        pipelinedCommands--;
        return (Long) readProtocolWithCheckingBroken();
    }

    public List<String> getMultiBulkReply() {
        return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
    }

    @SuppressWarnings("unchecked")
    public List<byte[]> getBinaryMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return (List<byte[]>) readProtocolWithCheckingBroken();
    }

    public void resetPipelinedCount() {
        pipelinedCommands = 0;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getRawObjectMultiBulkReply() {
        return (List<Object>) readProtocolWithCheckingBroken();
    }

    public List<Object> getObjectMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return getRawObjectMultiBulkReply();
    }

    @SuppressWarnings("unchecked")
    public List<Long> getIntegerMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return (List<Long>) readProtocolWithCheckingBroken();
    }

    public List<Object> getAll() {
        return getAll(0);
    }

    public List<Object> getAll(int except) {
        List<Object> all = new ArrayList<Object>();
        flush();
        while (pipelinedCommands > except) {
            try {
                all.add(readProtocolWithCheckingBroken());
            } catch (JedisDataException e) {
                all.add(e);
            }
            pipelinedCommands--;
        }
        return all;
    }

    public Object getOne() {
        flush();
        pipelinedCommands--;
        return readProtocolWithCheckingBroken();
    }

    public boolean isBroken() {
        return broken;
    }

    protected void flush() {
        try {
            outputStream.flush();
        } catch (IOException ex) {
            broken = true;
            throw new JedisConnectionException(ex);
        }
    }

    protected Object readProtocolWithCheckingBroken() {
        try {
            return Protocol.read(inputStream);
        } catch (JedisConnectionException exc) {
            broken = true;
            throw exc;
        }
    }


    private void sendCommand(final RedisOutputStream os, final byte[] command,
                             final byte[]... args) {
        try {
            os.write(ASTERISK_BYTE);
            os.writeIntCrLf(args.length + 1);
            os.write(DOLLAR_BYTE);
            os.writeIntCrLf(command.length);
            os.write(command);
            os.writeCrLf();

            for (final byte[] arg : args) {
                os.write(DOLLAR_BYTE);
                os.writeIntCrLf(arg.length);
                os.write(arg);
                os.writeCrLf();
            }
        } catch (IOException e) {
            throw new JedisConnectionException(e);
        }
    }
}
