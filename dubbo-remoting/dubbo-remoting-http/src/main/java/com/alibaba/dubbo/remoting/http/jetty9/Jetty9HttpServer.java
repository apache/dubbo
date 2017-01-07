package com.alibaba.dubbo.remoting.http.jetty9;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;

/**
 * 
 * @author wuyu
 * Date:2016/3/6
 */
public class Jetty9HttpServer extends AbstractHttpServer {

	private static final Logger logger = LoggerFactory.getLogger(Jetty9HttpServer.class);

	private Server server;

	private int port = 8080;

	public Jetty9HttpServer(URL url, final HttpHandler handler) {
		super(url, handler);
		DispatcherServlet.addHttpHandler(url.getPort(), handler);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

		int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setDaemon(true);
		threadPool.setMaxThreads(threads);
		threadPool.setMinThreads(threads);
		
		server = new Server(threadPool);
		
		ServerConnector serverConnector = new ServerConnector(server);
		if (!url.isAnyHost() && NetUtils.isValidLocalHost(url.getHost())) {
			serverConnector.setHost(url.getHost());
		}
		serverConnector.setPort(url.getPort());
		server.addConnector(serverConnector);
		context.setContextPath("/");
		server.setHandler(context);
		ServletHolder serveletHolder = context.addServlet(DispatcherServlet.class, "/*");
		serveletHolder.setInitOrder(2);

		ServletManager.getInstance().addServletContext(url.getPort(), context.getServletContext());
		try {
			server.start();
		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed to start jetty9 server on " + url.getAddress() + ", cause: " + e.getMessage(), e);
		}
	}

	public void close() {
		super.close();
		ServletManager.getInstance().removeServletContext(port);
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

}