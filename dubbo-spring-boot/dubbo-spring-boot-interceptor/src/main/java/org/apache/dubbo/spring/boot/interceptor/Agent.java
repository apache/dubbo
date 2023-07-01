public class Agent {
private static Agent instance = new Agent();
    private Logger logger = LoggerFactory.getLogger(Agent.class);
    private Agent() {}
    public static Agent getInstance(){ return instance; }
    public void install() {
        ByteBuddyAgent.install();
//        AgentBuilder.Listener listener = new AgentBuilder.Listener() {
// do nothing
//            ...
//        };

//        new AgentBuilder.Default()
//                .type(ElementMatchers.nameStartsWith("klordy.learning"))
//                .transform((builder, typeDescription, classLoader, module) ->
//                        builder.visit(Advice.to(TimeAdvice.class).on(ElementMatchers.isAnnotatedWith(named("org.apache.dubbo.spring.boot.annotation.DubboTagCrossThread")))))
//                .with(listener)
//                // *** added as supposed, but still seems not work.
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                .installOnByteBuddyAgent();
        new AgentBuilder.Default()
            .disableClassFormatChanges()
            .with(RETRANSFORMATION)
            .with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
            .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
            .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
            .type(ElementMatchers.nameStartsWith("org.apache.dubbo"))// TODO expose with config
            .transform((builder, typeDescription, classLoader, module) ->
                builder.visit(
                    Advice
                        .to(TimeAdvice.class)
                        .on(isAnnotatedWith(named("org.apache.dubbo.spring.boot.annotation.DubboTagCrossThread")))
                )
            )
            .installOnByteBuddyAgent();
         logger.info("byte buddy modification done.");
     }
}
