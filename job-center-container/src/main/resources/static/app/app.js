//定义主模块并注入依赖
var app = angular.module('ruleApp', ['ui.router', 'angularFileUpload', 'ngAnimate']);

//jobCenter
//var basicUrl = "http://jobcenter-test-2:8080";
var basicUrl = "";

//dashboard
var dashBaseUrl = "http://10.39.48.85:8089";
//var dashBaseUrl = "";

app.controller('mainCtrl', function ($scope, $location, $http, $rootScope) {
	var currentType = sessionStorage.getItem('currentType');
	currentType ? $scope.currentType = currentType : $scope.currentType = 'project';
	$scope.changeTab = function (type) {
		$scope.currentType = type;
		sessionStorage.setItem("currentType", type);
	};

	$http.get(basicUrl + '/api/user/info').then(function (response) {
		$rootScope.username = response.data.userName;
	})

	$scope.logout = function () {
		if (confirm('确定要退出登录吗？')) {
			$http.post(basicUrl + '/api/user/logout', {}).then(function () {
				window.location.href = '/login.html';
			});
		}
	}

});


//路由配置
app.config(['$stateProvider', '$urlRouterProvider', function ($stateProvider, $urlRouterProvider) {
	$urlRouterProvider.otherwise('/app/project');
	$stateProvider
		.state('app', {
			url: '/app',
			abstract: true,
			templateUrl: "tmpl/app/viewport.html",
			controller: 'mainCtrl'
		})
		.state('app.project', {
			url: '/project',
			templateUrl: "tmpl/project/project.html",
			controller: projectCtrl
		})
		.state('app.libs', {
			url: '/libs/:projectId/:projectName',
			templateUrl: "tmpl/project/libs.html",
			controller: libsCtrl
		})
		.state('app.jobs', {
			url: '/jobs/:projectId/:projectName',
			templateUrl: "tmpl/project/jobs.html",
			controller: jobsCtrl
		})
		.state('app.addjob', {
			url: '/jobs/addjob/:projectId/:projectName',
			templateUrl: "tmpl/project/addjob.html",
			controller: creatJobsCtrl
		})
		.state('app.editjob', {
			url: '/jobs/editjob/:projectId/:projectName',
			templateUrl: "tmpl/project/editjob.html",
			controller: editJobsCtrl
		})
		.state('app.runrecord', {
			url: '/jobs/runrecord/:projectId/:projectName/:jobId',
			templateUrl: "tmpl/project/runrecord.html",
			controller: runRecordCtrl
		})
		.state('app.dashlist', {
			url: '/dashlist',
			templateUrl: "tmpl/dashboard/dashlist.html",
			controller: dashListCtrl
		})
		.state('app.dashboard', {
			url: '/dashboard/:dashId/:dashName',
			templateUrl: "tmpl/dashboard/dashboard.html",
			controller: dashboardCtrl
		})
		.state('app.datasource', {
			url: '/datasource/:dashId/:dashName',
			templateUrl: "tmpl/dashboard/datasource.html",
			controller: dataSourceCtrl
		})
}]);

//全局拦截$http
app.factory('httpInterceptor', ['$q', '$injector', '$rootScope', function ($q, $injector, $rootScope) {
	var httpInterceptor = {
		'request': function (config) {
		    // set token in headers
		    var token = localStorage.getItem('authentication_token');
		    if(token){
		        config.headers['authentication_token'] = token;
		    }
			$rootScope.loading = true;
			return config;
		},
		'requestError': function (config) {
			console.log(config)
			return $q.reject(config);
		},
		'response': function (response) {
			$rootScope.loading = false;
			return response;
		},
		'responseError': function (response) {
			$rootScope.loading = false;
			if (response.status == 401 && response.data == 'NEED_LOGIN') {
				window.location = "/login.html";
			} else {
				alert(response.data.msg);
				return $q.reject(response);
			}
		}
	}
	return httpInterceptor;
}]);
app.config(function ($httpProvider) {
	$httpProvider.interceptors.push('httpInterceptor');
});

