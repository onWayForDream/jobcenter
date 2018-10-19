package cn.enn.portal.jobCenter.container.filter;

import cn.enn.portal.jobCenter.core.JobCenterCoreProperty;
import cn.enn.portal.jobCenter.core.entity.UserEntity;
import cn.enn.portal.jobCenter.core.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class LoginFilter implements Filter {

    private Logger logger = LoggerFactory.getLogger(LoginFilter.class);
    private List<String> ignoreList = null;

    @Autowired
    private JobCenterCoreProperty jobCenterCoreProperty;

    @Autowired
    private UserTokenService userTokenService;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("login filter init... jobJarRoot = {}", jobCenterCoreProperty.getJobJarRoot());
        ignoreList = Arrays.asList(
                "/api/user/login",
                "/api/user/unauthorized"
        );
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestUrl = request.getRequestURI();
        if (request.getMethod().toUpperCase().equals("OPTIONS")) {
            logger.debug("skip options request");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        if (!requestUrl.startsWith("/api/") || isIgnoreApi(requestUrl)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            HttpSession session = request.getSession();
            if (session.getAttribute("user") != null) {
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                String token = request.getHeader("authentication_token");
                String clientIp = getClientIp(request);
                logger.debug("token = {}", token);
                logger.debug("remoteIp = {}", clientIp);
                UserEntity userEntity = userTokenService.checkToken(token, clientIp);
                if (userEntity != null) {
                    logger.debug("logged from token");
                    session.setAttribute("user", userEntity);
                    filterChain.doFilter(servletRequest, servletResponse);
                } else {
                    logger.info("redirect to /login.html from url : {}", requestUrl);
                    HttpServletResponse response = (HttpServletResponse) servletResponse;
                    response.setStatus(401);
                    response.setHeader("Access-Control-Allow-Origin", "*");
                    response.getWriter().write("NEED_LOGIN");
                }
            }

        }

    }

    @Override
    public void destroy() {

    }

    private boolean isIgnoreApi(String requestUrl) {
        for (String ignoreItem : ignoreList) {
            if (requestUrl.startsWith(ignoreItem)) {
                return true;
            }
        }
        return false;
    }

    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
}
