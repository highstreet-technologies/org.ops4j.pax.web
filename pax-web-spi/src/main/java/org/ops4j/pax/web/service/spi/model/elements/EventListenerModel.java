/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model.elements;

import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;

import java.util.Arrays;
import java.util.EventListener;
import java.util.LinkedHashSet;
import java.util.Set;

public class EventListenerModel extends ElementModel<EventListener, EventListenerEventData> {

	private EventListener eventListener;

	// According to 140.7 Registering Listeners, there's no requirement to handle prototype-scoped listeners
	// It'd be quite hard to do, because in case of servlets and filters, all the runtimes have specific, extensible
	// (to different degree) "holder" classes with getInstance()/destroy() methods, where we can keep the instance
	// of org.osgi.framework.ServiceObjects.
	// There's usually no "holder" for listeners and also we (in Tomcat and Undertow) create proxied instances
	// of listeners for specific purposes (passing correct servlet context for example), so we explicitly do NOT
	// handle prototype-scope.
	// That's why we can keep single resolved instance of the listener (singleton or registering bundle-scoped)
	// Also, we don't have to unget(), because we cache the instance and it'll be unget() anyway when the bundle
	// is stopped
	private EventListener resolvedListener;

	/**
	 * Flag used for models registered using {@link javax.servlet.ServletContext#addListener}
	 */
	private boolean dynamic = false;

	public EventListenerModel() {
	}

	public EventListenerModel(final EventListener eventListener) {
		this.eventListener = eventListener;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		view.registerListener(this);
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		view.unregisterListener(this);
	}

	@Override
	public EventListenerEventData asEventData() {
		EventListenerEventData eventListenerEventData = new EventListenerEventData(eventListener);
		setCommonEventProperties(eventListenerEventData);
		return eventListenerEventData;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	/**
	 * Method to be called by actual runtime to obtain an instance of the listener. With
	 * servlets and filters it is performed by runtime-specific <em>holder</em> classes which
	 * control the lifecycle of servlets/filters, but here we do it ourselves, as there's no
	 * lifecycle of the listener itself from the point of view of JavaEE Servlets specification.
	 * @return
	 */
	public EventListener resolveEventListener() {
		if (resolvedListener != null) {
			return resolvedListener;
		}
		synchronized (this) {
			if (resolvedListener != null) {
				return resolvedListener;
			}
			if (eventListener != null) {
				resolvedListener = eventListener;
			} else if (getElementSupplier() != null) {
				// check supplier
				resolvedListener = getElementSupplier().get();
			}
			if (getElementReference() != null) {
				BundleContext context = getRegisteringBundle().getBundleContext();
				if (context != null) {
					resolvedListener = context.getService(getElementReference());
				}
			}
			if (resolvedListener == null) {
				dtoFailureCode = DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
			} else {
				dtoFailureCode = -1;
			}
			return resolvedListener;
		}
	}

	public void releaseEventListener() {
		if (getElementReference() != null) {
			BundleContext context = getRegisteringBundle().getBundleContext();
			if (context != null) {
				context.ungetService(getElementReference());
			}
		}
		resolvedListener = null;
	}

	public EventListener getResolvedListener() {
		return resolvedListener;
	}

	@Override
	public Boolean performValidation() {
		int sources = (eventListener != null ? 1 : 0);
		sources += (getElementReference() != null ? 1 : 0);
		sources += (getElementSupplier() != null ? 1 : 0);
		if (sources == 0) {
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
			throw new IllegalArgumentException("Event Listener Model must specify one of: listener instance,"
					+ " listener supplier or listener reference");
		}
		if (sources != 1) {
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
			throw new IllegalArgumentException("Event Listener Model should specify a listener uniquely as instance,"
					+ " supplier or service reference");
		}

		dtoFailureCode = -1;
		return Boolean.TRUE;
	}

	public ListenerDTO toDTO() {
		ListenerDTO dto = new ListenerDTO();
		// will be set later
		dto.servletContextId = 0L;
		dto.serviceId = getServiceId();
		Class<?> c = null;
		if (eventListener != null) {
			c = eventListener.getClass();
		} else if (getElementSupplier() != null) {
			c = getElementSupplier().get().getClass();
		} else if (getElementReference() != null) {
			dto.types = Utils.getObjectClasses(getElementReference());
		}
		if (c != null) {
			Set<Class<?>> interfaces = new LinkedHashSet<>();
			while (c != Object.class) {
				interfaces.addAll(Arrays.asList(c.getInterfaces()));
				c = c.getSuperclass();
			}
			dto.types = interfaces.stream().map(Class::getName).distinct().toArray(String[]::new);
		}
		return dto;
	}

	public FailedListenerDTO toFailedDTO(int dtoFailureCode) {
		FailedListenerDTO dto = new FailedListenerDTO();
		dto.servletContextId = 0L;
		dto.serviceId = getServiceId();
		Class<?> c = null;
		if (eventListener != null) {
			c = eventListener.getClass();
		} else if (getElementSupplier() != null) {
			c = getElementSupplier().get().getClass();
		} else if (getElementReference() != null) {
			dto.types = Utils.getObjectClasses(getElementReference());
		}
		if (c != null) {
			Set<Class<?>> interfaces = new LinkedHashSet<>();
			while (c != Object.class) {
				interfaces.addAll(Arrays.asList(c.getInterfaces()));
				c = c.getSuperclass();
			}
			dto.types = interfaces.stream().map(Class::getName).distinct().toArray(String[]::new);
		}
		dto.failureReason = dtoFailureCode;
		return dto;
	}

	@Override
	public String toString() {
		return "EventListenerModel{id=" + getId()
				+ (eventListener != null ? ",listener='" + eventListener + "'" : "")
				+ (getElementSupplier() != null ? ",supplier='" + getElementSupplier() + "'" : "")
				+ (getElementReference() != null ? ",reference='" + getElementReference() + "'" : "")
				+ ",contexts=" + getContextModelsInfo()
				+ "}";
	}
}
