//仪表盘列表控制器
function dashListCtrl($scope, $http, $state, $stateParams) {
	$scope.isAddProShow = false;
	$scope.isDelProShow = false;
	$scope.queryName = '';
	//获取仪表盘列表
	$scope.getALlDashBoard = function(callback) {
		$http({
			method: 'post',
			url: dashBaseUrl + '/webapi/getALlDashBoard',
			data: {
				"kw": $scope.queryName
			}
		}).success(function(resp) {
			$scope.dashboards = resp;
			if(callback != null && typeof callback == 'function') {
				callback(resp);
			}
		});
	}
	$scope.getALlDashBoard();
	//新增仪表盘
	$scope.dashId = 0;
	$scope.dashName = "";
	$scope.toDashboard = function() {
		if(!$scope.dashName) {
			$scope.errorMsg = "请输入仪表盘名称";
			return;
		}
		if(!$scope.dashName.match(/^[\u4E00-\u9FA5a-zA-Z0-9_]{3,20}$/)) {
			$scope.errorMsg = "只允许包含汉字、字母、数字及下划线且不超过20个字符";
			return;
		}
		$scope.errorMsg = '';
		$state.go('app.dashboard', {
			dashId: $scope.dashId,
			dashName: $scope.dashName
		});
	}
	//编辑仪表盘
	$scope.editDashboard = function(id, name) {
		$state.go('app.dashboard', {
			dashId: id,
			dashName: name
		});
	}
	//删除仪表盘
	$scope.deleteDashboard = function(ev, id) {
		ev.preventDefault();
		$scope.isDelProShow = true;
		$scope.deleteDashId = id;
	}
	$scope.confirmDel = function() {
		$http.delete(dashBaseUrl + "/webapi/delDashBoard/" + $scope.deleteDashId).then(function(resp) {
			$scope.isDelProShow = false;
			$scope.deleteDashId = '';
			$scope.getALlDashBoard();
		});
	}
}

