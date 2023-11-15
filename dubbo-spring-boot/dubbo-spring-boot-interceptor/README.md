# Dubbo Spring Boot interceptor

`dubbo-spring-boot-interceptor` copy the header(dubbo-tag)/cookie(dubbo.tag)/urlParameter(dubbo.tag) to dubbo . 




## Integrate with spring boot

### copy the header(dubbo-tag=tagx)/urlParameter(dubbo.tag=tagx) to dubbo
if there is no header(dubbo-tag) , downgrade use urlParameter(dubbo.tag)
```
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DubboTagHeaderOrParameterInterceptor()).addPathPatterns("/*").excludePathPatterns("/admin");
    }
}
```
### copy the cookie(dubbo.tag=tagx) to dubbo 
```
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DubboTagCookieInterceptor()).addPathPatterns("/*").excludePathPatterns("/admin");
    }
}
```

