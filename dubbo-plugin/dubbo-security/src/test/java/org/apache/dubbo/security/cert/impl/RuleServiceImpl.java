package org.apache.dubbo.security.cert.impl;

import org.apache.dubbo.auth.v1alpha1.ObserveRequest;
import org.apache.dubbo.auth.v1alpha1.ObserveResponse;
import org.apache.dubbo.auth.v1alpha1.RuleServiceGrpc;

import io.grpc.stub.StreamObserver;

public class RuleServiceImpl extends RuleServiceGrpc.RuleServiceImplBase {
    @Override
    public StreamObserver<ObserveRequest> observe(StreamObserver<ObserveResponse> responseObserver) {
        return super.observe(responseObserver);
    }
}
