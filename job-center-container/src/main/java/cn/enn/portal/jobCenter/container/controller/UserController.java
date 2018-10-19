package cn.enn.portal.jobCenter.container.controller;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.filter.LoginFilter;
import cn.enn.portal.jobCenter.container.viewmodel.LoggedInUserModel;
import cn.enn.portal.jobCenter.container.viewmodel.UserPostModel;
import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.entity.UserTokenEntity;
import cn.enn.portal.jobCenter.core.service.UserService;
import cn.enn.portal.jobCenter.core.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {

    Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserTokenService userTokenService;

    @PostMapping("/login")
    public LoggedInUserModel login(UserPostModel userPostModel, HttpSession session, HttpServletRequest request) throws ContainerException {
        UserEntity userEntity = userService.checkLogin(userPostModel.getUserName(), userPostModel.getUserPassword());
        if (userEntity == null) {
            if (userService.existUsername(userPostModel.getUserName())) {
                throw new ContainerException("password incorrect", HttpStatus.BAD_REQUEST);
            } else {
                throw new ContainerException("username not found", HttpStatus.BAD_REQUEST);
            }
        } else {
            // update last login time to now
            userService.updateLastLoginTime(userEntity.getId());
            // put userEntity to session
            session.setAttribute("user", userEntity);
            // generate token
            UserTokenEntity userTokenEntity = userTokenService.generateToken(userEntity, LoginFilter.getClientIp(request));
            logger.debug("generate token success:{}", userTokenEntity.getToken());
            userEntity.setUserPassword(null);
            return new LoggedInUserModel(userEntity, userTokenEntity.getToken());
        }
    }

    @GetMapping("/info")
    public UserEntity getUserInfo(HttpSession session) throws ContainerException {
        if (session.getAttribute("user") == null) {
            throw new ContainerException("user not login", HttpStatus.BAD_REQUEST);
        }
        return (UserEntity) session.getAttribute("user");
    }

    @PostMapping("/logout")
    public void logout(HttpSession session, HttpServletRequest request) {
        if (session.getAttribute("user") != null) {
            session.removeAttribute("user");
        }
        String token = request.getHeader("authentication_token");
        if (token != null) {
            userTokenService.deleteToken(token);
        }
    }


    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/unauthorized")
    public void unauthorized() throws ContainerException {
        throw new ContainerException("need login", HttpStatus.UNAUTHORIZED);
    }

}
