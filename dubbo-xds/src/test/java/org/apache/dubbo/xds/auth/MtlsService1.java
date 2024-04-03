package org.apache.dubbo.xds.auth;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.xds.istio.IstioConstant;

public class MtlsService1 extends AuthTest{

    public static void main(String[] args) {
        System.setProperty("API_SERVER_PATH","https://127.0.0.1:6443");
        System.setProperty("SA_CA_PATH","/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        System.setProperty("SA_TOKEN_PATH","/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar");
        System.setProperty("NAMESPACE","bar");
        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar";

        FrameworkModel f1 = new FrameworkModel();
        ApplicationModel applicationModel = f1.newApplication();
//        KubeEnv kubeEnv = new KubeEnv(applicationModel);
//
//        kubeEnv.setNamespace("foo");
//        kubeEnv.setEnableSsl(true);
//        kubeEnv.setApiServerPath( "https://127.0.0.1:6443");
//        kubeEnv.setServiceAccountTokenPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar");
//        kubeEnv.setServiceAccountCaPath("/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
//        applicationModel.getBeanFactory().registerBean(kubeEnv);

        newService(applicationModel,new DemoServiceImpl(),DemoService.class,10086);
        DemoService2 demoService2 = newRef(applicationModel, DemoService2.class);

        while (true) {
            try {
                RpcContext.getClientAttachment().setAttachment("s1","attachment from service1");
                System.out.println(demoService2.sayHello("service1 to service2"));
                Thread.sleep(1000L);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
