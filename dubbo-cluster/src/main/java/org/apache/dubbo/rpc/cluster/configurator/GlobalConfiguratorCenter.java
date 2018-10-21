package org.apache.dubbo.rpc.cluster.configurator;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.rpc.Invoker;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class GlobalConfiguratorCenter {
    private  static final ConcurrentHashMap<String,URL> configuratorMap= new ConcurrentHashMap();
    private  static   ZookeeperClient zkClient = null;

    public static void initZkClient( ZookeeperClient zookeeperClient){
        zkClient=zookeeperClient;
        zkClient.addChildListener( Constants.GLOBALCONFIG_ZK_PATH, new ChildListener()  {
            @Override
            public void childChanged(String path, List<String> children) {
                for (String configUrl: children) {
                        URL url = URL.valueOf( URL.decode(configUrl));
                        String mapKey=url.getAddress();
                        if(mapKey!=null&&!"".equals(mapKey)) configuratorMap.put(mapKey,url);
                }
            }
        });
    }



    public  static  int getInvokerWeight(Invoker<?> invoker, int deafultWeight) {
        try {

            if(invoker!=null&& invoker.getUrl()!=null){
                URL url =  configuratorMap.get(invoker.getUrl().getAddress());
                if(url!=null)deafultWeight =url.getParameter( Constants.WEIGHT_KEY, deafultWeight);
                return  deafultWeight;
            }
        }catch (Exception e){
            return  deafultWeight;
        }
        return  deafultWeight;
    }



}
