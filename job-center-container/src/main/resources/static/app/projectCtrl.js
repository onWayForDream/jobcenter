//项目管理控制器
function projectCtrl($scope, $http) {
	$scope.isAddProShow = false;
	$scope.isEditProShow = false;
	$scope.isDelProShow = false;
	$scope.isPagination = false;
	$scope.queryName = '';
	//获取项目
	$scope.getProgects = function (pageNo, callback) {
		$http.get(basicUrl + "/api/projects", { 'params': { 'page': pageNo, 'kw': $scope.queryName } }).then(function (resp) {
			$scope.projects = resp.data.content;
			$scope.isPagination = !Boolean(resp.data.content.length);
			$scope.pager.pages = resp.data.totalPages;
			if (callback != null && typeof callback == 'function') {
				callback(resp);
			}
		});
	}
	$scope.getProgects(1);
	//分页控件options
	$scope.pager = {
		pages: 1,
		currentPage: 1,
		goPage: function (p) {
			$scope.getProgects(p);
		}
	};
	//新增项目
	$scope.addProjectss = { "name": "" };
	$scope.addProject = function () {
		if (!$scope.addProjectss.name) {
			$scope.errorMsg = "请输入项目名称";
			return;
		}
		if (!$scope.addProjectss.name.match(/^[\u4E00-\u9FA5a-zA-Z0-9_]{3,20}$/)) {
			$scope.errorMsg = "项目名称不得超过20个字符（项目名称只允许包含汉字、字母、数字及下划线）";
			return;
		}
		$scope.errorMsg = '';
		$http({
			method: 'post',
			url: basicUrl + '/api/projects',
			data: $scope.addProjectss,
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			transformRequest: function (obj) {
				var str = [];
				for (var p in obj) {
					str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
				}
				return str.join("&");
			}
		}).success(function (resp) {
			$scope.getProgects(1);
			$scope.isAddProShow = false;
			$scope.addProjectss.name = '';
		});
	}
	//编辑项目
	$scope.putProjectss = { id: '', name: '' };
	$scope.editProject = function (ev, id, name) {
		ev.preventDefault();
		$scope.putProjectss.id = id;
		$scope.putProjectss.name = name;
		$scope.isEditProShow = true;
	}
	$scope.submitPutProject = function () {
		$http({
			method: 'put',
			url: basicUrl + '/api/projects',
			data: $scope.putProjectss,
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			transformRequest: function (obj) {
				var str = [];
				for (var p in obj) {
					str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
				}
				return str.join("&");
			}
		}).success(function (resp) {
			$scope.getProgects($scope.pager.currentPage);
			$scope.isEditProShow = false;
		});
	}
	//删除项目
	$scope.deleteProject = function (ev, id) {
		ev.preventDefault();
		$http.get(basicUrl + "/api/projects/" + id + "/libs", { 'params': { 'page': 1, 'kw': '' } }).then(function (resp) {
			var conLen = resp.data.content.length;
			if (conLen == 0) {
				$scope.isDelProShow = true;
				$scope.deleteProjectId = id;
			} else {
				alert("该项目下有库文件存在，不能直接删除！")
			}
		});
	}
	$scope.confirmDel = function () {
		$http.delete(basicUrl + "/api/projects/" + $scope.deleteProjectId).then(function (resp) {
			$scope.isDelProShow = false;
			$scope.deleteProjectId = '';
			$scope.getProgects($scope.pager.currentPage, function (resp) {
				if (resp.data.content.length == 0 && $scope.pager.currentPage > 1) {
					$scope.pager.currentPage--;
					$scope.getProgects($scope.pager.currentPage);
				}
			});
		});
	}
}