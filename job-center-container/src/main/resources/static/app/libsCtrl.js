//库管理控制器
function libsCtrl($scope, $http, $stateParams, FileUploader) {
    $scope.projectId = $stateParams.projectId;
    $scope.projectName = $stateParams.projectName;
    $scope.isUploadShow = false;
    $scope.isDelProShow = false;
    $scope.isPagination = false;
    $scope.isUploadComplete = false;
    $scope.queryName = '';
    $scope.isUploadState = 0;
    //获取库列表
    $scope.getLibs = function (pageNo, callback) {
        $http.get(basicUrl + "/api/projects/" + $stateParams.projectId + "/libs", { 'params': { 'page': pageNo, 'kw': $scope.queryName } }).then(function (resp) {
            $scope.libs = resp.data.content;
            $scope.isPagination = !Boolean(resp.data.content.length);
            $scope.pager.pages = resp.data.totalPages;
            if (callback != null && typeof callback == 'function') {
                callback(resp);
            }
        });
    }
    $scope.getLibs(1);
    //分页控件options
    $scope.pager = {
        pages: 1,
        currentPage: 1,
        goPage: function (p) {
            $scope.getLibs(p);
        }
    };
    //删除库
    $scope.delLibName = [];
    $scope.deleteLibs = function (ev, name) {
        ev.preventDefault();
        $scope.isDelProShow = true;
        $scope.delLibName.push(name);
    }
    $scope.confirmDel = function () {
        $http.delete(basicUrl + "/api/projects/" + $stateParams.projectId + "/libs", { 'params': { 'nameList': $scope.delLibName } }).then(function (resp) {
            $scope.isDelProShow = false;
            $scope.select_all = false;
            $scope.select_one = false;
            $scope.delLibName = [];
            $scope.getLibs($scope.pager.currentPage, function (resp) {
                if (resp.data.content.length == 0 && $scope.pager.currentPage > 1) {
                    $scope.pager.currentPage--;
                    $scope.getLibs($scope.pager.currentPage);
                }
            });
        });
    }
    //批量删除
    $scope.m = {};
    $scope.checked = [];
    $scope.selectAll = function () {
        if ($scope.select_all) {
            $scope.select_one = true;
            $scope.checked = [];
            angular.forEach($scope.libs, function (i, index) {
                $scope.checked.push(i.name);
                $scope.m[i.name] = true;
            })
        } else {
            $scope.select_one = false;
            $scope.checked = [];
            $scope.m = {};
        }
        $scope.delLibName = $scope.checked;
    };
    $scope.selectOne = function (select) {
        angular.forEach($scope.m, function (i, name) {
            var index = $scope.checked.indexOf(name);
            if (i && index === -1) {
                $scope.checked.push(name);
            } else if (!i && index !== -1) {
                $scope.checked.splice(index, 1);
            };
        });
        if ($scope.libs.length === $scope.checked.length) {
            $scope.select_all = true;
        } else {
            $scope.select_all = false;
        }
        $scope.delLibName = $scope.checked;
    }
    //编辑上传
    $scope.editLibs = function () {
        $scope.isUploadShow = true;
    }
    //上传库
    var uploader = $scope.uploader = new FileUploader({
        url: basicUrl + '/api/projects/' + $stateParams.projectId + '/libs'
    });
    uploader.filters.push({ //过滤
        name: 'customFilter',
        fn: function (item /*{File|FileLikeObject}*/, options) {
            return this.queue.length < 10;
        }
    });
    uploader.onAfterAddingFile = function (fileItem) {
        $scope.isUploadState = 0;
        fileItem.state = "等待上传";
    };
    uploader.onBeforeUploadItem = function (item) {
        $scope.isUploadState = 1;
        item.state = "正在上传中…";
    };
    uploader.onSuccessItem = function (fileItem, response, status, headers) {
        $scope.isUploadState = 2;
        fileItem.state = "上传成功";
    };
    uploader.onErrorItem = function (fileItem, response, status, headers) {
        $scope.isUploadState = 3;
        fileItem.state = "上传失败";
    };
    uploader.onCompleteAll = function () {
        $scope.isUploadComplete = true;
        $scope.getLibs(1);
    };
}