package cn.enn.portal.jobCenter.core.repository;

import cn.enn.portal.jobCenter.core.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    UserEntity getByUserNameAndUserPassword(String userName, String userPassword);

    boolean existsByUserName(String userName);
}
