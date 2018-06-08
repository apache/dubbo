# Steps to use benchmark project

0. New a benchmark project, say: demo.benchmark
1. Import both service interface (your service API jar) and `dubbo.benchmark.jar` into the project
2. Create a new class to implement `AbstractClientRunnable`:
    * Override parent's constructor
    * Implement `invoke` method, use `ServiceFactory` to create local proxy for the interface, and then implement business logic as below:
    
    ```java
    public class MyClient implements AbstractClientRunnable {
       public Object invoke(ServiceFactory serviceFactory) {
        DemoService demoService = (DemoService) serviceFactory.get(DemoService.class);
        return demoService.sendRequest("hello");
       }
    }
```

3. build the project and package the result into a jar file, for example: `demo.benchmark.jar`
4. put `demo.benchmark.jar` under `dubbo.benchmark/lib`
5. config in `dubbo.proerties`
6. run `run.bat` on windows, or `run.sh` on unix-like platforms