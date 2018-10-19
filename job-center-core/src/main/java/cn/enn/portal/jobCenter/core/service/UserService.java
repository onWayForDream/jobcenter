package cn.enn.portal.jobCenter.core.service;

import cn.enn.portal.jobCenter.core.UserRole;
import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.repository.UserRepository;
import cn.enn.portal.jobCenter.core.util.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.util.Date;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public UserEntity checkLogin(String userName, String password) {
        String passwordMD5 = Md5Util.md5(password);
        return userRepository.getByUserNameAndUserPassword(userName, passwordMD5);
    }

    public boolean existUsername(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Transactional
    public void updateLastLoginTime(int userId) {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("userId " + userId + " not found"));
        userEntity.setLastLoginTime(new Date());
        userRepository.save(userEntity);
    }

    public boolean isAdministrator(UserEntity user) {
        return user.getRoleName() != null && user.getRoleName().equals(UserRole.ROLE_ADMIN.toString());
    }

    public boolean isAdministrator(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        return isAdministrator(user);
    }

    public boolean isAdministratorOrOwner(UserEntity user, String owner) {
        if (isAdministrator(user)) {
            return true;
        }
        if (owner != null && !owner.isEmpty() && owner.equals(user.getUserName())) {
            return true;
        }
        return false;
    }

}
