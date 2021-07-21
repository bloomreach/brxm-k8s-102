package com.example.routingandfilteringgateway.filters.pre;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.ZuulFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrXMServerIdToCookieFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(BrXMServerIdToCookieFilter.class);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        if (request.getHeader("server-id") != null) {
            ctx.addZuulRequestHeader("Cookie", "SERVERID=" + request.getHeader("server-id") + "; Path=/; Secure; HttpOnly");
            log.info("Server ID Found!");
        }
        log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
        return null;
    }

}