//仪表盘控制器
function dashboardCtrl($scope, $http, $state, $stateParams, $timeout) {
	$scope.dashId = $stateParams.dashId;
	$scope.dashName = $stateParams.dashName;
	$scope.isPreviewShow = false;
	$scope.saveSuccess = false;
	$scope.isBindData = false;
	$scope.dataList = [];
	//dabric 初始化
	var canvas = new fabric.Canvas('canvas');
	fabric.Object.prototype.transparentCorners = false;
	//递归遍历循环json
	$scope.findText = function(arr, parentIndex) {
		angular.forEach(arr, function(item, index) {
			if(item.text) {
				var markArry = item.text.split('：');
				var markObj = {};
				markObj.zbName = markArry[0];
				markObj.zbData = markArry[1];
				markObj.zbIndex = parentIndex;
				$scope.dataList.push(markObj);
				return item;
			} else if(item.objects && item.objects.length > 0) {
				$scope.findText(item.objects, index);
			}
		});
	}
	//编辑仪表盘
	if($scope.dashId != 0) {
		$http.get(dashBaseUrl + "/webapi/getDashBoardById/" + $scope.dashId).then(function(resp) {
			var canvasData = resp.data.content;
			var dataArr = JSON.parse(canvasData).objects;
			$scope.findText(dataArr);
			canvas.loadFromJSON(canvasData);
		});
	}
	//图片拖拽添加
	$scope.addImage = function(path, name) {
		fabric.Image.fromURL(path, function(image) {
			image.set({
				left: 150,
				top: 150,
				strokeLineCap: name
			});
			canvas.add(image);
		});
	}
	$scope.dragstart = function(e) {
		e.dataTransfer.setData("ImageSrc", e.target.src);
		e.dataTransfer.setData("ImageAlt", e.target.alt);
	};
	$scope.dragover = function(e) {
		e.preventDefault();
	};
	$scope.drop = function(e) {
		e.preventDefault();
		var imgPath = e.dataTransfer.getData("ImageSrc");
		var imgName = e.dataTransfer.getData("ImageAlt");
		$scope.addImage(imgPath, imgName);
	};
	//delete键删除
	document.onkeydown = function(e) {
		if(e.key == "Delete") {
			canvas.remove(canvas.getActiveObject())
		}
	}
	//预览
	$scope.preview = function() {
		$scope.previewImg = canvas.toDataURL("image/png");
		$scope.isPreviewShow = true;
	}
	//保存提交
	$scope.newAddDashboard = function() {
		$scope.canvasJson = JSON.stringify(canvas.toJSON());
		$scope.postData = {
			"name": $scope.dashName,
			"content": $scope.canvasJson
		};
		if($scope.dashId == 0) { //新增
			$http({
				method: 'post',
				url: dashBaseUrl + '/webapi/addDashBoard',
				data: $scope.postData
			}).success(function(resp) {
				$scope.saveSuccess = true;
				$timeout(function() {
					$scope.saveSuccess = false;
				}, 1100);
			});
		} else { //编辑
			$http({
				method: 'put',
				url: dashBaseUrl + '/webapi/updateDashBoard/' + $scope.dashId,
				data: $scope.postData
			}).success(function(resp) {
				$scope.saveSuccess = true;
				$timeout(function() {
					$scope.saveSuccess = false;
				}, 1100);
			});
		}
	}
	//样式设置
	$scope.canvasModel = {
		"name": "",
		"left": 0,
		"top": 0,
		"angle": 0,
		"scaleX": 1
	}
	$scope.updateView = function(type) {
		switch(type) {
			case 'name':
				canvas.getActiveObject().set('strokeLineCap', $scope.canvasModel.name).setCoords();
				break;
			case 'scale':
				if($scope.canvasModel.scaleX > 3 || $scope.canvasModel.scaleX < 0.1) {
					$scope.canvasModel.scaleX = parseFloat(canvas.getActiveObject().scaleX).toFixed(2);
					return;
				}
				if(!isNaN($scope.canvasModel.scaleX) && $scope.canvasModel.scaleX != '') {
					canvas.getActiveObject().scale(parseFloat($scope.canvasModel.scaleX).toFixed(2)).setCoords();
				} else {
					$scope.canvasModel.scaleX = parseFloat(canvas.getActiveObject().scaleX).toFixed(2);
				}
				break;
			case 'angle':
				if($scope.canvasModel.angle > 360 || $scope.canvasModel.angle < 0) {
					$scope.canvasModel.angle = parseInt(canvas.getActiveObject().angle, 10);
					return;
				}
				if(!isNaN($scope.canvasModel.angle) && $scope.canvasModel.angle != '') {
					canvas.getActiveObject().set('angle', parseInt($scope.canvasModel.angle, 10)).setCoords();
				} else {
					$scope.canvasModel.angle = parseInt(canvas.getActiveObject().angle, 10);
				}
				break;
			case 'left':
				if($scope.canvasModel.left > 790 || $scope.canvasModel.left < 0) {
					$scope.canvasModel.left = parseInt(canvas.getActiveObject().left, 10);
					return;
				}
				if(!isNaN($scope.canvasModel.left) && $scope.canvasModel.left != '') {
					canvas.getActiveObject().set('left', parseInt($scope.canvasModel.left, 10)).setCoords();
				} else {
					$scope.canvasModel.left = parseInt(canvas.getActiveObject().left, 10);
				}
				break;
			case 'top':
				if($scope.canvasModel.top > 560 || $scope.canvasModel.top < 0) {
					$scope.canvasModel.top = parseInt(canvas.getActiveObject().top, 10);
					return;
				}
				if(!isNaN($scope.canvasModel.top) && $scope.canvasModel.top != '') {
					canvas.getActiveObject().set('top', parseInt($scope.canvasModel.top, 10)).setCoords();
				} else {
					$scope.canvasModel.top = parseInt(canvas.getActiveObject().top, 10);
				}
				break;
			default:
				break;
		}
		canvas.requestRenderAll();
	}
	$scope.updateModel = function() {
		$scope.$apply(function() {
			$scope.canvasModel.name = canvas.getActiveObject().strokeLineCap;
			$scope.canvasModel.scaleX = parseFloat(canvas.getActiveObject().scaleX).toFixed(2);
			$scope.canvasModel.angle = parseFloat(canvas.getActiveObject().angle).toFixed(2);
			$scope.canvasModel.left = parseFloat(canvas.getActiveObject().left).toFixed(2);
			$scope.canvasModel.top = parseFloat(canvas.getActiveObject().top).toFixed(2);
		});
	}
	canvas.on('mouse:down', function(e) {
		if(e.target) {
			$scope.$apply(function() {
				if(canvas.getActiveObject()._objects) {
					var markArry = canvas.getActiveObject()._objects[1].text.split('：');
					$scope.markName = markArry[0];
					$scope.markNum = markArry[1];
				} else {
					$scope.markName = '';
					$scope.markNum = '';
				}
				$scope.canvasModel.name = canvas.getActiveObject().strokeLineCap;
				$scope.canvasModel.scaleX = parseFloat(canvas.getActiveObject().scaleX).toFixed(2);
				$scope.canvasModel.angle = parseFloat(canvas.getActiveObject().angle).toFixed(2);
				$scope.canvasModel.left = parseFloat(canvas.getActiveObject().left).toFixed(2);
				$scope.canvasModel.top = parseFloat(canvas.getActiveObject().top).toFixed(2);
			});
		}
	});
	canvas.on({
		'object:selected': $scope.updateModel,
		'object:moving': $scope.updateModel,
		'object:scaling': $scope.updateModel,
		'object:resizing': $scope.updateModel,
		'object:rotating': $scope.updateModel,
		'object:skewing': $scope.updateModel
	});
	//数据设置
	$scope.updateMarkView = function(canvasIndex, index) {
		var activeObj = canvas._objects[canvasIndex];
		activeObj.set('strokeLineCap', $scope.dataList[index].zbName).setCoords();
		activeObj._objects[1].set('text', $scope.dataList[index].zbName + '：' + $scope.dataList[index].zbData).setCoords();
		canvas.requestRenderAll();
	};
	$scope.addMark = function() {
		var text = new fabric.Text("：", {
			fontSize: 14,
			left: 50,
			top: 23,
			fill: 'white'
		});
		fabric.Image.fromURL('statics/img/img04.png', function(image) {
			var group = new fabric.Group([image, text], {
				left: 500,
				top: 150,
				angle: 0,
				strokeLineCap: $scope.markName
			});
			canvas.add(group);
		});
		$timeout(function() {
			var canvasStr = JSON.stringify(canvas.toJSON());
			var canvasJson = JSON.parse(canvasStr).objects;
			$scope.dataList = [];
			$scope.findText(canvasJson);
		}, 100);
	}
	//绑定数据
	$scope.computeData = {
		"index": 0,
		"id": 0,
		"method": "SUM",
		"column": ""
	}
	$scope.dataTables = [];
	$scope.cloumnNames = [];
	$scope.checkeds = [];
	$scope.computeMethods = [{
			"id": "SUM",
			"name": "SUM"
		},
		{
			"id": "AVG",
			"name": "AVG"
		},
		{
			"id": "MAX",
			"name": "MAX"
		},
		{
			"id": "MIN",
			"name": "MIN"
		}
	];
	$scope.computeData.method = $scope.computeMethods[0].id;

	$scope.toBindData = function(canvasIndex, index) {
		//获取数据源内所有数据
		$http.get(dashBaseUrl + "/webapi/getALlDataSource").then(function(resp) {
			if(resp.data.length > 0) {
				$scope.dataTables = resp.data;
				$scope.computeData.id = $scope.dataTables[0].id;
				$scope.previewTable($scope.dataTables[0]);
			}
		});
		$scope.computeData = {
			"index": 0,
			"id": 0,
			"method": "SUM",
			"column": ""
		}
		$scope.isBindData = true;
		$scope.canvasIndex = canvasIndex;
		$scope.zbIndexData = index;
	}
	//获取列名
	$scope.previewTable = function(data) {
		$http({
			method: 'post',
			url: dashBaseUrl + '/webapi/previewData',
			data: data
		}).success(function(resp) {
			$scope.cloumnNames = resp[0];
			$scope.computeData.column = $scope.cloumnNames[0];
			$scope.checkeds = [];
			for(var i=0;i<$scope.cloumnNames.length;i++){
				$scope.checkeds.push("SUM");
			}
		});
	}
	//更改列组
	$scope.changeColumn = function(){
		$http.get(dashBaseUrl + "/webapi/getDataSourceById/" + $scope.computeData.id,).then(function(resp) {
			$scope.previewTable(resp.data);
		});
	}
	//更改列名
	$scope.changeRadio = function(index){
		$scope.computeData.index = index;
		$scope.computeData.method = $scope.checkeds[index];
	}
	//更改聚合方式
	$scope.changeMethod = function(index){
		if($scope.computeData.index == index){
			$scope.computeData.method = $scope.checkeds[index];
		}
	}
	//绑定
	$scope.submitBindData = function() {
		//数据聚合接口
		if(!$scope.computeData.id) {
				alert("请选择数据表！");
				return;
			}
			if(!$scope.computeData.column) {
				alert("请选择列名！");
				return;
			}
			if(!$scope.computeData.method) {
				alert("请选择聚合方式！");
				return;
			}
			$http({
				method: 'post',
				url: dashBaseUrl + '/webapi/aggrgate/' + $scope.computeData.method +"/"+$scope.computeData.id+"/"+$scope.computeData.column
			}).success(function(resp) {
				$scope.dataList[$scope.zbIndexData].zbData = resp;
				$scope.updateMarkView($scope.canvasIndex, $scope.zbIndexData);
				$scope.isBindData = false;
			});
	}

}

