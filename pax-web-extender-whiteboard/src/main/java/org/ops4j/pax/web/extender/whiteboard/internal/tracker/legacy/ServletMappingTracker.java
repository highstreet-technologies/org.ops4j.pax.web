/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>Tracks {@link ServletMapping}s. This is the way to register {@link Servlet} Whiteboard service using
 * Pax Web legacy {@link ServletMapping} which carries all the information in the service itself (instead of via
 * service properties and annotations).</p>
 *
 * <p>This customizer customizes {@code ServletMapping -> ElementModel<Servlet> = ServletModel}</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class ServletMappingTracker extends AbstractMappingTracker<ServletMapping, Servlet, ServletEventData, ServletModel> {

	private ServletMappingTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<ServletMapping, ServletModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ServletMappingTracker(whiteboardExtenderContext, bundleContext).create(ServletMapping.class);
	}

	@Override
	protected ServletModel doCreateElementModel(Bundle bundle, ServletMapping servletMapping, Integer rank, Long serviceId) {

		// When user registers OSGi service with org.ops4j.pax.web.service.whiteboard.ServletMapping, we'll
		// dereference it immediately (making Service/Prototype factories useless), but user STILL may
		// implement org.ops4j.pax.web.service.whiteboard.ServletMapping.getServlet() method to return _new_
		// instance of the servlet on demand (for scenario, where a Servlet is installed into more contexts)
		//
		// the key is that we're using pax-web-extender-whiteboard's bundle context to get the service

		ServletModel.Builder builder = new ServletModel.Builder()
				.withServiceRankAndId(rank, serviceId)
				.withRegisteringBundle(bundle)
				.withAlias(servletMapping.getAlias())
				.withUrlPatterns(servletMapping.getUrlPatterns())
				.withServletName(servletMapping.getServletName())
				.withInitParams(servletMapping.getInitParameters())
				.withAsyncSupported(servletMapping.getAsyncSupported())
				.withLoadOnStartup(servletMapping.getLoadOnStartup())
				.withMultipartConfigElement(servletMapping.getMultipartConfig())
				.withErrorDeclarations(servletMapping.getErrorPages());

		// handle actual source of the servlet
		if (servletMapping.getServletClass() != null) {
			builder.withServletClass(servletMapping.getServletClass());
		} else {
			// Just get the servlet and don't care if getServlet() returns a singleton or not.
			// This will allow us to unget the mapping
			builder.withServlet(servletMapping.getServlet());
		}

		return builder.build();
	}

}
