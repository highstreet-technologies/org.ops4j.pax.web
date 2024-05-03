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
package org.ops4j.pax.web.service.spi.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.elements.JspConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SessionConfigurationModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.task.Change;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class represents OSGi-specific {@link HttpContext}/{@link ServletContextHelper}
 * and points to single, server-specific {@link javax.servlet.ServletContext} and (at model level) to single
 * {@link ServletContextModel}. It maps <em>directly</em> 1:1 to an OSGi service registered by user:<ul>
 *     <li>{@link HttpContext} with legacy Pax Web servier registration properties</li>
 *     <li>{@link ServletContextHelper} with standard properties and/or annotations</li>
 *     <li>{@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}</li>
 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}</li>
 * </ul>
 * There's yet another internal relationship. Single {@link OsgiContextModel}, while related 1:1 with single
 * <em>ephemeral</em> context, it can be associated with multiple {@link ServletContextHelper} or
 * {@link HttpContext} instances, because the original {@link ServiceReference} can represent
 * {@link org.osgi.framework.ServiceFactory}, so many bundles may obtain different instances of target
 * context.</p>
 *
 * <p>Discovered service registration properties are stored as well to ensure proper context selection according
 * to 140.3 Common Whiteboard Properties.</p>
 *
 * <p>The most important role is to wrap actual {@link HttpContext} or
 * {@link ServletContextHelper} that'll be used when given servlet will be accessing
 * own {@link ServletContext}, to comply with Whiteboard Specification.</p>
 *
 * <p>While many {@link OsgiContextModel OSGi-related contexts} may point to single {@link ServletContextModel} and
 * contribute different web elements (like some bundles provide servlets and other bundle provides login configuration),
 * some aspects need conflict resolution - for example session timeout setting. Simply highest ranked
 * {@link OsgiContextModel} will be the one providing the configuration for given {@link ServletContextModel}.</p>
 *
 * <p>Some aspects of {@link ServletContext} visible to registered element are however dependent on which particular
 * {@link OsgiContextModel} was used. Resource access will be done through {@link HttpContext} or
 * {@link ServletContextHelper} and context parameters will be stored in this
 * class (remember: there can be different {@link OsgiContextModel}s for the same {@link ServletContextModel}, but
 * providing different init parameters ({@code <context-param>} from {@code web.xml}).</p>
 *
 * <p>Another zen-like question: there may be two different {@link ServletContextHelper}
 * services registered for the same <em>context path</em> with different
 * {@link ServletContextHelper#handleSecurity}. Then two filters are registered
 * to both of such contexts - looks like when sending an HTTP request matching this common <em>context path</em>,
 * both {@code handleSecurity()} methods must be called before entering the filter pipeline. Fortunately
 * specification is clear about it. "140.5 Registering Servlet Filters" says:<blockquote>
 *     Servlet filters are only applied to servlet requests if they are bound to the same Servlet Context Helper
 *     and the same Http Whiteboard implementation.
 * </blockquote></p>
 *
 * <p>In Felix-HTTP, N:1 mapping between many {@link OsgiContextModel} and {@link ServletContextModel} relationship
 * is handled by {@code org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry}. And
 * {@code org.apache.felix.http.base.internal.registry.HandlerRegistry#registrations} is sorted using 3 criteria:<ul>
 *     <li>context path length: longer path, higher priority</li>
 *     <li>service rank: higher rank, higher priority</li>
 *     <li>service id: higher id, lower priority</li>
 * </ul></p>
 *
 * <p><em>Shadowing</em> {@link OsgiContextModel} (see
 * {@link org.osgi.service.http.runtime.dto.DTOConstants#FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE}) can happen
 * <strong>only</strong> when there's name/id conflict, so:<ul>
 *     <li>When there are two contexts with same name and different context path, one is chosen (using ranking)
 *     - that's the way to override {@code default} context, for example by changing its context path</li>
 *     <li>When there are two contexts with different name and same context path, both are used, because there may
 *     be two Whiteboard servlets registered, associated with both OSGi contexts</li>
 *     <li>If one servlet is associated with two {@link OsgiContextModel} pointing to the same context path, only
 *     one should be used (registered into actual server-specific {@link ServletContext} - again, according to service
 *     ranking</li>
 *     <li>At actual server runtime level, each servlet is associated (through {@link ServletConfig#getServletContext()})
 *     with <em>own</em> {@link OsgiContextModel}, but there are things to do before actual request processing - like
 *     calling {@link javax.servlet.ServletContainerInitializer#onStartup(Set, ServletContext)} methods. Here, the
 *     {@link OsgiContextModel} passed to such calls is the highest ranked {@link OsgiContextModel} /
 *     {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext} which may be different that the context
 *     associated with the servlets running in such context.</li>
 *     <li>When calling {@code registerXXX} methods on {@link org.ops4j.pax.web.service.WebContainer}, by default (or
 *     explicitly) some bundle-scoped instance of {@link HttpContext} is used (mapped internally again to some
 *     {@link OsgiContextModel}, but when Whiteboard is enabled, the {@link OsgiContextModel} that is highest-ranked
 *     for given {@link ServletContext} (and particular context path) is usually the "default" {@link OsgiContextModel}
 *     created for Whiteboard (shared by default and associated with pax-web-extender-whiteboard bundle, which may
 *     not be the best source of e.g., resources loaded through {@link ServletContextHelper#getResource(String)}
 *     until a bundle-scoped instance of {@link ServletContextHelper} is created for a bundle that registers some
 *     web elements.</li>
 * </ul></p>
 *
 * <p>{@link OsgiContextModel} may represent legacy (Http Service specification) <em>context</em> and standard
 * (Whiteboard Service specification) <em>context</em>. If it's created (<em>customized</em>) for {@link HttpContext}
 * (registered directly or via {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}) and if it's a
 * singleton, then such {@link OsgiContextModel} is equivalent to one created directly through
 * {@link org.osgi.service.http.HttpService} and user may continue to register servlets via
 * {@link org.osgi.service.http.HttpService} to such contexts. That's the way to change the context path of such
 * context. Without any additional steps, the servlets (and filters and resources) registered through
 * {@link org.ops4j.pax.web.service.WebContainer} will <strong>always</strong> be associated with {@link OsgiContextModel}
 * that is lower ranked than the "default" {@link OsgiContextModel} coming from Whiteboard.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.3.0, December 29, 2007
 */
public final class OsgiContextModel extends Identity implements Comparable<OsgiContextModel> {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiContextModel.class);

	/** The singleton {@link OsgiContextModel} used both by pax-web-runtime and pax-web-extender-whiteboard */
	public static final OsgiContextModel DEFAULT_CONTEXT_MODEL;

	static {
		// bundle that "registered" the default ServletContextHelper according to "140.2 The Servlet Context"
		// it's not relevant, because the actual ServletContextHelper will be bound to the bundle for which
		// actual servlet was registered.
		Bundle bundle = FrameworkUtil.getBundle(OsgiContextModel.class);

		// in Whiteboard, rank of "default" context is 0, so it can be overriden by any service ranked higher than 0
		// "140.4 Registering Servlets":
		//     The Servlet Context of the Http Service is treated in the same way as all contexts managed by the
		//     Whiteboard implementation. The highest ranking is associated with the context of the Http Service.
		DEFAULT_CONTEXT_MODEL = new OsgiContextModel(bundle, 0, 0L, true);
		DEFAULT_CONTEXT_MODEL.setName(HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		DEFAULT_CONTEXT_MODEL.setContextPath(PaxWebConstants.DEFAULT_CONTEXT_PATH);

		// the "instance" of the ServletContextHelper will be set as supplier, so it'll depend on the
		// bundle context for which the web element (like servlet) is registered
		// that's the default implementation of "140.2 The Servlet Context" chapter
		// instance of org.osgi.service.http.context.ServletContextHelper will be used. It's abstract, but without
		// any abstract methods
		DEFAULT_CONTEXT_MODEL.setContextSupplier((context, contextName) -> {
			Bundle whiteboardBundle = context == null ? null : context.getBundle();
			return new WebContainerContextWrapper(whiteboardBundle, new DefaultServletContextHelper(whiteboardBundle),
					contextName);
		});

		Hashtable<String, Object> registration = DEFAULT_CONTEXT_MODEL.getContextRegistrationProperties();
		registration.clear();
		// We pretend that this ServletContextModel was:
		//  - registered to represent the Whiteboard's "default" context (org.osgi.service.http.context.ServletContextHelper)
		registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		//  - NOT registered to represent the HttpService's "default" context (org.osgi.service.http.HttpContext)
		registration.remove(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY);
		registration.put(Constants.SERVICE_ID, DEFAULT_CONTEXT_MODEL.getServiceId());
		registration.put(Constants.SERVICE_RANKING, DEFAULT_CONTEXT_MODEL.getServiceRank());
		//  - registered with "/" context path
		registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, PaxWebConstants.DEFAULT_CONTEXT_PATH);

		// no context.init.* properties
		DEFAULT_CONTEXT_MODEL.getContextParams().clear();
	}

	/**
	 * <p>1:1 mapping to <em>server specific</em> {@link ServletContextModel}. Though we don't need entire model,
	 * especially when we don't have all the data required for {@link ServletContextModel}, so we keep only
	 * the context path - a <em>key</em> to {@link ServletContextModel}.</p>
	 *
	 * <p>The context path for ROOT context is {@code "/"}, not {@code ""}.</p>
	 */
	private String contextPath;

	/** If this name is set, it'll be used in associated {@link WebContainerContext} */
	private String name = null;

	/**
	 * <p>Actual OSGi-specific <em>context</em> (can be {@link HttpContext} or
	 * {@link ServletContextHelper} wrapper) that'll be used by {@link ServletContext}
	 * object visible to web elements associated with this OSGi-specific context.</p>
	 *
	 * <p>If this context {@link WebContainerContext#isShared() allows sharing}, {@link OsgiContextModel} can be
	 * populated by different bundles, but still, the helper {@link HttpContext} or
	 * {@link ServletContextHelper} comes from single bundle that has <em>started</em>
	 * configuration/population of given {@link OsgiContextModel}.</p>
	 *
	 * <p>This context may not be set directly. If it's {@code null}, then {@link #resolveHttpContext(Bundle)}
	 * should <em>resolve</em> the {@link WebContainerContext} on each call - to bind returned context with proper
	 * bundle.</p>
	 */
	private WebContainerContext httpContext;

	/**
	 * <p>When a <em>context</em> is registered as Whiteboard service, we have to keep the reference here, because
	 * actual service may be a {@link org.osgi.framework.ServiceFactory}, so it has to be dereferenced within
	 * the context (...) of actual web element (like Servlet).</p>
	 *
	 * <p>The type of the reference is not known, because it can be:<ul>
	 *     <li>{@link ServletContextHelper} - as specified in Whiteboard Service (the recommended way)</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping} - legacy Pax Web way for legacy
	 *         context</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping} - legacy Pax Web way for new
	 *         context</li>
	 *     <li>{@link HttpContext} - not specified anywhere, but supported...</li>
	 * </ul></p>
	 *
	 * <p>Such reference is <strong>not</strong> used if {@link #httpContext} is provided directly, but even if user
	 * registers {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping} which could provide
	 * {@link HttpContext} directly, we keep the reference to obtain the
	 * {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping} when needed.</p>
	 */
	private ServiceReference<?> contextReference;

	/**
	 * If user registers {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping} or
	 * {@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}, those methods MAY return
	 * {@link org.ops4j.pax.web.service.WebContainerContext} as new instance on each call to relevant {@code get()}
	 * method. In Whiteboard Service specification, everything revolves around passing {@link ServiceReference} around,
	 * here we use {@link Function}, because the resulting {@link WebContainerContext} may (or rather should) be
	 * associated with some {@link BundleContext}.
	 */
	private BiFunction<BundleContext, String, WebContainerContext> contextSupplier;

	/**
	 * Properties used when {@link HttpContext} or {@link ServletContextHelper}
	 * was registered. Used for context selection by any LDAP-style filter.
	 */
	private final Hashtable<String, Object> contextRegistrationProperties = new Hashtable<>();

	/**
	 * <p>Context parameters as defined by {@link ServletContext#getInitParameterNames()} and
	 * represented by {@code <context-param>} elements if {@code web.xml}.</p>
	 *
	 * <p>Keeping the parameters at OSGi-specific <em>context</em> level instead of server-specific <em>context</em>
	 * level allows to access different parameters for servlets registered with different {@link HttpContext} or
	 * {@link ServletContextHelper} while still pointing to the same
	 * {@link ServletContext}.</p>
	 *
	 * <p>These parameters come from {@code context.init.*} service registration properties.</p>
	 */
	private final Map<String, String> contextParams = new HashMap<>();

	/**
	 * When {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext} is created with this
	 * {@link OsgiContextModel}, it should get some initial "attributes" (in addition to "init params") including
	 * (but not limited to) {@link ServletContext#ORDERED_LIBS}.
	 */
	private final Map<String, Object> initialContextAttributes = new HashMap<>();

	/**
	 * <p>Virtual Host List as specified when {@link ServletContextHelper},
	 * {@link HttpContext} or {@link org.ops4j.pax.web.service.whiteboard.ContextMapping} was registered.</p>
	 *
	 * <p>For each VHost from the list, related {@link ServletContextModel} should be added to given VHost.
	 * Empty list means the {@link ServletContextModel} is part of all, including the default (fallback), VHosts.</p>
	 *
	 * <p>Entire implementation of virtual hosts is based on the most flexible solution from Jetty, where a "host"
	 * is not a dedicated object being part of the hierarchy (as in Tomcat), but it's rather a label on the context
	 * (WAR, web application) itself - this allows single web application to be accessed through different
	 * virtual hosts and connectors.</p>
	 */
	private final List<String> virtualHosts = new ArrayList<>();

	/**
	 * Connector names for given context - always handled together with connectors.
	 */
	private final List<String> connectors = new ArrayList<>();

	/**
	 * <p>This is the <em>owner</em> bundle of this <em>context</em>. For {@link org.osgi.service.http.HttpService}
	 * scenario, that's the bundle of bundle-scoped {@link org.osgi.service.http.HttpService} used to create
	 * {@link HttpContext}. For Whiteboard scenario, that's the bundle registering
	 * {@link ServletContextHelper}. For old Pax Web Whiteboard, that can be a
	 * bundle which registered <em>shared</em> {@link HttpContext}.</p>
	 *
	 * <p>This is the most important {@link Bundle} that will be used inside "web context class loader" - the
	 * class loader obtained from {@link ServletContext#getClassLoader()}}, but definitely it won't be the only
	 * bundle used - we'll need the bundle of actual runtime (like pax-web-tomcat) and for example pax-web-jsp bundle.</p>
	 */
	private Bundle ownerBundle;

	/** Registration rank of associated {@link HttpContext} or {@link ServletContextHelper} */
	private int serviceRank = 0;
	/** Registration service.id of associated {@link HttpContext} or {@link ServletContextHelper} */
	private long serviceId = 0L;

	private Boolean isValid;

	/** If there's any failure during the lifetime of the context, we can provide a failure DTO information here. */
	private int dtoFailureCode = -1;

	/** Such model is shared, if underlying {@link WebContainerContext} is shared */
	private Boolean shared = true;

	/** Per OSGi context configuration of JSP engine (taglibs, JSP property groups) */
	private final JspConfigurationModel jspConfiguration = new JspConfigurationModel();

	/** Per OSGi context configuration of sessions - standard config from Servlet spec + Server specific config */
	private final SessionConfigurationModel sessionConfiguration = new SessionConfigurationModel();

	/** Per OSGi context configuration of security - login config, security constraints and security roles */
	private final SecurityConfigurationModel securityConfiguration = new SecurityConfigurationModel();

	/** Context-specific list of URLs for XML descriptors (only Jetty and Tomcat handle them) */
	private final List<URL> serverSpecificDescriptors = new ArrayList<>();

	/** Flag indicating whether this {@link OsgiContextModel} comes from Whiteboard. */
	private final boolean whiteboard;

	/** Flag indicating whether this {@link OsgiContextModel} comes from a WAB. */
	private boolean wab;

	/** Tracks {@link Change unregistration changes} for dynamic servlets/filters/listeners */
	private final List<Change> unregistrations = new ArrayList<>();

	/**
	 * When a model is registered from Whiteboard, we usually do NOT want to wait for the registration to finish.
	 * Internal model manipulation ({@link org.ops4j.pax.web.service.spi.model.ServerModel}) still has to be
	 * synchronized, but we don't want to wait for the registration to complete (maybe only in unit tests).
	 */
	private boolean async = false;

	/**
	 * {@link ClassLoader} may be configured for given {@link OsgiContextModel} in some cases (WAB), but externally
	 * configured {@link ClassLoader} may be created in other scenarios (whiteboard, {@link org.osgi.service.http.HttpService}.
	 */
	private OsgiServletContextClassLoader classLoader = null;

	public OsgiContextModel(Bundle ownerBundle, Integer rank, Long serviceId, boolean whiteboard) {
		this.ownerBundle = ownerBundle;
		this.serviceRank = rank;
		this.serviceId = serviceId;

		contextRegistrationProperties.put(Constants.SERVICE_ID, serviceId);
		contextRegistrationProperties.put(Constants.SERVICE_RANKING, rank);
		this.whiteboard = whiteboard;
	}

	public OsgiContextModel(WebContainerContext httpContext, Bundle ownerBundle, String contextPath,
			boolean whiteboard) {
		this.httpContext = httpContext;
		this.ownerBundle = ownerBundle;
		this.contextPath = contextPath;
		this.whiteboard = whiteboard;
		this.name = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
	}

	@Override
	protected String getIdPrefix() {
		return "OCM";
	}

	/**
	 * <p>This method should be called from Whiteboard infrastructure to really perform the validation and set
	 * <em>isValid</em> flag, which is then used for "Failure DTO" purposes.</li>
	 */
	public boolean isValid() {
		if (isValid == null) {
			try {
				isValid = performValidation();
			} catch (Exception ignored) {
				isValid = false;
			}
		}
		return isValid;
	}

	/**
	 * <p>Perform context-specific validation and throws different exceptions when needed.</p>
	 *
	 * <p>This method should be called in Http Service scenario where we immediately need strong feedback - with
	 * exceptions thrown for all validation problems.</p>
	 *
	 * @return
	 */
	public Boolean performValidation() throws Exception {
		if (name == null || "".equals(name.trim())) {
			if (contextReference != null) {
				LOG.warn("Missing name property for context registered using {} reference", contextReference);
			} else if (httpContext != null) {
				LOG.warn("Missing name property for context {}", httpContext);
			}
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
			return Boolean.FALSE;
		}

		if (contextPath == null || !contextPath.startsWith("/")) {
			if (contextReference != null) {
				LOG.warn("Illegal context path (\"{}\") for context registered using {} reference. Should start with \"/\".",
						contextPath, contextReference);
			} else if (httpContext != null) {
				LOG.warn("Illegal context path (\"{}\") for context {}. Should start with \"/\".",
						contextPath, httpContext);
			}
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
			return Boolean.FALSE;
		}

		dtoFailureCode = -1;
		return Boolean.TRUE;
	}

	/**
	 * <p>This method gets a {@link WebContainerContext} asociated with given model. If the context is not
	 * configured directly, we may need to dereference it if needed.</p>
	 *
	 * <p>It's both obvious and conformant to Whiteboard Service specification, that {@link ServletContextHelper}
	 * may be registered as {@link org.osgi.framework.ServiceFactory} (and <em>default</em> one <em>behaves</em>
	 * exactly like this), so it must be dereferenced within a context of given Whiteboard service's (e.g., Servlet's)
	 * {@link BundleContext}).</p>
	 *
	 * <p>Similar case is in Pax Web, when user registers {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}
	 * or {@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}. Relevant method may create new
	 * instance of the context on each call (but the {@code getHttpContext()}/{@code getServletContextHelper()} doesn't
	 * accept {@link BundleContext}).</p>
	 *
	 * @param bundle
	 * @return
	 */
	public WebContainerContext resolveHttpContext(Bundle bundle) {
		if (httpContext != null) {
			// immediate singleton context
			return httpContext;
		}

		BundleContext bundleContext = bundle != null ? bundle.getBundleContext() : null;
		if (bundleContext == null) {
			throw new IllegalArgumentException("Can't resolve WebContainerContext without Bundle argument");
		}

		if (contextSupplier != null) {
			// HttpContextMapping and ServletContextHelperMapping cases are handled via contextSupplier
			// where we actually can't be sure that apply() will return the same instance on each call, so
			// singleton/bundle/prototype scopes are hidden here
			return contextSupplier.apply(bundleContext, getName());
		}
		if (contextReference != null) {
			LOG.debug("Dereferencing {} for {}", toString(contextReference), bundleContext.getBundle());

			// not handling (via ServiceObjects) prototype-scope, so no need to unget()
			Object context = bundleContext.getService(contextReference);
			if (context instanceof WebContainerContext) {
				// Pax Web specific WebContainerContext
				return (WebContainerContext) context;
			}
			if (context instanceof HttpContext) {
				// the very legacy way, because HttpContext was never designed to be registered as Whiteboard service
				return new WebContainerContextWrapper(bundleContext.getBundle(), (HttpContext) context, name);
			}
			if (context instanceof ServletContextHelper) {
				// the preferred way
				return new WebContainerContextWrapper(bundleContext.getBundle(), (ServletContextHelper) context, name);
			}

			dtoFailureCode = DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
			throw new IllegalStateException("Unsupported Whiteboard service for HttpContext/ServletContextHelper"
					+ " specified");
		}

		dtoFailureCode = DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
		throw new IllegalStateException("No HttpContext/ServletContextHelper configured for " + this);
	}

	private String toString(ServiceReference<?> ref) {
		Long id = (Long) ref.getProperty(Constants.SERVICE_ID);
		return String.format("ServiceReference (id=%d, objectClass=%s)", id, String.join(", ", Utils.getObjectClasses(ref)));
	}

	/**
	 * Call {@link BundleContext#ungetService(ServiceReference)} for given bundle if needed, to release the
	 * {@link WebContainerContext} reference.
	 * @param bundle
	 */
	public void releaseHttpContext(Bundle bundle) {
		if (contextReference == null) {
			return;
		}
		BundleContext context = bundle != null ? bundle.getBundleContext() : null;
		if (context != null) {
			LOG.debug("Ungetting {} for {}", toString(contextReference), context.getBundle());

			context.ungetService(contextReference);
		}
	}

	/**
	 * <p>At {@link OsgiContextModel} level we track a list of {@link Change changes} that represent implicit
	 * unregistrations of dynamic servlets/filters/listeners that may have been added for example inside
	 * {@link javax.servlet.ServletContainerInitializer#onStartup(Set, ServletContext)} method.</p>
	 *
	 * <p>JavaEE doesn't bother with unregistration of such elements, but Pax Web does ;)</p>
	 *
	 * @param unregistration
	 */
	public void addUnregistrationChange(Change unregistration) {
		this.unregistrations.add(unregistration);
	}

	public List<Change> getUnregistrations() {
		return unregistrations;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getContextParams() {
		return contextParams;
	}

	public Map<String, Object> getInitialContextAttributes() {
		return initialContextAttributes;
	}

	public Hashtable<String, Object> getContextRegistrationProperties() {
		return contextRegistrationProperties;
	}

	public List<String> getVirtualHosts() {
		return virtualHosts;
	}

	public List<String> getConnectors() {
		return connectors;
	}

	public Bundle getOwnerBundle() {
		return ownerBundle;
	}

	public void setOwnerBundle(Bundle ownerBundle) {
		this.ownerBundle = ownerBundle;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public int getServiceRank() {
		return serviceRank;
	}

	public void setServiceRank(int serviceRank) {
		this.serviceRank = serviceRank;
	}

	public long getServiceId() {
		return serviceId;
	}

	public void setServiceId(long serviceId) {
		this.serviceId = serviceId;
	}

	public ServiceReference<?> getContextReference() {
		return contextReference;
	}

	public void setContextReference(ServiceReference<?> contextReference) {
		this.contextReference = contextReference;
	}

	public BiFunction<BundleContext, String, WebContainerContext> getContextSupplier() {
		return contextSupplier;
	}

	public void setContextSupplier(BiFunction<BundleContext, String, WebContainerContext> contextSupplier) {
		this.contextSupplier = contextSupplier;
	}

	public void setHttpContext(WebContainerContext httpContext) {
		this.httpContext = httpContext;
	}

	public OsgiServletContextClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * In Whiteboard and HttpService scenarios, {@link OsgiServletContextClassLoader} is created in the wrapper
	 * for actual server runtime (to include the bundle specific to given runtime). But in WAB case, we already
	 * have some set of bundles collected as reachable bundles.
	 * @param classLoader
	 */
	public void setClassLoader(OsgiServletContextClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Checks if this {@link OsgiContextModel} has direct instance (not bound to any {@link Bundle}) of
	 * {@link WebContainerContext}. Such {@link OsgiContextModel} represents the <em>context</em> from the point
	 * of view of Http Service specification (in Whiteboard, <em>context</em> should be obtained from service registry
	 * when needed, because it's recommended to register it as {@link org.osgi.framework.ServiceFactory}).
	 * @return
	 */
	public boolean hasDirectHttpContextInstance() {
		return httpContext != null;
	}

	/**
	 * If caller knows there's a direct reference to {@link WebContainerContext} (wrapping an instance
	 * of {@link HttpContext} or {@link ServletContextHelper}), then we can get this context without "resulution"
	 * process.
	 * @return
	 */
	public WebContainerContext getDirectHttpContextInstance() {
		return httpContext;
	}

	/**
	 * Returns {@code true} if the model represents Whiteboard context ({@link ServletContextHelper}) or
	 * it represents old {@link HttpContext} provided as direct (not a service reference) and is an
	 * instance of {@link org.ops4j.pax.web.service.MultiBundleWebContainerContext}.
	 * @return
	 */
	public boolean isShared() {
		return shared;
	}

	public void setShared(Boolean shared) {
		this.shared = shared;
	}

	public int getDtoFailureCode() {
		return dtoFailureCode;
	}

// --- methods that are used directly from web.xml (or fragment) parsing and from WebContainer methods
	//     related to JSP configuration

	public void addTagLibs(Collection<TaglibDescriptor> tagLibs) {
		// that's trivial, no need to check duplicates for now
		jspConfiguration.getTaglibs().addAll(tagLibs);
	}

	public void addJspPropertyGroupDescriptor(JspPropertyGroupDescriptor descriptor) {
		// because javax.servlet.descriptor.JspConfigDescriptor may contain more property group descriptors, we
		// should implement some kind of identification. So descriptors are "same" if they share at least one
		// URI pattern. In such case the property group is replaced, otherwise it is added.
		for (Iterator<JspPropertyGroupDescriptor> it = jspConfiguration.getJspPropertyGroups().iterator(); it.hasNext(); ) {
			JspPropertyGroupDescriptor existing = it.next();
			boolean match = false;
			for (String p1 : existing.getUrlPatterns()) {
				for (String p2 : descriptor.getUrlPatterns()) {
					if (p1.equals(p2)) {
						match = true;
						break;
					}
				}
				if (match) {
					break;
				}
			}
			if (match) {
				it.remove();
				break;
			}
		}

		// whether some existing property group was removed or not, we'll add new one
		jspConfiguration.getJspPropertyGroups().add(descriptor);
	}

	/**
	 * Method to support {@link ServletContext#getJspConfigDescriptor()}
	 * @return
	 */
	public JspConfigDescriptor getJspConfigDescriptor() {
		// this object may be reconfigured during the lifetime of OsgiContextModel and the OsgiServletContext
		// it is contained in
		return this.jspConfiguration;
	}

	// --- methods invoked during web.xml (or fragment) parsing and from WebContainer's session configuration methods

	public void setSessionTimeout(Integer minutes) {
		this.sessionConfiguration.setSessionTimeout(minutes);
	}

	public void setSessionCookieConfig(SessionCookieConfig config) {
		this.sessionConfiguration.setSessionCookieConfig(config);
	}

	public SessionConfigurationModel getSessionConfiguration() {
		return sessionConfiguration;
	}

	public SecurityConfigurationModel getSecurityConfiguration() {
		return securityConfiguration;
	}

	public List<URL> getServerSpecificDescriptors() {
		return serverSpecificDescriptors;
	}

	/**
	 * A "whiteboard" context can override implicit "httpservice" context. This allows users to override the
	 * context (and its path for example) used in {@link org.osgi.service.http.HttpService} scenario.
	 * @return
	 */
	public boolean isWhiteboard() {
		return whiteboard;
	}

	/**
	 * A "WAB" context is always created by pax-web-extender-war when a WAB is installed and provides dedicated,
	 * direct (no service reference and no supplier) to {@link HttpContext}.
	 * @return
	 */
	public boolean isWab() {
		return wab;
	}

	public void setWab(boolean wab) {
		this.wab = wab;
	}

	public void setAsynchronusRegistration(boolean async) {
		this.async = async;
	}

	public boolean isAsynchronusRegistration() {
		return async;
	}

	@Override
	public String toString() {
		String source = ",";
		if (httpContext != null) {
			source += "context=" + httpContext;
		} else if (contextSupplier != null) {
			source += "context=(supplier)";
		} else if (contextReference != null) {
			source += "ref=" + contextReference;
		}
		return "OsgiContextModel{"
				+ (whiteboard ? "WB" : (wab ? "WAB" : "HS"))
				+ ",id=" + getId()
				+ ",name='" + name
				+ "',path='" + contextPath
				+ (ownerBundle == null ? "',shared=true" : "',bundle=" + ownerBundle.getSymbolicName())
				+ source
				+ "}";
	}

	@Override
	public int compareTo(OsgiContextModel o) {
		// don't compare paths at all! This comparing method should order the OCMs by ranking ONLY

		// reverse check for ranking - higher rank is "first"
		long serviceRank = (long)o.getServiceRank() - (long)this.getServiceRank();
		if (serviceRank != 0) {
			return serviceRank > 0 ? 1 : -1;
		}

		// service ID - lower is "first"
		long serviceId = this.getServiceId() - o.getServiceId();
		if (serviceId != 0L) {
			return (int) serviceId;
		}

		if (isWab() != o.isWab()) {
			// the WAB should ALWAYS win, because Whiteboard and HttpService work on existing instances
			// while WAB does a lot of classloading during initialization
			// see https://github.com/ops4j/org.ops4j.pax.web/issues/1725 where it was described
			return isWab() ? -1 : 1;
		}

		// fallback case - mostly in tests cases
		return this.getNumericId() - o.getNumericId();
	}

	/**
	 * Each {@link OsgiContextModel} should have separate "working directory" and this method returns such relative
	 * path depending on {@link #contextPath} and {@link #getName()}
	 * @return
	 */
	public String getTemporaryLocation() {
		String name = getName();
		name = name.replace("/", "_");
		name = name.replace("\\", "_");
		name = name.replace(":", "_");
		return String.format("%s/%s", "/".equals(contextPath) ? "ROOT" : contextPath.substring(1), name);
	}

	/**
	 * Called on demand when someone wants to create {@link ServletContextDTO successful DTO} from this
	 * {@link OsgiContextModel}
	 * @return
	 */
	public ServletContextDTO toDTO() {
		ServletContextDTO scDTO = new ServletContextDTO();
		scDTO.name = getName();
		scDTO.contextPath = getContextPath();
		scDTO.serviceId = getServiceId();
		// they're kept at OsgiServletContext, not OsgiContextModel level
		scDTO.attributes = new HashMap<>();
		// but I can include the initial attributes
		scDTO.attributes.putAll(getInitialContextAttributes());
		scDTO.initParams = new HashMap<>();
		scDTO.initParams.putAll(getContextParams());
		return scDTO;
	}

	/**
	 * Called on demand when someone wants to create {@link FailedServletContextDTO failed DTO} from this
	 * {@link OsgiContextModel}, specifying a reason of failure.
	 * @param failureReason
	 * @return
	 */
	public FailedServletContextDTO toFailedDTO(int failureReason) {
		FailedServletContextDTO scDTO = new FailedServletContextDTO();
		scDTO.name = getName();
		scDTO.contextPath = getContextPath();
		scDTO.serviceId = getServiceId();
		// they're kept at OsgiServletContext, not OsgiContextModel level
		scDTO.attributes = new HashMap<>();
		// but I can include the initial attributes
		scDTO.attributes.putAll(getInitialContextAttributes());
		scDTO.initParams = new HashMap<>();
		scDTO.initParams.putAll(getContextParams());
		scDTO.failureReason = failureReason;
		return scDTO;
	}

}
