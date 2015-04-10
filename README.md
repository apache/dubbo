文档最新地址： http://dubbo.io

dubbo-sf原始源代码是阿里巴巴开源的项目dubbo-2.5.4-SNAPSHO版本拷贝的, 下载源码后可mvn clean source:jar install -Dmaven.test.skip进行本地编译最新版本

jar版本都发布在http://10.118.46.12:8081/nexus/content/groups/public/com/alibaba/dubbo/中，坐标只是在version元素的值前加入了sf.前缀，例如：
<groupId>com.alibaba</groupId>
<artifactId>dubbo</artifactId>
<version>sf.1.0.0</version>

下面是版本升级和修改记录
**********dubbo-sf-1.0.0**********
1. 升级到1.6编译
2. 将Spring2.5.6升级至Spring3.2.8版本
升级版本日期是2014-11-05，唐继模（709166）

**********dubbo-sf-1.0.1**********
修复创建AdaptiveExtensionClass时方法异常签名问题

**********dubbo-sf-1.0.2**********