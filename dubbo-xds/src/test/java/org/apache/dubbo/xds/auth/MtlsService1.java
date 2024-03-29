package org.apache.dubbo.xds.auth;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.istio.IstioConstant;

public class MtlsService1 extends AuthTest{

    public static void main(String[] args) {
        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar";
        FrameworkModel f1 = new FrameworkModel();
        newService(f1,new DemoServiceImpl(),DemoService.class,10086);
        DemoService2 demoService2 = newRef(f1, DemoService2.class);

        while (true) {
            try {
                System.out.println(demoService2.sayHello("service1 to service2"));
                Thread.sleep(1000L);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
