# Dubbo Spring Boot interceptor

`dubbo-spring-boot-interceptor` copy the header(dubbo-tag)/cookie(dubbo.tag) to dubbo . 




## Integrate with spring boot

### copy the header(dubbo-tag) to dubbo 
```
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DubboTagHeaderInterceptor()).addPathPatterns("/*").excludePathPatterns("/admin");
    }
}
```
### copy the cookie(dubbo.tag) to dubbo 
```
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DubboTagCookieInterceptor()).addPathPatterns("/*").excludePathPatterns("/admin");
    }
}
```

