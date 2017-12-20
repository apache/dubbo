1、create a benchmark project,like demo.benchmark
2、import your service interface jar and dubbo.benchmark.jar(unzip dubbo.benchmark.tar.gz，in lib folder)
3、create a new class，implement AbstractClientRunnable
    a、implement the parent class constructor
    b、implement invoke method，create local interface proxy by serviceFactory,and implement your own business，eg：
    public Object invoke(ServiceFactory serviceFactory) {
        DemoService demoService = (DemoService) serviceFactory.get(DemoService.class);
        return demoService.sendRequest("hello");
    }
4、package your benchmark project to jar,like demo.benchmark.jar
5、put demo.benchmark.jar into dubbo.benchmark/lib folder
6、config dubbo.properties
7、run run.bat(windows) or run.sh(linux)