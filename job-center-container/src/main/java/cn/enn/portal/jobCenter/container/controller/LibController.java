package cn.enn.portal.jobCenter.container.controller;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.exception.FieldRequiredException;
import cn.enn.portal.jobCenter.container.exception.UnauthorizedException;
import cn.enn.portal.jobCenter.container.viewmodel.StringList;
import cn.enn.portal.jobCenter.container.viewmodel.UserLib;
import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.service.HdfsJarService;
import cn.enn.portal.jobCenter.core.service.ProjectService;
import org.apache.hadoop.fs.FileStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{project_id}/libs")
public class LibController {

    @Autowired
    HdfsJarService hdfsJarService;

    @Autowired
    ProjectService projectService;

    @GetMapping("")
    public Page<UserLib> getLibsOfProject(@PathVariable int project_id,
                                          @RequestParam(required = false, defaultValue = "1") int page,
                                          @RequestParam(required = false, defaultValue = "") String kw,
                                          HttpSession session) throws IOException, UnauthorizedException {
        if (!projectService.checkAuthorization(project_id, session)) {
            throw new UnauthorizedException();
        }
        int pageSize = 10;
        FileStatus[] fileStatusList = hdfsJarService.listProjectJars(project_id);
        if (fileStatusList == null) {
            return createPage(new ArrayList<UserLib>(), page - 1, pageSize, Sort.Direction.DESC, "createTime");
        } else {
            List<UserLib> fileList = Arrays.stream(fileStatusList)
                    .sorted((f1, f2) -> f1.getModificationTime() > f2.getModificationTime() ? -1 : 1)
                    .filter(s -> kw.isEmpty() || s.getPath().getName().contains(kw))
                    .map(f -> new UserLib(f.getPath().getName(), new Date(f.getModificationTime())))
                    .collect(Collectors.toList());
            return createPage(fileList, page - 1, pageSize, Sort.Direction.DESC, "createTime");
        }

    }

    private <T> Page<T> createPage(List<T> files, int pageNumber, int pageSize, Sort.Direction direction, String... sortProperty) {
        int skip = pageNumber * pageSize;
        int take = files.size() - skip >= pageSize ? pageSize : files.size() - skip;
        return new Page<T>() {
            @Override
            public int getTotalPages() {
//                if (files.length == 0)
//                    return 1;
                return files.size() % pageSize == 0 ? files.size() / pageSize : files.size() / pageSize + 1;
            }

            @Override
            public long getTotalElements() {
                return files.size();
            }

            @Override
            public <U> Page<U> map(Function<? super T, ? extends U> function) {
                return null;
            }

            @Override
            public int getNumber() {
                return pageNumber;
            }

            @Override
            public int getSize() {
                return pageSize;
            }

            @Override
            public int getNumberOfElements() {
                return take > 0 ? take : 0;
            }

            @Override
            public List<T> getContent() {
                if (take > 0)
                    return files.subList(skip, skip + take);
                else
                    return new ArrayList<>();
            }

            @Override
            public boolean hasContent() {
                return take > 0;
            }

            @Override
            public Sort getSort() {
                return Sort.by(direction, sortProperty);
            }

            @Override
            public boolean isFirst() {
                return pageNumber == 0;
            }

            @Override
            public boolean isLast() {
                return this.getTotalPages() == pageNumber + 1;
            }

            @Override
            public boolean hasNext() {
                return !this.isLast();
            }

            @Override
            public boolean hasPrevious() {
                return pageNumber != 0;
            }

            @Override
            public Pageable nextPageable() {
                return null;
            }

            @Override
            public Pageable previousPageable() {
                return null;
            }

            @Override
            public Iterator<T> iterator() {
                return null;
            }
        };

    }

    @PostMapping("")
    public UserLib uploadLib(@RequestParam MultipartFile file, @PathVariable int project_id, HttpSession session) throws IOException, UnauthorizedException {
        if (!projectService.checkAuthorization(project_id, session)) {
            throw new UnauthorizedException();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String newName = file.getOriginalFilename();
        if (newName.endsWith(".jar")) {
            newName = sdf.format(new Date()) + "-" + file.getOriginalFilename();
        }
        hdfsJarService.putJar(project_id, file.getInputStream(), newName);
        return new UserLib(newName, new Date());
    }


    @DeleteMapping("")
    public List<String> deleteLibs(@PathVariable int project_id,
                                   StringList nameList,
                                   HttpSession session) throws ContainerException, IOException {
        if (nameList.getNameList() == null || nameList.getNameList().size() == 0) {
            throw new FieldRequiredException("nameList");
        }

        if (!projectService.checkAuthorization(project_id, session)) {
            throw new UnauthorizedException();
        }

        List<String> deletedFiles = new ArrayList<>();
        IOException exception = null;
        for (String name : nameList.getNameList()) {
            try {
                if (hdfsJarService.deleteJar(project_id, name)) {
                    deletedFiles.add(name);
                }
            } catch (IOException e) {
                exception = e;
            }
        }
        if (exception != null) {
            throw exception;
        }
        return deletedFiles;
    }

}
