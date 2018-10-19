package cn.enn.portal.jobCenter.container.viewmodel;

public class ErrorResponse {

    public ErrorResponse(){}
    public ErrorResponse(String msg){
        this.msg = msg;
    }

    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