//自定义分页控件
app.directive('myPager', function () {
	return {
		template: '<div class="pagination" ng-show="pager.pages > 0">' +
			'<button ng-click="changeCurrent(pager.currentPage -1);pager.goPage(pager.currentPage)" ng-disabled="preDisable">上一页</button>' +
			'<button ng-repeat="p in pageList" ng-click="changeCurrent(p);pager.goPage(p);" ng-class="{active:pager.currentPage == p}">{{p}}</button>' +
			'<button ng-click="changeCurrent(pager.currentPage +1);pager.goPage(pager.currentPage)" ng-disabled="nextDisable">下一页</button>' +
			'</div>',
		scope: {
			pager: '=pager'
		},
		link: function (scope, element, attrs) {
			scope.pageList = [];
			scope.changeCurrent = function (p) {
				if (p > scope.pager.pages && p > scope.pager.pages > 0) {
					return scope.changeCurrent(p - 1);
				}
				if (p == 0) {
					p = 1;
				}
				scope.pager.currentPage = p;
				var start, end;
				var pagerDisplayLength = 7;
				var pager = scope.pager;
				if (pager.pages <= pagerDisplayLength) {
					start = 1;
					end = pager.pages;
				} else {
					if (pager.pages - pager.currentPage < pagerDisplayLength / 2) {
						end = pager.pages;
						start = end - pagerDisplayLength + 1;
					} else {
						var middle = pager.currentPage;
						start = parseInt(middle - pagerDisplayLength / 2) + 1;
						if (start <= 1) {
							start = 1;
						}
						// end = parseInt(middle + pagerDisplayLength / 2);
						end = start + pagerDisplayLength - 1;
					}
				}
				scope.pageList.length = 0;
				for (var i = start; i <= end; i++) {
					scope.pageList.push(i);
				}
				scope.preDisable = pager.currentPage == 1;
				scope.nextDisable = pager.currentPage == pager.pages;
			};
			// scope.changeCurrent(scope.pager.currentPage);
			scope.$watch('pager.pages', function () {
				scope.changeCurrent(scope.pager.currentPage);
			});
		}
	}
});

// 自定义日期时间控件
app.directive('defLaydate', function ($timeout) {
	return {
		require: '?ngModel',
		restrict: 'A',
		scope: {
			ngModel: '=',
			minDate: '@'
		},
		link: function (scope, element, attr, ngModel) {
			var _date = null, _config = {};
			// 渲染模板完成后执行
			$timeout(function () {
				// 初始化参数 
				_config = {
					elem: '#' + attr.id,
					type: 'datetime',
					btns: ['clear', 'confirm'],
					min: attr.hasOwnProperty('minDate') ? attr.minDate : '',
					choose: function (data) {
						scope.$apply(setViewValue);
					},
					clear: function () {
						ngModel.$setViewValue(null);
					},
					done: function (data) {
						ngModel.$setViewValue(data);
					}
				};
				// 初始化
				_date = laydate.render(_config);
				// 模型值同步到视图上
				ngModel.$render = function () {
					element.val(ngModel.$viewValue || '');
				};
				// 监听元素上的事件
				element.on('blur keyup change', function () {
					scope.$apply(setViewValue);
				});
				setViewValue();
				// 更新模型上的视图值
				function setViewValue() {
					var val = element.val();
					ngModel.$setViewValue(val);
				}
			}, 0);
		}
	}
});

//拖拽指令
var convertFirstUpperCase = function (str) {
	return str.replace(/(\w)/, function (s) {
		return s.toUpperCase();
	});
}
var rubyDragEventDirectives = {};
angular.forEach("dragstart drag dragenter dragover drop dragleave dragend".split(' '), function (eventName) {
	var rubyEventName = 'ruby' + convertFirstUpperCase(eventName);
	rubyDragEventDirectives[rubyEventName] = ['$parse', function ($parse) {
		//$parse 语句解析器
		return {
			restrict: 'A',
			compile: function (ele, attr) {
				var fn = $parse(attr[rubyEventName]);
				return function rubyEventHandler(scope, ele) {
					ele[0].addEventListener(eventName, function (event) {
						if (eventName == 'dragover' || eventName == 'drop') {
							event.preventDefault();
						}
						var callback = function () {
							fn(scope, { event: event });
						};
						callback();
					});
				}
			}
		}
	}]
});
app.directive(rubyDragEventDirectives);

// 日期事件格式化
function formatDateTime(date, str) {
	var mat = {};
	mat.M = date.getMonth() + 1;//月份记得加1
	mat.H = date.getHours();
	mat.s = date.getSeconds();
	mat.m = date.getMinutes();
	mat.Y = date.getFullYear();
	mat.D = date.getDate();
	mat.d = date.getDay();//星期几
	mat.d = check(mat.d);
	mat.H = check(mat.H);
	mat.M = check(mat.M);
	mat.D = check(mat.D);
	mat.s = check(mat.s);
	mat.m = check(mat.m);
	if (str.indexOf(":") > -1) {
		mat.Y = mat.Y.toString().substr(2, 2);
		return mat.Y + "/" + mat.M + "/" + mat.D + " " + mat.H + ":" + mat.m + ":" + mat.s;
	}
	if (str.indexOf("/") > -1) {
		return mat.Y + "/" + mat.M + "/" + mat.D + " " + mat.H + "/" + mat.m + "/" + mat.s;
	}
	if (str.indexOf("-") > -1) {
		return mat.Y + "-" + mat.M + "-" + mat.D + " " + mat.H + "-" + mat.m + "-" + mat.s;
	}
}
// 检查是不是两位数字，不足补全
function check(str) {
	str = str.toString();
	if (str.length < 2) {
		str = '0' + str;
	}
	return str;
}

