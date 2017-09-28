一、新建一个benchmark工程，如demo.benchmark
二、导入自己服务的接口api包和dubbo.benchmark.jar(解压dubbo.benchmark.tar.gz，在lib目录下)
三、新建一个类，实现AbstractClientRunnable
    a、实现父类的构造函数
    b、实现invoke方法，通过serviceFactory创建本地接口代理，并实现自己的业务逻辑，如下
    public Object invoke(ServiceFactory serviceFactory) {
        DemoService demoService = (DemoService) serviceFactory.get(DemoService.class);
        return demoService.sendRequest("hello");
    }
四、将自己的benchmark工程打成jar包,如demo.benchmark.jar
五、将demo.benchmark.jar放到dubbo.benchmark/lib目录下
六、配置dubbo.properties
七、运行run.bat(windows)或run.sh(linux)