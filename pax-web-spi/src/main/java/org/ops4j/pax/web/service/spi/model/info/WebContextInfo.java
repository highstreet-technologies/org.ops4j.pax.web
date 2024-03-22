/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.model.info;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * A read-only (or at least "not affecting real model") class to present information about
 * {@link OsgiContextModel}.
 */
public class WebContextInfo {

	private final OsgiContextModel model;

	public WebContextInfo(OsgiContextModel model) {
		this.model = model;
	}

	// keep it package-private
	OsgiContextModel getModel() {
		return model;
	}

}
