package org.apache.dubbo.test;

import com.alibaba.pelican.chaos.client.impl.RemoteCmdClient;
import com.alibaba.pelican.deployment.junit.AbstractJUnit4PelicanTests;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

/**
 * @author moyun@middleware
 */

@Slf4j
public class DubboLoadBalanceTest extends AbstractJUnit4PelicanTests {

    private String ip;

    private String userName;

    private String password;

    {
        Map<String, String> params = this.getTestProject().getVariables();
        ip = params.get("ip");
        userName = params.get("userName");
        password = params.get("password");
    }

    @Test
    public void test() throws Exception {

        for (int i = 0, length = 20; i < length; i++) {
            Response response = given()
                    .get(String.format("http://%s:8085/sayHello?name=123123", ip));
            TimeUnit.SECONDS.sleep(1);
            response.print();
        }

        RemoteCmdClient client = new RemoteCmdClient(ip, userName, password);
        client.killProcess("dubbo-provider-1.0-SNAPSHOT");

        log.info("kill dubbo-provider");

        for (int i = 0, length = 20; i < length; i++) {
            Response response = given()
                    .get(String.format("http://%s:8085/sayHello?name=123123", ip));
            TimeUnit.SECONDS.sleep(1);
            response.print();
        }

    }

}
