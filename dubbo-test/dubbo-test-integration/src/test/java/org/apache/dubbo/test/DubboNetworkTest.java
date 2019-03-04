package org.apache.dubbo.test;

import com.alibaba.pelican.chaos.client.impl.RemoteCmdClient;
import com.alibaba.pelican.chaos.client.utils.NetAccessUtils;
import com.alibaba.pelican.deployment.junit.AbstractJUnit4PelicanTests;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * @author moyun@middleware
 */

@Slf4j
public class DubboNetworkTest extends AbstractJUnit4PelicanTests {

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

        RemoteCmdClient client = new RemoteCmdClient(ip, userName, password);
        NetAccessUtils.blockPortProtocol(client, "8085", "TCP", 20);

        try {
            Response response = given()
                    .get(String.format("http://%s:8085/sayHello?name=123123", ip));
            response.print();
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            if (message.contains("Operation timed out (Connection timed out)")) {
                log.info("Operation timed out (Connection timed out)");
            }
            NetAccessUtils.clearAll(client);
        }
        Response response = given()
                .get(String.format("http://%s:8085/sayHello?name=123123", ip));
        response.print();

    }

}
