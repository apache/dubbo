package org.apache.dubbo.xds.listener;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.FilterChainMatch;
import io.envoyproxy.envoy.config.listener.v3.Listener;

@Activate
public class TlsModeListener implements LdsListener {

    protected static final String TLS = "tls";

    protected static final String LDS_VIRTUAL_INBOUND = "virtualInbound";

    private final TlsModeRepo repo;

    public TlsModeListener(ApplicationModel applicationModel){
        this.repo = applicationModel.getBeanFactory().getOrRegisterBean(TlsModeRepo.class);
    }

    @Override
    public void onResourceUpdate(List<Listener> listeners) {
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        Map<String, TlsType> indicatorToTls = new HashMap<>(1);
        for (Listener listener : listeners) {
            List<FilterChain> filterChains = listener.getFilterChainsList();
            if (!LDS_VIRTUAL_INBOUND.equals(listener.getName())) {
                continue;
            }
            if (CollectionUtils.isEmpty(filterChains)) {
                continue;
            }
            for (FilterChain filterChain : filterChains) {
                if (!LDS_VIRTUAL_INBOUND.equals(filterChain.getName())) {
                    continue;
                }
                FilterChainMatch match = filterChain.getFilterChainMatch();
                int port = match.getDestinationPort().getValue();
//                String applicationProtocolIndicator = Strings.join(match.getApplicationProtocolsList(),":");

                TlsType newTlsType = TlsType.DISABLE;
                if (TLS.equals(match.getTransportProtocol())) {
                    newTlsType = TlsType.STRICT;
                }
                //PERMISSIVE mode resolves both plaintext and tls
                if (indicatorToTls.containsKey(port)) {
                    newTlsType = TlsType.PERMISSIVE;
                }
                indicatorToTls.put(String.valueOf(port), newTlsType);
            }
        }

        TlsType globalSetting = indicatorToTls.get("0");
        repo.setGlobalConfig(globalSetting);
        repo.update(indicatorToTls);
    }


    public enum TlsType {
        STRICT(0, "Strict Mode"),
        PERMISSIVE(1, "Permissive Mode"),
        DISABLE(2, "Disable Mode"),
        ;
        public static Map<Integer, TlsType> map = new HashMap<>();

        static {
            for (TlsType tlsEnum : TlsType.values()) {
                map.put(tlsEnum.code, tlsEnum);
            }
        }

        private int code;
        private String msg;

        TlsType(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static TlsType getFromCode(int code) {
            return map.get(code);
        }

        @Override
        public String toString() {
            return "TlsType{" +
                    "code=" + code +
                    ", msg='" + msg + '\'' +
                    '}';
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
