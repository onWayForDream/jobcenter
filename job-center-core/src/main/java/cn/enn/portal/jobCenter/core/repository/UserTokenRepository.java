package cn.enn.portal.jobCenter.core.repository;

import cn.enn.portal.jobCenter.core.entity.UserTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface UserTokenRepository extends JpaRepository<UserTokenEntity, Integer> {

    @Query(nativeQuery = true, value = "select * from user_token where token = ?1 and bind_ip = ?2 and expire >= now() limit 1")
    UserTokenEntity checkToken(String token, String bindIp);

    @Transactional
    int deleteByToken(String token);
}
