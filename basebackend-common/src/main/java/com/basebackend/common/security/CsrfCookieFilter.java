package com.basebackend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

/**
 * 将 CSRF Token 写入 Cookie，便于前端读取
 */
@Slf4j
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "XSRF-TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            Cookie existingCookie = WebUtils.getCookie(request, COOKIE_NAME);
            String token = csrfToken.getToken();
            if (token != null && (existingCookie == null || !token.equals(existingCookie.getValue()))) {
                Cookie cookie = new Cookie(COOKIE_NAME, token);
                cookie.setPath("/");
                cookie.setHttpOnly(false);
                cookie.setSecure(request.isSecure());
                try {
                    cookie.setAttribute("SameSite", "Lax");
                } catch (NoSuchMethodError ignored) {
                    log.debug("Servlet container does not support SameSite attributes");
                }
                response.addCookie(cookie);
            }
        }
        filterChain.doFilter(request, response);
    }
}
