package org.apache.dubbo.xds.auth;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.istio.IstioConstant;

public class MtlsService2 extends AuthTest{

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("API_SERVER_PATH","https://127.0.0.1:6443");
        System.setProperty("SA_CA_PATH","/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        System.setProperty("SA_TOKEN_PATH","/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");
        System.setProperty("NAMESPACE","foo");
        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo";

        FrameworkModel f2 = new FrameworkModel();
        ApplicationModel applicationModel = f2.newApplication();

        newService(applicationModel,new DemoServiceImpl2(), DemoService2.class,10087);

        DemoService demoService = newRef(applicationModel, DemoService.class);

        while (true) {
            try {
                System.out.println(demoService.sayHello("service2 to service1"));
                Thread.sleep(1000L);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
