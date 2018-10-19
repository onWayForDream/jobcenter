//编辑JOB
function editJobsCtrl($scope, $http, $state, $stateParams) {
	console.log("load");
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
	$scope.jobObj = sessionStorage.getItem('jobObj');
	if ($scope.jobObj) {
		$scope.editJob = JSON.parse($scope.jobObj);
		$scope.editPostModel = {
			"id": 0,
			"jobName": "",
			"executeClass": "",
			"jobParamJson": "",
			"propertiesFile": "",
			"profileName": "",
			"concurrentOpt": "",
			"scheduleType": "",
			"scheduleValue": "",
			"validityFrom": "",
			"validityTo": ""
		};
		$scope.editPostModel.id = $scope.editJob.jobId;
		$scope.editPostModel.jobName = $scope.editJob.jobName;
		$scope.editPostModel.executeClass = $scope.editJob.executeClass;
		$scope.editPostModel.jobParamJson = $scope.editJob.jobParamJson;
		$scope.editPostModel.propertiesFile = $scope.editJob.propertiesFile;
		$scope.editPostModel.profileName = $scope.editJob.profileName;
		$scope.editPostModel.concurrentOpt = $scope.editJob.concurrentOpt;
		$scope.editPostModel.scheduleType = $scope.editJob.scheduleType;
		$scope.editPostModel.scheduleValue = $scope.editJob.scheduleValue;
		$scope.editPostModel.validityFrom = $scope.editJob.validityFrom;
		$scope.editPostModel.validityTo = $scope.editJob.validityTo;
		$scope.editPostModel.runtimeType = $scope.editJob.runtimeType;
		switch ($scope.editPostModel.scheduleType) {
			case "MANUAL":
				$scope.editStartTimeA = $scope.editJob.validityFrom;
				break;
			case "INTERVAL":
				$scope.editStartTimeB = $scope.editJob.validityFrom;
				$scope.editEndTimeB = $scope.editJob.validityTo;
				$scope.editIntervalTime = $scope.editJob.scheduleValue / 1000;
				break;
			case "CRON_EXPRESSION":
				$scope.editStartTimeC = $scope.editJob.validityFrom;
				$scope.editEndTimeC = $scope.editJob.validityTo;
				$scope.editCronValue = $scope.editJob.scheduleValue;
				break;
			default:
				break;
		};
		// 提交
		$scope.submitEditJob = function () {
			if (!$scope.editPostModel.jobName) {
				alert("请填写JOB名称");
				document.getElementById('jobname').focus();
				return;
			}
			if (!$scope.editPostModel.executeClass) {
				if ($scope.selectedRuntimeIndex == 0)
					alert("请填写运行类名");
				else if ($scope.selectedRuntimeIndex == 1) {
					alert("请填写启动脚本和参数");
				}
				document.getElementById('executeclass').focus();
				return;
			}
			if ($scope.selectedRuntimeIndex == 1) {
				$scope.editPostModel.jobParamJson = null;
				$scope.editPostModel.propertiesFile = null;
				$scope.editPostModel.profileName = null;
			}
//			if ($scope.editPostModel.jobParamJson) {
//				try {
//					JSON.parse($scope.editPostModel.jobParamJson);
//				} catch (e) {
//					alert("参数信息必须为JSON格式");
//					document.getElementById('jobparam').focus();
//					return false;
//				}
//			}
			if ($scope.editPostModel.propertiesFile) {
				var regex = /\.properties$/;
				if (!regex.test($scope.editPostModel.propertiesFile)) {
					alert("properties文件名格式错误!");
					return false;
				}
			}
			switch ($scope.editPostModel.scheduleType) {
				case "MANUAL":
					$scope.editPostModel.scheduleValue = "";
					$scope.editPostModel.validityFrom = $scope.editStartTimeA;
					$scope.editPostModel.validityTo = ""
					break;
				case "INTERVAL":
					var re = /^[0-9]+$/;
					if (!$scope.editIntervalTime) {
						alert("请设置间隔时间");
						document.getElementById('intervaltime').focus();
						return;
					}
					if (!re.test($scope.editIntervalTime)) {
						alert("间隔时间必须为正整数");
						document.getElementById('intervaltime').focus();
						return;
					}
					$scope.editPostModel.scheduleValue = $scope.editIntervalTime * $scope.selectedCycle;
					if ($scope.editPostModel.scheduleValue < 5000) {
						alert("间隔时间不能小于5秒");
						document.getElementById('intervaltime').focus();
						return;
					}
					$scope.editPostModel.validityFrom = $scope.editStartTimeB;
					$scope.editPostModel.validityTo = $scope.editEndTimeB;
					break;
				case "CRON_EXPRESSION":
					if (!$scope.editCronValue) {
						alert("请填写cron表达式");
						document.getElementById('cronvalue').focus();
						return;
					}
					$scope.editPostModel.scheduleValue = $scope.editCronValue;
					$scope.editPostModel.validityFrom = $scope.editStartTimeC;
					$scope.editPostModel.validityTo = $scope.editEndTimeC;
					break;
				default:
					break;
			};
			$scope.editPostModel.runtimeType = $scope.runtimeTypes[$scope.selectedRuntimeIndex].value;

			debugger;
			if ($scope.editJob.status != '已停止') {
				if (confirm('[' + $scope.editJob.jobName + ']正在运行，必须停止后才能更新内容，是否现在停止？')) {
					$http({
						method: 'post',
						url: basicUrl + '/api/projects/' + $scope.projectId + '/jobs/' + $scope.editJob.jobId + '/shutdown'
					}).success(function (resp) {
						updateJob($scope.projectId, $scope.projectName);
					});
				}
			} else {
				updateJob($scope.projectId, $scope.projectName);
			}
		}
	}

	function updateJob(projectId, projectName) {
		$http({
			method: 'put',
			url: basicUrl + '/api/projects/' + projectId + '/jobs',
			data: $scope.editPostModel
		}).success(function (resp) {
			$state.go('app.jobs', { projectId: projectId, projectName: projectName });
			sessionStorage.removeItem("jobObj");
		});
	}

	$scope.runtimeTypes = [
		{ name: 'java job', value: 'JVM', },
		{ name: "spring-boot job", value: "SpringBoot" },
		{ name: 'linux shell job', value: 'LinuxShell' }
		// { name: 'http request job', value: 'HttpRequest', } 
	]

	function findRuntimeIndex(runtimeType) {
		for (var i = 0; i < $scope.runtimeTypes.length; i++) {
			if ($scope.runtimeTypes[i].value == runtimeType) {
				return i;
			}
		}
		return 0;
	}

	$scope.selectedRuntimeIndex = findRuntimeIndex($scope.editPostModel.runtimeType);

	$scope.changeRuntime = function (index) {
		$scope.selectedRuntimeIndex = index;
	}

}