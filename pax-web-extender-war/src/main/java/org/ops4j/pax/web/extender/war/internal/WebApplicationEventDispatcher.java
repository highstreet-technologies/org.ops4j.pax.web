/*
 * Copyright 2011 Achim Nierbeck.
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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class was inspired by BlueprintEventDispatcher for firing WebEvents.</p>
 *
 * <p>Dispatcher of events related to registration/unregistration of <em>web applications</em>. For events related
 * to particular <em>web elements</em> see {@code org.ops4j.pax.web.service.internal.WebElementEventDispatcher} in
 * pax-web-runtime and {@link WebElementEventListener}.</p>
 *
 * <p>It's activated using a method from {@link WebApplicationEventListener} that called to <em>send</em> the event
 * and the event is passed to other registered {@link WebApplicationEventListener}s.</p>
 */
public class WebApplicationEventDispatcher implements WebApplicationEventListener,
		ServiceTrackerCustomizer<WebApplicationEventListener, WebApplicationEventListener>, BundleListener {

	private static final Logger LOG = LoggerFactory.getLogger(WebApplicationEventDispatcher.class);

	private final BundleContext bundleContext;
	private final ExecutorService executor;

	/** {@link ServiceTracker} for {@link WebApplicationEventListener web app listeners} */
	private final ServiceTracker<WebApplicationEventListener, WebApplicationEventListener> webApplicationListenerTracker;

	/** All tracked {@link WebApplicationEventListener web app listeners} */
	private final Set<WebApplicationEventListener> listeners = new CopyOnWriteArraySet<>();

	public WebApplicationEventDispatcher(final BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		this.executor = Executors.newFixedThreadPool(1, new NamedThreadFactory("wab-events"));

		this.webApplicationListenerTracker = new ServiceTracker<>(bundleContext, WebApplicationEventListener.class.getName(), this);
		this.webApplicationListenerTracker.open();

		this.bundleContext.addBundleListener(this);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
	}

	@Override
	public WebApplicationEventListener addingService(ServiceReference<WebApplicationEventListener> reference) {
		WebApplicationEventListener listener = bundleContext.getService(reference);
		if (listener != null) {
			LOG.debug("New WebApplicationEventListener added: {}", listener.getClass().getName());
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
		return listener;
	}

	@Override
	public void modifiedService(ServiceReference<WebApplicationEventListener> reference, WebApplicationEventListener service) {
	}

	@Override
	public void removedService(ServiceReference<WebApplicationEventListener> reference, WebApplicationEventListener service) {
		listeners.remove(service);
		bundleContext.ungetService(reference);
		LOG.debug("WebApplicationEventListener is removed: {}", service.getClass().getName());
	}

	@Override
	public void webEvent(WebApplicationEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + event + " for bundle " + event.getBundleName());
		}
		synchronized (listeners) {
			callListeners(event);
		}
	}

	void destroy() {
		bundleContext.removeBundleListener(this);
		webApplicationListenerTracker.close();
		executor.shutdown();
		// wait for the queued tasks to execute
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
		}
	}

	/**
	 * Package-private method to register the main listener for WAB conflict management.
	 * @return
	 */
	Set<WebApplicationEventListener> getListeners() {
		return listeners;
	}

	private void callListeners(WebApplicationEvent event) {
		for (WebApplicationEventListener listener : listeners) {
			try {
				callListener(listener, event);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final WebApplicationEventListener listener, final WebApplicationEvent event) {
		try {
			executor.invokeAny(Collections.<Callable<Void>>singleton(() -> {
				listener.webEvent(event);
				return null;
			}), 60L, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			LOG.warn("Thread interrupted", ie);
			Thread.currentThread().interrupt();
		} catch (TimeoutException te) {
			LOG.warn("Listener timed out, will be ignored", te);
			listeners.remove(listener);
		} catch (ExecutionException ee) {
			LOG.warn("Listener caused an exception, will be ignored", ee);
			listeners.remove(listener);
		}
	}

}
