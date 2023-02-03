package org.apache.dubbo.metrics.filter;

import org.apache.dubbo.common.extension.Activate;

import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;


@Activate(group =CONSUMER, order = -1)
public class MetricsFilterConsumer implements Filter, BaseFilter.Listener, ScopeModelAware {

    DefaultMetricsCollector collector=null;
    ApplicationModel applicationModel=null;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        collector=applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (collector == null || !collector.isCollectEnabled()) {
            return invoker.invoke(invocation);
        }
        collect(invocation, MetricsCollectExecutor::beforeExecute);

        return invoker.invoke(invocation);
    }

    private void collect(Invocation invocation, Consumer<MetricsCollectExecutor> execute) {
        if (collector == null || !collector.isCollectEnabled()) {
            return;
        }
        MetricsCollectExecutor collectorExecutor = new MetricsCollectExecutor(applicationModel.getApplicationName(),collector, invocation);
        execute.accept(collectorExecutor);
    }

    @Override
    public void onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        collect(invocation, collector->collector.postExecute(result));
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        collect(invocation, collector-> collector.throwExecute(t));
    }
}
