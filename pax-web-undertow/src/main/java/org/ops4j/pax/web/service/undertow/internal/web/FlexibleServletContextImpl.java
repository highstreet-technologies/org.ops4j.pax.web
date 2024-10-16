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
package org.ops4j.pax.web.service.undertow.internal.web;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.spec.ServletContextImpl;

/**
 * Special {@link ServletContextImpl} implementation to make it easier to reuse
 * {@link io.undertow.servlet.handlers.DefaultServlet} for resource handling.
 */
public class FlexibleServletContextImpl extends ServletContextImpl {

	public FlexibleServletContextImpl(Deployment deployment) {
		super(null, deployment);
	}

}
