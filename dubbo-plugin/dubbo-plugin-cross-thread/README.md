# Dubbo Plugin Cross Thread

`dubbo-plugin-cross-thread` copy dubbo-tag cross thread lightly . 

## Integrate example
### scan annotation by byte-buddy
(you can install with ByteBuddyAgent or use it with `-javaagent=<agentjar>`)
```
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, tag);
        Callable<String> callable = CallableWrapper.of(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return RpcContext.getClientAttachment().getAttachment(CommonConstants.TAG_KEY);
            }
        });
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        Future<String> result = threadPool.submit(callable);
```
### add annotation @DubboCrossThread

```
@DubboCrossThread
public class TargetClass implements Runnable{
    @Override
    public void run() {
        // ...
    }
}
```
### wrap Callable or Runnable
```
Callable<String> callable = CallableWrapper.of(new Callable<String>() {
    @Override
    public String call() throws Exception {
        return null;
    }
});
```
```
Runnable runnable = RunnableWrapper.of(new Runnable() {
    @Override
    public void run() {
        // ...
    }
});
```
## Integrate with spring boot

### make listener
```
public class DubboCrossThreadAnnotationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private Logger logger = LoggerFactory.getLogger(DubboCrossThreadAnnotationListener.class);
    private Instrumentation instrumentation;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
        RunnableOrCallableActivation.install(this.instrumentation);
        logger.info("finished byte buddy installation.");
    }

    public DubboCrossThreadAnnotationListener(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    private DubboCrossThreadAnnotationListener() {

    }
}

```
### install ByteBuddyAgent
```
@SpringBootApplication
@ComponentScan(basePackages = "org.apache.your-package")
public class SpringBootDemoApplication {

    public static void main(String[] args) {
       SpringApplication application = new SpringApplication(SpringBootDemoApplication.class);
       application.addListeners(new DubboCrossThreadAnnotationListener(ByteBuddyAgent.install()));
       application.run(args);
    }
}
```

