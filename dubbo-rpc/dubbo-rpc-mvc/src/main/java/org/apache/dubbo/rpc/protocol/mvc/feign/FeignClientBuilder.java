package org.apache.dubbo.rpc.protocol.mvc.feign;

import feign.Feign;
import feign.Target;
import org.apache.dubbo.rpc.protocol.mvc.feign.coder.FeignBaseDecoder;
import org.apache.dubbo.rpc.protocol.mvc.feign.coder.FeignBaseEncoder;
import org.apache.dubbo.rpc.protocol.mvc.feign.support.ResponseEntityDecoder;
import org.apache.dubbo.rpc.protocol.mvc.feign.support.SpringMvcContract;
import org.springframework.web.bind.annotation.RequestMapping;

import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

public class FeignClientBuilder<T> {

    public static <T> T createFeignClient(Class<T> service, String url) {


        String baseUrl = getBaseUrl(service);
        Target.HardCodedTarget<T> feignClient = new Target.HardCodedTarget(service, url + baseUrl);

        Feign build = Feign.builder().contract(new SpringMvcContract()).encoder(new FeignBaseEncoder())
            .decoder(new ResponseEntityDecoder(new FeignBaseDecoder()))
            .build();
        T restClientService = build.newInstance(feignClient);
        return restClientService;
    }

    private static <T> String getBaseUrl(Class<T> service) {
        String baseUrl = "/";
        RequestMapping classAnnotation = findMergedAnnotation(service,
            RequestMapping.class);
        if (classAnnotation == null) {
            return baseUrl;
        }

        // Prepend path from class annotation if specified
        if (classAnnotation.value().length > 0) {
            String pathValue = emptyToNull(classAnnotation.value()[0]);
            if (!pathValue.startsWith("/")) {
                pathValue = "/" + pathValue;
                baseUrl = pathValue;
            }

        }
        return baseUrl;
    }

}
