package org.apache.dubbo.metrics.prometheus;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Optional;

@Cmd(name = "metrics", summary = "online report")
public class PrometheusMetricsReporterCmd implements BaseCommand {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(PrometheusMetricsReporterCmd.class);

    public FrameworkModel frameworkModel;

    public PrometheusMetricsReporterCmd(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (args == null || args.length == 0) {
            ApplicationModel applicationModel = ApplicationModel.defaultModel();
            return getResponseByApplication(applicationModel);
        } else {
            String appName = args[0];
            Optional<ApplicationModel> modelOptional = frameworkModel.getApplicationModels().stream().filter(applicationModel -> appName.equals(applicationModel.getApplicationName())).findFirst();
            if (modelOptional.isPresent()) {
                return getResponseByApplication(modelOptional.get());
            } else {
                return "Not exist application: " + appName;
            }
        }
    }


    private String getResponseByApplication(ApplicationModel applicationModel) {

        String response = "MetricsReporter not init";
        MetricsReporter metricsReporter = applicationModel.getBeanFactory().getBean(MetricsReporter.class);
        if (metricsReporter != null) {
            long begin = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("scrape begin");
            }

            metricsReporter.refreshData();

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("scrape end,Elapsed Time：%s", System.currentTimeMillis() - begin));
            }
            response = metricsReporter.getResponse();

        }
        return response;
    }
}
