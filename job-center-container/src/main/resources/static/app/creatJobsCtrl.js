//新建JOB
function creatJobsCtrl($scope, $http, $state, $stateParams) {
	$scope.projectId = $stateParams.projectId;
	$scope.projectName = $stateParams.projectName;
	//间隔时间单位
	$scope.cycles = [
		{ "id": 1000, "name": "秒" },
		{ "id": 1000 * 60, "name": "分" },
		{ "id": 1000 * 60 * 60, "name": "时" },
		{ "id": 1000 * 60 * 60 * 24, "name": "天" }
	];
	$scope.selectedCycle = $scope.cycles[0].id;
	var myDate = new Date(); //获取系统当前时间
	$scope.nowTime = formatDateTime(myDate, "yyyy-mm-dd HH:mm:ss");
	$scope.postModel = {
		"jobName": "",
		"executeClass": "",
		"jobParamJson": "",
		"propertiesFile": "",
		"profileName": "",
		"concurrentOpt": "JustRunIt",
		"scheduleType": "MANUAL",
		"scheduleValue": "",
		"validityFrom": "",
		"validityTo": ""
	};
	$scope.creatNewJob = function () {
		if (!$scope.postModel.jobName) {
			alert("请填写JOB名称");
			document.getElementById('jobname').focus();
			return;
		}
		if (!$scope.postModel.executeClass) {
			if ($scope.selectedRuntimeIndex == 0)
				alert("请填写运行类名");
			else if ($scope.selectedRuntimeIndex == 1) {
				alert("请填写启动脚本和参数");
			}
			document.getElementById('executeclass').focus();
			return;
		}
		if ($scope.selectedRuntimeIndex == 1) {
			$scope.postModel.jobParamJson = null;
			$scope.postModel.propertiesFile = null;
			$scope.postModel.profileName = null;
		}
//		if ($scope.postModel.jobParamJson) {
//			try {
//				JSON.parse($scope.postModel.jobParamJson);
//			} catch (e) {
//				alert("参数信息必须为JSON格式");
//				document.getElementById('jobparam').focus();
//				return false;
//			}
//		}
		if ($scope.postModel.propertiesFile) {
			var regex = /\.properties$/;
			if (!regex.test($scope.postModel.propertiesFile)) {
				alert("properties文件名格式错误!");
				return false;
			}
		}
		switch ($scope.postModel.scheduleType) {
			case "MANUAL":
				$scope.postModel.scheduleValue = "";
				$scope.postModel.validityFrom = $scope.startTimeA;
				$scope.postModel.validityTo = ""
				break;
			case "INTERVAL":
				var re = /^[0-9]+$/;
				if (!$scope.intervalTime) {
					alert("请设置间隔时间");
					document.getElementById('intervaltime').focus();
					return;
				}
				if (!re.test($scope.intervalTime)) {
					alert("间隔时间必须为正整数");
					document.getElementById('intervaltime').focus();
					return;
				}
				$scope.postModel.scheduleValue = $scope.intervalTime * $scope.selectedCycle;
				if ($scope.postModel.scheduleValue < 5000) {
					alert("间隔时间不能小于5秒");
					document.getElementById('intervaltime').focus();
					return;
				}
				$scope.postModel.validityFrom = $scope.startTimeB;
				$scope.postModel.validityTo = $scope.endTimeB;
				break;
			case "CRON_EXPRESSION":
				if (!$scope.cronValue) {
					alert("请填写cron表达式");
					document.getElementById('cronvalue').focus();
					return;
				}
				$scope.postModel.scheduleValue = $scope.cronValue;
				$scope.postModel.validityFrom = $scope.startTimeC;
				$scope.postModel.validityTo = $scope.endTimeC;
				break;
			default:
				break;
		}
		$scope.postModel.runtimeType = $scope.runtimeTypes[$scope.selectedRuntimeIndex].value;
		$http({
			method: 'post',
			url: basicUrl + '/api/projects/' + $stateParams.projectId + '/jobs',
			data: $scope.postModel
		}).success(function (resp) {
			$state.go('app.jobs', { projectId: $scope.projectId, projectName: $scope.projectName });
		});
	}

	$scope.runtimeTypes = [
		{ name: 'java job', value: 'JVM', },
		{ name: "spring-boot job", value: "SpringBoot" },
		{ name: 'linux shell job', value: 'LinuxShell' }
		// { name: 'http request job', value: 'HttpRequest', }
	]
	$scope.selectedRuntimeIndex = 0;

	$scope.changeRuntime = function (index) {
		$scope.selectedRuntimeIndex = index;
	}


}