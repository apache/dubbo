package org.apache.dubbo.xds.auth;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.istio.IstioConstant;

public class MtlsService2 extends AuthTest{

    public static void main(String[] args) throws InterruptedException {
        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo";
        FrameworkModel f2 = new FrameworkModel();
        newService(f2,new DemoServiceImpl2(), DemoService2.class,10087);

        DemoService demoService = newRef(f2, DemoService.class);

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
