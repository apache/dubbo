package com.alibaba.dubbo.governance.web.personal.module.screen;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.UserService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.User;

public class Passwds extends Restful {

    @Autowired
    private UserService userDAO;

    public void index(Map<String, Object> context) {

    }

    public boolean create(Map<String, Object> context) {
        User user = new User();
        user.setOperator(operator);
        user.setOperatorAddress(operatorAddress);
        user.setPassword((String) context.get("newPassword"));
        user.setUsername(operator);

        boolean sucess = userDAO.updatePassword(user, (String) context.get("oldPassword"));
        if (!sucess)
            context.put("message", getMessage("passwd.oldwrong"));
        return sucess;
    }

}
