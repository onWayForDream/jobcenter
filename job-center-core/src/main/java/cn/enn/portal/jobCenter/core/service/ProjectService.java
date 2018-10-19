package cn.enn.portal.jobCenter.core.service;

import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProjectService {

    @Autowired
    UserService userService;

    @Autowired
    ProjectRepository projectRepository;

    public boolean checkAuthorization(ProjectEntity project, UserEntity user) {
        return userService.isAdministratorOrOwner(user, project.getOwner());
    }

    public boolean checkAuthorization(int projectId, UserEntity user) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("projectId " + projectId + " not found"));
        return checkAuthorization(project, user);
    }

    public boolean checkAuthorization(ProjectEntity project, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        return checkAuthorization(project, user);
    }

    public boolean checkAuthorization(int projectId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        return checkAuthorization(projectId, user);
    }



}
