package org.apache.dubbo.rpc.protocol.tri.support;

public class IGreeterException extends Exception {
        //异常信息
        private String message;

        //构造函数
        public IGreeterException(String message){
            super(message);
            this.message = message;
        }
}
