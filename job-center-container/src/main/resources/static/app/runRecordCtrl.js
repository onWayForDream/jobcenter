//运行记录
function runRecordCtrl($scope, $http, $state, $stateParams) {
	$scope.projectId = $stateParams.projectId;
	$scope.jobId = $stateParams.jobId;
	$scope.projectName = $stateParams.projectName;
	$scope.isPagination = false;
	//分页控件options
	$scope.pager = {
		pages: 1,
		currentPage: 1,
		goPage: function (p) {
			$scope.getRunRecord(p);
		}
	};

	// 获取job name
	$http.get(basicUrl + "/api/projects/" + $stateParams.projectId + "/jobs/" + $stateParams.jobId).then(function (resp) {
		$scope.jobName = resp.data.jobName;
	})

	// 获取运行记录列表
	$scope.getRunRecord = function (pageNo, callback) {
		$http.get(basicUrl + "/api/projects/" + $stateParams.projectId + "/jobs/" + $stateParams.jobId + "/run_history", { 'params': { 'page': pageNo } }).then(function (resp) {
			$scope.runRecordList = resp.data.content;
			$scope.isPagination = !Boolean(resp.data.content.length);
			$scope.pager.pages = resp.data.totalPages;
			if (callback != null && typeof callback == 'function') {
				callback(resp);
			}
		});
	}
	$scope.getRunRecord(1);
	//查看日志
	$scope.lookLog = function (id) {
		var logUrl = basicUrl + "/api/projects/" + $scope.projectId + "/jobs/" + $scope.jobId + "/running_details/" + id;
		window.open(logUrl);
	}
}
