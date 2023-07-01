public class DubboTagListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private Logger logger = LoggerFactory.getLogger(AppEnvListener.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
        Agent.getInstance().install();
        logger.info("finished byte buddy installation.");
    }
}
