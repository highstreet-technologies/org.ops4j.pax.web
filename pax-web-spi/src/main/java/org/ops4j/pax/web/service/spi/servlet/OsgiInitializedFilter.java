/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link Filter} wrapper that uses correct {@link FilterConfig} wrapper that returns correct wrapper
 * for {@link javax.servlet.ServletContext}
 */
public class OsgiInitializedFilter implements Filter {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiInitializedFilter.class);

	private final Filter filter;
	private final FilterModel filterModel;
	private final ServletContext servletContext;
	private Pattern[] filterPatterns = null;

	/**
	 * Whether TCCL should be set to servlet's bundle classloader. If {@code false}, TCCL from
	 * containing {@link ServletContext} will be used.
	 */
	private final boolean whiteboardTCCL;

	public OsgiInitializedFilter(Filter filter, FilterModel model, ServletContext servletSpecificContext,
			boolean whiteboardTCCL) {
		this.filter = filter;
		this.filterModel = model;
		this.servletContext = servletSpecificContext;
		this.whiteboardTCCL = whiteboardTCCL;

		if (model != null && model.getMappingsPerDispatcherTypes().size() == 1) {
			String[] regexPatterns = model.getMappingsPerDispatcherTypes().get(0).getRegexPatterns();
			if (regexPatterns != null && regexPatterns.length > 0) {
				// we have Whiteboard-special RegEx filter
				LOG.debug("Preparing RegEx based filter for {}", model);
				filterPatterns = new Pattern[regexPatterns.length];
				List<Pattern> patterns = new ArrayList<>();
				for (String pattern : regexPatterns) {
					try {
						patterns.add(Pattern.compile(pattern));
					} catch (PatternSyntaxException e) {
						LOG.warn("Problem compiling filter RegEx pattern \"{}\". Skipping", pattern);
					}
				}
				if (patterns.size() > 0) {
					filterPatterns = patterns.toArray(new Pattern[0]);
				} else {
					LOG.warn("No RegEx pattern can be compiled. Filter will match all the requests");
				}
			}
		}
	}

	@Override
	public void init(final FilterConfig config) throws ServletException {
		if (filter == null) {
			return;
		}
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader newCl = null;
			if (servletContext != null) {
				newCl = servletContext.getClassLoader();
				if (newCl == null) {
					Bundle bundle = null;
					if (filterModel != null) {
						bundle = filterModel.getRegisteringBundle();
					}
					if (bundle == null && servletContext instanceof OsgiScopedServletContext) {
						bundle = ((OsgiScopedServletContext) servletContext).getBundle();
					}
					BundleWiring wiring = bundle == null ? null : bundle.adapt(BundleWiring.class);
					if (wiring != null) {
						newCl = wiring.getClassLoader();
					}
				}
			}
			if (newCl != null) {
				Thread.currentThread().setContextClassLoader(newCl);
			}
			filter.init(new FilterConfig() {
				@Override
				public String getFilterName() {
					return config.getFilterName();
				}

				@Override
				public ServletContext getServletContext() {
					return OsgiInitializedFilter.this.servletContext;
				}

				@Override
				public String getInitParameter(String name) {
					return config.getInitParameter(name);
				}

				@Override
				public Enumeration<String> getInitParameterNames() {
					return config.getInitParameterNames();
				}
			});
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		ClassLoader tccl = null;
		try {
			if (whiteboardTCCL) {
				tccl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(request.getServletContext().getClassLoader());
			}
			if (filterPatterns != null) {
				// do RegEx matching
				boolean match = false;
				if (request instanceof HttpServletRequest) {
					String uri = ((HttpServletRequest) request).getRequestURI();
					if (((HttpServletRequest) request).getQueryString() != null) {
						uri += "?" + ((HttpServletRequest) request).getQueryString();
					}
					for (Pattern p : filterPatterns) {
						if (p.matcher(uri).matches()) {
							match = true;
							break;
						}
					}
				} else {
					match = true;
				}
				if (match) {
					filter.doFilter(request, response, chain);
				} else {
					// just invoke the chain without this filter
					chain.doFilter(request, response);
				}
			} else {
				if (filter == null) {
					chain.doFilter(request, response);
				} else {
					filter.doFilter(request, response, chain);
				}
			}
		} finally {
			if (whiteboardTCCL) {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}
	}

	@Override
	public void destroy() {
		filter.destroy();
	}

	public Filter getDelegate() {
		return filter;
	}

}
