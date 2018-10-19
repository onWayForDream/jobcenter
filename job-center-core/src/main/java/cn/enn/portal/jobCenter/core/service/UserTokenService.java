package cn.enn.portal.jobCenter.core.service;

import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.entity.UserTokenEntity;
import cn.enn.portal.jobCenter.core.repository.UserRepository;
import cn.enn.portal.jobCenter.core.repository.UserTokenRepository;
import cn.enn.portal.jobCenter.core.util.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserTokenService {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserTokenEntity generateToken(UserEntity user, String remoteIp) {
        String token = Md5Util.md5(UUID.randomUUID().toString());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 7);
        UserTokenEntity userTokenEntity = new UserTokenEntity();
        userTokenEntity.setUserId(user.getId());
        userTokenEntity.setToken(token);
        userTokenEntity.setExpire(calendar.getTime());
        userTokenEntity.setBindIp(remoteIp);
        return userTokenRepository.save(userTokenEntity);
    }

    public UserEntity checkToken(String token, String remoteIp) {
        UserTokenEntity userTokenEntity = userTokenRepository.checkToken(token, remoteIp);
        if (userTokenEntity != null) {
            Optional<UserEntity> userEntityOptional = userRepository.findById(userTokenEntity.getUserId());
            if (userEntityOptional.isPresent()) {
                return userEntityOptional.get();
            }
        }
        return null;
    }

    public int deleteToken(String token) {
        return userTokenRepository.deleteByToken(token);
    }

}
