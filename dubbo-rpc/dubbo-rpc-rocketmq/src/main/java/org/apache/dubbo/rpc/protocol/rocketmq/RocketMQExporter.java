package org.apache.dubbo.rpc.protocol.rocketmq;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.rpc.protocol.DelegateExporterMap;

public class RocketMQExporter<T> extends AbstractExporter<T> {

	private final String key;

	private final DelegateExporterMap delegateExporterMap;

	public RocketMQExporter(Invoker<T> invoker,String key, DelegateExporterMap delegateExporterMap) {
		super(invoker);
		this.key = key;
		this.delegateExporterMap = delegateExporterMap;
	}

	public void afterUnExport() {
		delegateExporterMap.removeExportMap(key, this);
	}

}
