//package com.alibaba.dubbo.rpc.protocol.ratpack;
//
//import org.springframework.core.annotation.AnnotationAwareOrderComparator;
//import ratpack.file.FileHandlerSpec;
//import ratpack.func.Action;
//import ratpack.handling.Chain;
//import ratpack.handling.Handler;
//import ratpack.handling.Handlers;
//import ratpack.server.ServerConfig;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
///**
// * Created by wuyu on 2017/1/18.
// */
//public class ChainConfigurers implements Action<Chain> {
//
//    private List<Action<Chain>> delegates = new CopyOnWriteArrayList<>();
//
//    private List<Handler> handlers = new CopyOnWriteArrayList<>();
//
//    @Override
//    public void execute(Chain chain) throws Exception {
//        for (Action<Chain> delegate : delegates) {
//            delegate.execute(chain);
//        }
//    }
//
//    public void addAction(Object obj) {
//        delegates.add((Action<Chain>) obj);
//    }
//
//    public void removeAction(Object obj) {
//        for (Action<Chain> action : delegates) {
//            if (action.getClass().isAssignableFrom(obj.getClass())) {
//                delegates.remove(action);
//            }
//        }
//    }
//
//
//    private Action<Chain> staticResourcesAction(final ServerConfig config) {
//        return new Action<Chain>() {
//            @Override
//            public void execute(Chain chain) throws Exception {
//                Handlers.files(config, new Action<FileHandlerSpec>() {
//                    @Override
//                    public void execute(FileHandlerSpec fileHandlerSpec) throws Exception {
//                        fileHandlerSpec.dir("static").indexFiles("index.html");
//                        fileHandlerSpec.dir("public").indexFiles("index.html");
//                    }
//                });
//            }
//        };
//    }
//
//    private Action<Chain> singleHandlerAction() {
//        return new Action<Chain>() {
//            @Override
//            public void execute(Chain chain) throws Exception {
//                if (handlers.size() == 1) {
//                    chain.get(handlers.get(0));
//                }
//            }
//        };
//    }
//}
