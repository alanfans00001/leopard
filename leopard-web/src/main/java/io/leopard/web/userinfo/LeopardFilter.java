package io.leopard.web.userinfo;

import io.leopard.topnb.LeopardWebTimeLog;
import io.leopard.util.Clocker;
import io.leopard.util.MonitorContext;
import io.leopard.util.avgtime.AvgTime;
import io.leopard.web.interceptor.ConnectionLimitInterceptor;
import io.leopard.web.security.CsrfUtil;
import io.leopard.web.userinfo.service.ConfigHandler;
import io.leopard.web.userinfo.service.SkipFilterService;
import io.leopard.web.userinfo.service.UserinfoService;
import io.leopard.web4j.proxy.ResinProxy;
import io.leopard.web4j.session.SessionService;
import io.leopard.web4j.view.RequestUtil;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class LeopardFilter implements Filter {
	private final Log logger = LogFactory.getLog(this.getClass());

	@Resource
	private ConfigHandler loginHandler;

	// @Autowired(required = false)
	// protected AdminService adminService;

	@Autowired(required = false)
	protected SessionService sessionService;

	@Autowired
	private UserinfoService userinfoService;
	@Autowired
	private SkipFilterService skipFilterService;

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		MonitorContext.setRequest(request);// 按入口进行性能监控
		String uri = RequestUtil.getRequestContextUri(request);// request.getRequestURI();

		// 页面代理，实现访问指定resin机器.
		{
			boolean proxy = ResinProxy.proxy(uri, request, res);
			if (proxy) {
				return;
			}
		}
		LeopardWebTimeLog.start();
		HttpServletResponse response = (HttpServletResponse) res;

		if (skipFilterService.isSkipFilter(uri)) {
			chain.doFilter(req, res);
			return;
		}
		LeopardRequestWrapper httpRequestWraper = new LeopardRequestWrapper(request, response, sessionService);
		if (!this.checkLogin(httpRequestWraper, response)) {
			return;
		}
		LeopardWebTimeLog.stop();
		chain.doFilter(httpRequestWraper, response);
	}

	protected boolean checkLogin(HttpServletRequest request, HttpServletResponse response) {
		boolean isExcludeUri = userinfoService.isExcludeUri(request);
		CsrfUtil.setExcludeUri(request, isExcludeUri);
		// System.err.println("uri:" + RequestUtil.getRequestContextUri(request)
		// + " isExcludeUri:" + isExcludeUri);
		if (isExcludeUri) {
			return true;
		}
		Object account = userinfoService.login(request, response);
		if (account == null) {
			this.userinfoService.showLoginBox(request, response);
			// System.err.println("forwardLoginUrl:");
			return false;
		}

		ConnectionLimitInterceptor.setAccount(request, account);

		return true;
	}

	@Override
	public void destroy() {
		logger.info("destroy");
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.info("init");
	}

}
