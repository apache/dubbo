package org.apache.dubbo.metadata.report;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class MetadataReportFactory$Adaptive implements org.apache.dubbo.metadata.report.MetadataReportFactory {
public org.apache.dubbo.metadata.report.MetadataReport getMetadataReport(org.apache.dubbo.common.URL arg0)  {
if (arg0 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg0;
String extName = ( url.getProtocol() == null ? "redis" : url.getProtocol() );
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.metadata.report.MetadataReportFactory) name from url (" + url.toString() + ") use keys([protocol])");
org.apache.dubbo.metadata.report.MetadataReportFactory extension = (org.apache.dubbo.metadata.report.MetadataReportFactory)ExtensionLoader.getExtensionLoader(org.apache.dubbo.metadata.report.MetadataReportFactory.class).getExtension(extName);
return extension.getMetadataReport(arg0);
}
}