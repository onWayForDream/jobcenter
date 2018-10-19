$(function () {
    $('#txt-username').focus();
    // resize the right-panel height
    setHeight();
    $(window).on('resize', function () {
        setHeight();
    });

    $('#btn-login').on('click', function () {
        doLogin();
    });

    $('#txt-password').keypress(function (e) {
        if (e.which == 13) {
            doLogin();
        }
    });

    function setHeight() {
        var windowHeight = $(window).height();
        $('#right-panel').height(windowHeight);
    }

    function doLogin() {
        var username = $('#txt-username').val();
        var password = $('#txt-password').val();
        console.log(username + ':' + password);
        if (username == '') {
            alert('请输入用户名');
            return;
        }
        if (password == '') {
            alert('请输入密码');
            return;
        }

//        var baseUrl = "http://jobcenter-test-2:8080";
        var baseUrl = "";
        if (window.location.origin == 'http://localhost:5500') {
            baseUrl = 'http://localhost:8080';
        }

        var loginUrl = baseUrl + '/api/user/login';

        $.ajax({
            url: loginUrl,
            method: "POST",
            data: {
                userName: username, userPassword: password
            }
        }).done(function (response) {
            localStorage.setItem('authentication_token', response.token);
            window.location ='/';
        }).fail(function (response) {
            var obj = JSON.parse(response.responseText);
            if (obj && obj.msg) {
                alert('登录失败：' + obj.msg);
            } else {
                alert('登录失败');
            }
        })
    }
});