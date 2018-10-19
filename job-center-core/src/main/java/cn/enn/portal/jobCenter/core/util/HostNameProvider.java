package cn.enn.portal.jobCenter.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class HostNameProvider {

    public HostNameProvider(@Value("${server.port}") String serverPort) throws UnknownHostException {
        InetAddress myHost = InetAddress.getLocalHost();
        hostname = myHost.getHostName() + ":" + serverPort;
    }

    private String hostname;

    public String getHostName() {
        return hostname;
    }
}
