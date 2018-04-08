package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.config.RefConf;

/**
 * 动态reference服务
 * User: zhouzhipeng
 * Date: 2018/3/21:22:34
 */
public class DynamicReferenceService {

    private AnnotationBean annotationBean;

    private DynamicReferenceService() {
    }

    DynamicReferenceService(AnnotationBean annotationBean) {
        this.annotationBean = annotationBean;
    }

    public <T> T getDubboService(Class<T> interfaceClass) {
        return (T) annotationBean.referNew(new RefConf(), interfaceClass);
    }

    public <T> T getDubboService(Class<T> interfaceClass, RefConf referenceConf) {
        return (T) annotationBean.referNew(referenceConf, interfaceClass);
    }

}
