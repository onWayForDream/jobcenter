package cn.enn.portal.jobCenter;

import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.service.UserService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestLogin {

    @Autowired
    UserService userService;

    @Test
    public void testLogin() {
        UserEntity userEntity = userService.checkLogin("admin", "admin");
        Assert.assertTrue(userEntity != null);
        System.out.println(userEntity.getUserName());
    }
}
