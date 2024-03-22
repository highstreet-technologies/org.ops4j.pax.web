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
package org.ops4j.pax.web.service.undertow.internal;

import io.undertow.server.session.SecureRandomSessionIdGenerator;

public class PaxWebSessionIdGenerator extends SecureRandomSessionIdGenerator {

	public static ThreadLocal<String> sessionIdPrefix = new ThreadLocal<>();
	public static ThreadLocal<String> cookieSessionId = new ThreadLocal<>();

	@Override
	public String createSessionId() {
		// See also org.ops4j.pax.web.service.jetty.internal.PaxWebSessionIdManager.newSessionId()
		String prefix = sessionIdPrefix.get();
		String cookie = cookieSessionId.get();
		if (prefix == null) {
			return super.createSessionId();
		} else {
			if (cookie != null) {
				// cookie used already and we're creating new session
				cookieSessionId.set(null);
				return prefix + "~" + cookie;
			}
			return prefix + "~" + super.createSessionId();
		}
	}

}
