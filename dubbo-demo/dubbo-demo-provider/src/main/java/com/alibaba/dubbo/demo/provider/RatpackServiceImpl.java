//package com.alibaba.dubbo.demo.provider;
//
//import com.alibaba.dubbo.demo.RatpackService;
//import ratpack.func.Action;
//import ratpack.handling.Chain;
//import ratpack.handling.Context;
//import ratpack.handling.Handler;
//
///**
// * Created by wuyu on 2017/1/18.
// */
//public class RatpackServiceImpl implements RatpackService, Action<Chain> {
//
//    @Override
//    public void execute(Chain chain) throws Exception {
//        chain.get("/:message", new Handler() {
//            @Override
//            public void handle(Context ctx) throws Exception {
//                ctx.render("Hello " + ctx.getPathTokens().get("message"));
//            }
//        });
//    }
//
//}
