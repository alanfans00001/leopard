package io.leopard.web.mvc;

import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.DispatcherServlet;

public class LeopardDispatcherServlet extends DispatcherServlet {

	private static final long serialVersionUID = 1L;

	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String uri = request.getRequestURI();
		String mimeType = getServletContext().getMimeType(uri);

		System.err.println("request:" + uri + " mimeType:" + mimeType);

		InputStream input = request.getServletContext().getResourceAsStream(uri);
		if (input == null) {
			super.noHandlerFound(request, response);
			return;
		}
		byte[] bytes = IOUtils.toByteArray(input);

		response.setContentLength(bytes.length);
		if (mimeType != null) {
			response.setContentType(mimeType);
		}

		ServletOutputStream out = response.getOutputStream();
		out.write(bytes);
	}

}
