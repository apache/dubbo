package org.apache.dubbo.remoting.transport.smartsocket;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/26
 */
public class DecoderException extends RuntimeException {

    public DecoderException(String message) {
        super(message);
    }


    public DecoderException(Throwable cause) {
        super(cause);
    }

}
