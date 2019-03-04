## 具体介绍
Dubbo-example，是基于PelicanDT实现dubbo环境准备，禁止端口网络访问，执行接口调用验证端口是否禁用示例
    
## 前期准备
1. 本示例程序是基于阿里云ECS或远程Linux服务器完成，只需[购买](https://ecs-buy.aliyun.com/wizard?spm=5176.8789780.1092585.1.520157a8WqaKjA#/prepay/cn-zhangjiakou)阿里云机器，或者选定已准备好的远程服务器即可
2. 下载[Dubbo-example](https://github.com/alibaba/PelicanDT/tree/master/Dubbo-example)代码

## 快速入门

### 修改配置
1. 打开dubbo.properties配置文件，具体路径：Dubbo-example/src/test/resources/env/func/dubbo.properties
2. 填写ip，userName，password

### 运行示例

本地代码控制远程服务器执行Dubbo验证：
1. 打开TestDubboNetwork.java，具体路径：Dubbo-example/src/test/java/com/alibaba/pelican/rocketmq/TestDubboNetwork.java
2. 运行单元测试

### 预期结果
日志输出内容如下
    
    2019-02-01 17:20:30 [INFO] [main] c.a.p.c.client.utils.NetAccessUtils - Block port 8085 protcol TCP, dalay time 20 seconds.
    2019-02-01 17:20:45 [INFO] [main] c.a.p.rocketmq.TestDubboNetwork - Operation timed out (Connection timed out)
    Hello 123123, response form provider: 10.66.204.25:20880
    
- 通过第1行日志可以看出，8085端口断网
- 通过第2行日志可以看出，在8085端口断网的情况下，接口访问超时
- 通过第3行日志可以看出，端口网络恢复后，接口访问成功