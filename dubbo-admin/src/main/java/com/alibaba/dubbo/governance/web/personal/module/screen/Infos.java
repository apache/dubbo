package com.alibaba.dubbo.governance.web.personal.module.screen;

import com.alibaba.dubbo.governance.service.UserService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.User;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class Infos extends Restful {
    @Autowired
    private UserService userDAO;

    public void index(Map<String, Object> context) {
        User user = userDAO.findById(currentUser.getId());
        context.put("user", user);
    }

    public boolean update(Map<String, Object> context) {
        User user = new User();
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());
        user.setOperatorAddress(operatorAddress);
        user.setName((String) context.get("name"));
        user.setDepartment((String) context.get("department"));
        user.setEmail((String) context.get("email"));
        user.setPhone((String) context.get("phone"));
        user.setAlitalk((String) context.get("alitalk"));
        user.setLocale((String) context.get("locale"));
        userDAO.modifyUser(user);
        context.put("redirect", "../" + getClass().getSimpleName().toLowerCase());
        return true;
    }
}
