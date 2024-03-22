/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.war.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link org.osgi.framework.BundleActivator} for pax-web-extender-war, which sets all the things up.
 */
public class Activator extends AbstractExtender {

	public static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private static final int DEFAULT_POOL_SIZE = 3;

	/**
	 * WAR Extender context - this is where the Bundles are managed as <em>extensions</em> and interaction with
	 * {@link org.osgi.service.http.HttpService} / {@link org.ops4j.pax.web.service.WebContainer} happens.
	 */
	private WarExtenderContext warExtenderContext;

	private int poolSize = DEFAULT_POOL_SIZE;

	private final Map<Bundle, Extension> extensions = new ConcurrentHashMap<>();

	/** Registration of {@link org.osgi.service.cm.ManagedService} for {@code org.ops4j.pax.web} PID. */
	private ServiceRegistration<?> managedServiceReg;

	private ExecutorService executors;

	@Override
	protected ExecutorService createExecutor() {
		// return null, because we want to be able to replace the executorService, which is private in parent class
		return null;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		String poolSizeValue = context.getProperty(PaxWebConfig.BUNDLE_CONTEXT_PROPERTY_WAR_EXTENDER_THREADS);
		if (poolSizeValue != null && !"".equals(poolSizeValue)) {
			try {
				poolSize = Integer.parseInt(poolSizeValue);
			} catch (NumberFormatException ignored) {
			}
		}

		LOG.info("Configuring WAR extender thread pool. Pool size = {}", poolSize);
		executors = Executors.newScheduledThreadPool(poolSize, new NamedThreadFactory("wab-extender"));

		// let's be explicit - even if asynchronous mode is default. Please do not change this.
		setSynchronous(false);

		// static information which we don't have to obtain for each new WebApplicationEvent.
		WebApplicationEvent.setExtenderBundle(context.getBundle());

		super.start(context);
	}

	@Override
	protected void doStart() {
		LOG.debug("Starting Pax Web WAR Extender");

		// WarExtenderContext (just like WhiteboardExtenderContext) manages the lifecycle of both the bundles
		// being extended and the WebContainer OSGi service where the WARs are to be installed
		warExtenderContext = new WarExtenderContext(getBundleContext(), getExecutors());

		// start tracking the extensions (Bundle -> WebApp)
		startTracking();

		LOG.debug("Pax Web WAR Extender started");
	}

	@Override
	public synchronized void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	@Override
	protected void doStop() {
		LOG.debug("Stopping Pax Web WAR Extender");

		if (managedServiceReg != null) {
			managedServiceReg.unregister();
			managedServiceReg = null;
		}

		if (warExtenderContext != null) {
			// it means we were started in the first place

			// stop tracking the extensions
			stopTracking();

			warExtenderContext.shutdown();
		}

		LOG.debug("Pax Web WAR Extender stopped");
	}

	@Override
	public synchronized ExecutorService getExecutors() {
		return executors;
	}

	@Override
	protected Extension doCreateExtension(final Bundle bundle) {
		Extension extension = warExtenderContext.createExtension(bundle, () -> extensions.remove(bundle));
		if (extension != null) {
			extensions.put(bundle, extension);
		}
		return extension;
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		// prevent confusing "Starting destruction process" for bundles that were never tracked
		Bundle bundle = event.getBundle();
		if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
			if (extensions.containsKey(bundle)) {
				super.bundleChanged(event);
			}
		}
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		// Nothing to do
		if (extensions.containsKey(bundle)) {
			super.removedBundle(bundle, event, object);
		}
	}

	@Override
	protected void debug(Bundle bundle, String msg) {
		if (LOG.isDebugEnabled()) {
			if (bundle == null) {
				LOG.debug("(no bundle): " + msg);
			} else {
				LOG.debug(bundle.getSymbolicName() + "/" + bundle.getVersion() + ": " + msg);
			}
		}
	}

	@Override
	protected void warn(Bundle bundle, String msg, Throwable t) {
		if (bundle == null) {
			if (t != null) {
				LOG.warn("(no bundle): " + msg, t);
			} else {
				LOG.warn("(no bundle): " + msg);
			}
		} else {
			if (t != null) {
				LOG.warn(bundle.getSymbolicName() + "/" + bundle.getVersion() + ": " + msg, t);
			} else {
				LOG.warn(bundle.getSymbolicName() + "/" + bundle.getVersion() + ": " + msg);
			}
		}
	}

	@Override
	protected void error(String msg, Throwable t) {
		if (t != null) {
			LOG.error(msg, t);
		} else {
			LOG.error(msg);
		}
	}

}
