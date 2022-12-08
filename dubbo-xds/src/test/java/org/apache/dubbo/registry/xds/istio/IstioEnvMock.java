package org.apache.dubbo.registry.xds.istio;

public class IstioEnvMock {

    private static final IstioEnvMock INSTANCE = new IstioEnvMock();

    private String podName;

    private String caAddr;

    private String jwtPolicy;

    private String trustDomain;

    private String workloadNameSpace;

    private int rasKeySize;

    private String eccSigAlg;

    private int secretTTL;

    private float secretGracePeriodRatio;

    private String istioMetaClusterId;

    private String pilotCertProvider;

    public IstioEnvMock() {

    }

    public static IstioEnvMock getInstance() {
        return INSTANCE;
    }

    public String getPodName() {
        return podName;
    }

    public String getWorkloadNameSpace() {
        return workloadNameSpace;
    }
    public String getIstioMetaClusterId() {
        return istioMetaClusterId;
    }
}
