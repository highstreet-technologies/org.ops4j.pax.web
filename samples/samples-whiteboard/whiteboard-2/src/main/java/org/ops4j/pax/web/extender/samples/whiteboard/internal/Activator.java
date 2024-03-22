/*
 * Copyright 2022 OPS4J.
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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<ServletContextHelper> contextReg;

	public void start(final BundleContext bundleContext) throws Exception {
		new Thread(() -> {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "default");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
			props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
			contextReg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper() {
				@Override
				public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
					response.getWriter().println("Overriden default / context");
					return true;
				}
			}, props);
		}).start();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (contextReg != null) {
			contextReg.unregister();
			contextReg = null;
		}
	}

}