//数据源控制器
function dataSourceCtrl($scope, $http, $stateParams, $rootScope, FileUploader) {
	$scope.dashId = $stateParams.dashId;
	$scope.dashName = $stateParams.dashName;
	$scope.isDelProShow = false;
	$scope.isPagination = false;
	$scope.isAddDatasource = false;
	$scope.isEditDatasource = false;
	$scope.dataTables = [];
	$scope.queryName = '';
	//获取数据列表
	$scope.getDataList = function(pageNo, callback) {
		$http({
			method: 'post',
			url: dashBaseUrl + '/webapi/getALlDataSource',
			data: {
				"kw": $scope.queryName,
				"page": pageNo
			}
		}).success(function(resp) {
			$scope.dataList = resp.content;
			$scope.isPagination = !Boolean(resp.content.length);
			$scope.pager.pages = resp.totalPages;
			if(callback != null && typeof callback == 'function') {
				callback(resp);
			}
		});
	}
	$scope.getDataList(1);
	//分页控件options
	$scope.pager = {
		pages: 1,
		currentPage: 1,
		goPage: function(p) {
			$scope.getDataList(p);
		}
	};
	//删除数据源
	$scope.deleteData = function(ev, id) {
		ev.preventDefault();
		$scope.isDelProShow = true;
		$scope.deleteDataId = id;
	}
	$scope.confirmDel = function() {
		$http.delete(dashBaseUrl + "/webapi/delDataSource/" + $scope.deleteDataId).then(function(resp) {
			$scope.isDelProShow = false;
			$scope.deleteDataId = '';
			$scope.getDataList($scope.pager.currentPage, function(resp) {
				if(resp.content.length == 0 && $scope.pager.currentPage > 1) {
					$scope.pager.currentPage--;
					$scope.getDataList($scope.pager.currentPage);
				}
			});
		});
	}

	/* 新增数据 */
	$scope.AddDataSource = {
		"name": "",
		"type": "csv",
		"path": "",
		"url": "",
		"username": "",
		"password": "",
		"tableName": "",
		"param": ""
	}
	//上传文件
	var uploader = $scope.uploader = new FileUploader({
		url: dashBaseUrl + '/webapi/uploadimg',
		autoUpload: true
	});
	uploader.filters.push({ //过滤
		name: 'csvFilter',
		fn: function(item, options) {
			var type = item.name.slice(item.name.lastIndexOf('.') + 1);
			if(type != 'csv') {
				alert("只支持csv文件!");
				return false;
			}
			var fileSize = Math.round(item.size / 1024 * 100) / 100;
			if(fileSize > 1024) {
				alert("文件大小不能超过1M!");
				return false;
			}
			return true;
		}
	});
	uploader.onSuccessItem = function(fileItem, response, status, headers) {
		$scope.AddDataSource.path = response;
	};
	uploader.onBeforeUploadItem = function(item) {
		$rootScope.loading = true;
	};
	uploader.onCompleteAll = function() {
		$rootScope.loading = false;
	};
	//提交
	$scope.submitDataSource = function() {
		if(!$scope.AddDataSource.name) {
			$scope.errorMsg = "请输入数据名称";
			return;
		}
		if(!$scope.AddDataSource.name.match(/^[\u4E00-\u9FA5a-zA-Z0-9_]{3,20}$/)) {
			$scope.errorMsg = "只允许包含汉字、字母、数字及下划线且不超过20个字符";
			return;
		}
		$scope.errorMsg = '';
		switch($scope.AddDataSource.type) {
			case 'csv':
				if(!$scope.AddDataSource.path) {
					alert("请上传csv格式的文件！");
					return;
				}
				break;
			case 'mysql':
				if(!$scope.AddDataSource.url) {
					alert("请输入数据库url！");
					return;
				}
				if(!$scope.AddDataSource.username) {
					alert("请输入用户名！");
					return;
				}
				if(!$scope.AddDataSource.password) {
					alert("数据库密码错误！");
					return;
				}
				if(!$scope.AddDataSource.tableName) {
					alert("请选择数据表！");
					return;
				}
				break;
			default:
				break;
		}
		//ajax
		$http({
			method: 'post',
			url: dashBaseUrl + '/webapi/addDataSource',
			data: $scope.AddDataSource
		}).success(function(resp) {
			$scope.getDataList(1);
			$scope.isAddDatasource = false;
			$scope.dataTables = [];
			$scope.AddDataSource = {
				"name": "",
				"type": "csv",
				"path": "",
				"url": "",
				"username": "",
				"password": "",
				"tableName": "",
				"param": ""
			}
		});
	}

	/* 编辑数据 */
	$scope.editDataSource = {
		"id": 0,
		"name": "",
		"type": "",
		"path": "",
		"url": "",
		"username": "",
		"password": "",
		"tableName": "",
		"param": ""
	}
	$scope.editDataById = function(ev, data) {
		ev.preventDefault();
		$scope.editDataSource = {
			"id": data.id,
			"name": data.name,
			"type": data.type,
			"path": data.path,
			"url": data.url,
			"username": data.username,
			"password": data.password,
			"tableName": data.tableName,
			"param": data.param
		}
		if($scope.editDataSource.type == 'mysql') {
			$scope.queryAllTable(1); // 查询所有表
		}
		$scope.isEditDatasource = true;
	}
	// 提交
	$scope.submitEditData = function() {
		if(!$scope.editDataSource.name) {
			$scope.errorMsg = "请输入数据名称";
			return;
		}
		if(!$scope.editDataSource.name.match(/^[\u4E00-\u9FA5a-zA-Z0-9_]{3,20}$/)) {
			$scope.errorMsg = "只允许包含汉字、字母、数字及下划线且不超过20个字符";
			return;
		}
		$scope.errorMsg = '';
		switch($scope.editDataSource.type) {
			case 'csv':
				if(!$scope.editDataSource.path) {
					alert("请上传csv格式的文件！");
					return;
				}
				break;
			case 'mysql':
				if(!$scope.editDataSource.url) {
					alert("请输入数据库url！");
					return;
				}
				if(!$scope.editDataSource.username) {
					alert("请输入用户名！");
					return;
				}
				if(!$scope.editDataSource.password) {
					alert("数据库密码错误！");
					return;
				}
				if(!$scope.editDataSource.tableName) {
					alert("请选择数据表！");
					return;
				}
				break;
			default:
				break;
		}
		$http({
			method: 'put',
			url: dashBaseUrl + '/webapi/updateDataSource/' + $scope.editDataSource.id,
			data: $scope.editDataSource
		}).success(function(resp) {
			$scope.getDataList(1);
			$scope.isEditDatasource = false;
			$scope.dataTables = [];
			$scope.editDataSource = {
				"id": 0,
				"name": "",
				"type": "",
				"path": "",
				"url": "",
				"username": "",
				"password": "",
				"tableName": "",
				"param": ""
			}
		});
	}
	//查询所有的表
	$scope.queryAllTable = function(type) {
		if(type == 0) { //新增
			$http({
				method: 'post',
				url: dashBaseUrl + '/webapi/queryAllTable',
				data: $scope.AddDataSource
			}).success(function(resp) {
				if(resp.length > 0) {
					angular.forEach(resp, function(item) {
						var obj = {
							"id": "",
							"name": ""
						};
						obj.id = item.name;
						obj.name = item.name;
						$scope.dataTables.push(obj);
					});
					$scope.AddDataSource.tableName = $scope.dataTables[0].id;
				}
			});
		} else { //编辑
			$http({
				method: 'post',
				url: dashBaseUrl + '/webapi/queryAllTable',
				data: $scope.editDataSource
			}).success(function(resp) {
				if(resp.length > 0) {
					angular.forEach(resp, function(item) {
						var obj = {
							"id": "",
							"name": ""
						};
						obj.id = item.name;
						obj.name = item.name;
						$scope.dataTables.push(obj);
					});
				}
			});
		}

	}
	//测试数据库连接
	$scope.testConnect = function(type) {
		if(type == 0) { //新增
			if(!$scope.AddDataSource.url) {
				alert("请输入数据库url！");
				return;
			}
			if(!$scope.AddDataSource.username) {
				alert("请输入用户名！");
				return;
			}
			if(!$scope.AddDataSource.password) {
				alert("数据库密码错误！");
				return;
			}
			$http({
				method: 'post',
				url: dashBaseUrl + '/webapi/testConn',
				data: $scope.AddDataSource
			}).success(function(resp) {
				$scope.queryAllTable(0);
			});
		} else { //编辑
			if(!$scope.editDataSource.url) {
				alert("请输入数据库url！");
				return;
			}
			if(!$scope.editDataSource.username) {
				alert("请输入用户名！");
				return;
			}
			if(!$scope.editDataSource.password) {
				alert("数据库密码错误！");
				return;
			}
			$http({
				method: 'post',
				url: dashBaseUrl + '/webapi/testConn',
				data: $scope.editDataSource
			}).success(function(resp) {
				console.log(resp);
			});
		}
	}

	//预览表数据
	$scope.cloumnNames = [];
	$scope.tableDatas = [];
	$scope.previewTable = function(data) {
		$http({
			method: 'post',
			url: dashBaseUrl + '/webapi/previewData',
			data: data
		}).success(function(resp) {
			$scope.cloumnNames = resp[0];
			$scope.tableDatas = resp.slice(1);
		});
	}

}