package org.apache.dubbo.rpc.cluster.adaptive;

import org.apache.dubbo.rpc.Invoker;

public class Node {
    private volatile int remain;
    private Invoker invoker;
    private String address;

    public Node(Invoker invoker){
        this();
        this.invoker = invoker;
        this.address = invoker.getUrl().getBackupAddress();
    }

    public Node(){
        this.remain = 1;
    }

    public String getAddress(){
        return this.address;
    }

    public int getRemain(){
        return this.remain;
    }

    public boolean isValid(){
        return true;
    }

    public Invoker getInvoker(){
        return this.invoker;
    }

    public void updateState(int remain,long rtt){
        this.remain = remain;
    }
}
