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

/**
 * <p>Listener interface to observe events related to {@link org.ops4j.pax.web.service.spi.model.elements.ElementModel}
 * registration.</p>
 *
 * <p>Before Pax Web 8 there was only {@code ServletListener}, but now we have more generic mechanism.</p>
 */
public interface WebElementEventListener {

	/**
	 * Notification about registration (failed or successful) and unregistration of any
	 * {@link org.ops4j.pax.web.service.spi.model.elements.ElementModel}
	 * @param event
	 */
	void registrationEvent(WebElementEvent event);

}
