package org.apache.dubbo.metrics.report;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Cmd(name = "metrics_default", summary = "reuse qos report")
public class DefaultMetricsReporterCmd implements BaseCommand {
    
    public FrameworkModel frameworkModel;
    
    public DefaultMetricsReporterCmd(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }
    
    @Override
    public String execute(CommandContext commandContext, String[] args) {
        List<ApplicationModel> models = frameworkModel.getApplicationModels();
        String result = "There is no application with data";
        if (notSpecifyApplication(args)) {
            result = useFirst(models, result,null);
        } else if (args.length == 1) {
            result = specifyApplication(args[0], models, null);
        } else if (args.length == 2) {
            result = specifyApplication(args[0], models, args[1]);
        }
        return result;
    }
    
    private boolean notSpecifyApplication(String[] args) {
        return args == null || args.length == 0;
    }
    
    private String useFirst(List<ApplicationModel> models, String result, String metricsName) {
        for (ApplicationModel model : models) {
            String current = getResponseByApplication(model, metricsName);
            if (getLineNumber(current) > 0) {
                result = current;
                break;
            }
        }
        return result;
    }
    
    private String specifyApplication(String appName, List<ApplicationModel> models, String metricsName) {
        if ("application_all".equals(appName)) {
            return allApplication(models);
        } else {
            return specifySingleApplication(appName, models,metricsName);
        }
    }
    
    private String specifySingleApplication(String appName, List<ApplicationModel> models, String metricsName) {
        Optional<ApplicationModel> modelOptional = models.stream()
                .filter(applicationModel -> appName.equals(applicationModel.getApplicationName())).findFirst();
        if (modelOptional.isPresent()) {
            return getResponseByApplication(modelOptional.get(),metricsName);
        } else {
            return "Not exist application: " + appName;
        }
    }
    
    private String allApplication(List<ApplicationModel> models) {
        Map<String, String> appResultMap = new HashMap<>();
        for (ApplicationModel model : models) {
            appResultMap.put(model.getApplicationName(), getResponseByApplication(model, null));
        }
        return JsonUtils.toJson(appResultMap);
    }
    
    private String getResponseByApplication(ApplicationModel applicationModel, String metricsName) {
        String response = "MetricsReporter not init";
        MetricsReporter metricsReporter = applicationModel.getBeanFactory().getBean(MetricsReporter.class);
        if (metricsReporter != null) {
            metricsReporter.refreshData();
            response = metricsReporter.getResponseWithName(metricsName);
        }
        return response;
    }
    
    
    private static long getLineNumber(String content) {
        LineNumberReader lnr = new LineNumberReader(new CharArrayReader(content.toCharArray()));
        try {
            lnr.skip(Long.MAX_VALUE);
            lnr.close();
        } catch (IOException ignore) {
        }
        return lnr.getLineNumber();
    }
}
