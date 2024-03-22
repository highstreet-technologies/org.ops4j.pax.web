/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.war.fragment;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "annotatedServlet1", urlPatterns = { "/as1", "/as1/*" }, initParams = {
		@WebInitParam(name = "param1", value = "value1"),
		@WebInitParam(name = "param2", value = "value2")
}, loadOnStartup = 3)
@MultipartConfig(location = "/dev/null", maxFileSize = 424242L, maxRequestSize = 1024, fileSizeThreshold = 128)
public class AnnotatedServlet1 extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String msg = String.format("Hello %s!", req.getPathInfo());
		resp.setContentLength(msg.length());
		resp.getWriter().print(msg);
	}

}
