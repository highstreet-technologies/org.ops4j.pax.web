/*
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.service.spi.model.events;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * <p>Event related to registration of single {@link org.ops4j.pax.web.service.spi.model.elements.ElementModel} which
 * may represent any <em>web element</em> like {@link Servlet} or {@link javax.servlet.Filter}.
 * In Pax Web 7 there was only a {@code ServletEvent}.</p>
 *
 * <p>While the names may be similar, these events are <strong>not</strong> the events mentioned in chapter 128.5,
 * which defines WAB events. Thus these events are not required to be passed to Event Admin Service.</p>
 *
 * @author Achim Nierbeck
 */
public class WebElementEvent {

	public enum State {
		DEPLOYING, DEPLOYED, UNDEPLOYING, UNDEPLOYED, FAILED, WAITING
	}

	private final boolean replay;

	private final State type;

	private final Bundle bundle;
	private final long bundleId;
	private final String bundleName;
	private final String bundleVersion;

	private final long timestamp;

	private final WebElementEventData data;
	private final Exception exception;

	public WebElementEvent(WebElementEvent event, boolean replay) {
		this.type = event.getType();
		this.bundle = event.getBundle();
		this.bundleId = event.getBundleId();
		this.bundleName = event.getBundleName();
		this.bundleVersion = event.getBundleVersion();
		this.timestamp = event.getTimestamp();
		this.exception = event.exception;

		this.data = event.getData();
		this.replay = replay;
	}

	public WebElementEvent(State type, WebElementEventData data) {
		this(type, data, null);
	}

	public WebElementEvent(State type, WebElementEventData data, Exception exception) {
		this.type = type;
		this.bundle = data.getOriginBundle();
		this.bundleId = bundle.getBundleId();
		this.bundleName = bundle.getSymbolicName();
		this.bundleVersion = bundle.getVersion() == null ? Version.emptyVersion.toString() : bundle.getVersion().toString();

		this.timestamp = System.currentTimeMillis();

		this.data = data;
		this.exception = exception;

		this.replay = false;
	}

	@Override
	public String toString() {
		return String.format("%s (%s/%s): %s%s", type, bundleName, bundleVersion, data.toString(),
				exception == null ? "" : " " + exception.getMessage());
	}

	public boolean isReplay() {
		return replay;
	}

	public State getType() {
		return type;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public long getBundleId() {
		return bundleId;
	}

	public String getBundleName() {
		return bundleName;
	}

	public String getBundleVersion() {
		return bundleVersion;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public WebElementEventData getData() {
		return data;
	}

}
