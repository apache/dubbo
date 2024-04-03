package org.apache.dubbo.xds.listener;

import org.apache.dubbo.xds.listener.TlsModeListener.TlsType;

import java.util.Map;

public class TlsModeRepo {

    public TlsModeRepo(){};

    private Map<String, TlsType> connectionType;

    private TlsType globalConfig;

    public void update(Map<String,TlsType> connectionType){
        this.connectionType = connectionType;
    }

    public void setGlobalConfig(TlsType tlsType){
        this.globalConfig = tlsType;
    }

    public TlsType getType(String indicator){
        if(globalConfig != null){
            return globalConfig;
        }else {
            return connectionType.get(indicator);
        }
    }
}
