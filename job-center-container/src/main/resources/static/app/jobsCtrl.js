//JOB管理控制器
function jobsCtrl($scope, $http, $state, $stateParams) {
	$scope.projectId = $stateParams.projectId;
	$scope.projectName = $stateParams.projectName;
	$scope.isDelProShow = false;
	$scope.isPagination = false;
	$scope.isDetailShow = false;
	//搜索相关
	$scope.queryName = '';
	$scope.states = [
		{ "id": "", "name": "全部" },
		{ "id": "RUNNING", "name": "运行中" },
		{ "id": "FUTURE_RUN", "name": "将要运行" },
		{ "id": "DONE", "name": "已停止" },
		{ "id": "FAILED", "name": "异常" }
	];
	$scope.selectedState = $scope.states[0].id;

	//分页控件options
	$scope.pager = {
		pages: 1,
		currentPage: 1,
		goPage: function (p) {
			$scope.getJobs(p);
		}
	};

	// 获取JOB列表
	$scope.getJobs = function (pageNo, callback) {
		$http.get(basicUrl + "/api/projects/" + $stateParams.projectId + "/jobs", { 'params': { 'page': pageNo, 'kw': $scope.queryName, 'status': $scope.selectedState } }).then(function (resp) {
			$scope.jobs = resp.data.content;
			$scope.isPagination = !Boolean(resp.data.content.length);
			$scope.pager.pages = resp.data.totalPages;
			if (callback != null && typeof callback == 'function') {
				callback(resp);
			}
		});
	}
	$scope.getJobs(1);
	// 获取子job
	$scope.getSubjobs = function (ev, id, index) {
		ev.preventDefault();
		$scope.jobs[index].isChildShow = !$scope.jobs[index].isChildShow;
		if (!$scope.jobs[index].subJobs) {
			$http.get(basicUrl + "/api/projects/" + $stateParams.projectId + "/jobs/subjobs/" + id).then(function (resp) {
				$scope.jobs[index].subJobs = resp.data;
			});
		}
	}
	// 获取JOB详情
	$scope.getJobDetail = function (id) {
		$http.get(basicUrl + "/api/projects/" + $stateParams.projectId + "/jobs/" + id).then(function (resp) {
			$scope.isDetailShow = true;
			$scope.jobDetails = resp.data;
		});
	}
	// 删除JOB
	$scope.deleteJobs = function (ev, id, status) {
		ev.preventDefault();
		if (status == "运行中" || status == "将要运行") {
			alert("该JOB正在运行中，不能直接删除！");
		} else {
			$scope.isDelProShow = true;
			$scope.deleteJobId = id;
		}
	}
	$scope.confirmDel = function () {
		$http.delete(basicUrl + "/api/projects/" + $stateParams.projectId + "/jobs/" + $scope.deleteJobId).then(function (resp) {
			$scope.isDelProShow = false;
			$scope.deleteJobId = "";
			$scope.getJobs($scope.pager.currentPage, function (resp) {
				if (resp.data.content.length == 0 && $scope.pager.currentPage > 1) {
					$scope.pager.currentPage--;
					$scope.getJobs($scope.pager.currentPage);
				}
			});
		});
	}
	// 编辑JOB
	sessionStorage.removeItem("jobObj"); //清除已存在的jobObj
	$scope.editJobs = function (ev, job) {
		ev.preventDefault();
		sessionStorage.setItem("jobObj", JSON.stringify(job));
		$state.go('app.editjob', { projectId: $scope.projectId, projectName: $scope.projectName });
	}
	//启动/停止JOB
	$scope.startStopJob = function (ev, job, index) {
		ev.preventDefault();
		if (job.status == "已停止") {
			if (!confirm('确定要启动[' + job.jobName + ']吗？')) {
				return;
			}
			$http({
				method: 'post',
				url: basicUrl + '/api/projects/' + $stateParams.projectId + '/jobs/' + job.jobId + '/start'
			}).success(function (resp) {
				$scope.jobs[index].status = "运行中";
			});
		} else {
			if (!confirm('确定要停止[' + job.jobName + ']吗？')) {
				return;
			}
			$http({
				method: 'post',
				url: basicUrl + '/api/projects/' + $stateParams.projectId + '/jobs/' + job.jobId + '/shutdown'
			}).success(function (resp) {
				$scope.jobs[index].status = "已停止";
			});
		}
	}
}