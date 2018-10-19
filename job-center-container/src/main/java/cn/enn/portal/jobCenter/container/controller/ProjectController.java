package cn.enn.portal.jobCenter.container.controller;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.exception.FieldRequiredException;
import cn.enn.portal.jobCenter.container.exception.UnauthorizedException;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import cn.enn.portal.jobCenter.core.service.HdfsJarService;
import cn.enn.portal.jobCenter.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

@RestController()
@RequestMapping("/api/projects")
public class ProjectController {

    Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    HdfsJarService hdfsJarService;

    @Autowired
    UserService userService;

    @GetMapping("")
    public Page<ProjectEntity> getProjects(@RequestParam(defaultValue = "1", required = false) int page,
                                           @RequestParam(defaultValue = "", required = false) String kw,
                                           HttpSession session) {
        int pageSize = 10;
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        UserEntity user = (UserEntity) session.getAttribute("user");

        Page<ProjectEntity> resultList = null;

        if (userService.isAdministrator(user)) {
            // ROLE_ADMIN can access all project, ignore the project owner
            if (kw.isEmpty()) {
                resultList = projectRepository.findAll(pageable);
            } else {
                resultList = projectRepository.findByNameContaining(kw, pageable);
            }
        } else {
            if (kw.isEmpty()) {
                resultList = projectRepository.findByOwner(user.getUserName(), pageable);
            } else {
                resultList = projectRepository.findByOwnerAndNameContaining(user.getUserName(), kw, pageable);
            }
        }
        return resultList;
    }


    @PostMapping("")
    public ProjectEntity newProject(ProjectEntity projectEntity, HttpSession session) throws FieldRequiredException, IOException {
        if (projectEntity.getName() == null || projectEntity.getName().isEmpty()) {
            throw new FieldRequiredException("name");
        }
        UserEntity user = (UserEntity) session.getAttribute("user");
        projectEntity.setCreateTime(new Date());
        projectEntity.setOwner(user.getUserName());
        projectRepository.save(projectEntity);
        hdfsJarService.createProjectFolder(projectEntity.getId());
        return projectEntity;
    }

    @PutMapping("")
    @Transactional
    public ProjectEntity updateProject(ProjectEntity projectEntity, HttpSession session) throws ContainerException {
        if (projectEntity.getId() <= 0) {
            throw new ContainerException("id must greater than 0", HttpStatus.BAD_REQUEST);
        }
        Optional<ProjectEntity> existEntity = projectRepository.findById(projectEntity.getId());
        if (!existEntity.isPresent()) {
            throw new ContainerException("project id " + projectEntity.getId() + " not found", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = (UserEntity) session.getAttribute("user");
        ProjectEntity _existOne = existEntity.get();
        if (userService.isAdministratorOrOwner(user, _existOne.getOwner())) {
            _existOne.setName(projectEntity.getName());
            projectRepository.save(_existOne);

            return _existOne;
        } else {
            throw new UnauthorizedException();
        }
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable int id, HttpSession session) throws ContainerException {
        if (!projectRepository.existsById(id)) {
            throw new ContainerException("project id not found", HttpStatus.NOT_FOUND);
        }
        ProjectEntity project = projectRepository.findById(id).get();
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (!userService.isAdministratorOrOwner(user, project.getOwner())) {
            throw new UnauthorizedException();
        }
        projectRepository.deleteById(id);
    }
}
