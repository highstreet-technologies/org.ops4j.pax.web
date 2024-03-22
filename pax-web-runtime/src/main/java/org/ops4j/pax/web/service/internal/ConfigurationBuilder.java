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
package org.ops4j.pax.web.service.internal;

import java.util.Map;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.util.property.PropertyResolver;

public class ConfigurationBuilder {

	private ConfigurationBuilder() {
	}

	/**
	 * Produces {@link Configuration} object with properties coming from passed {@code resolver}. Returned
	 * {@link Configuration} may be prepopulated (having properties resolved immediately), however, when using
	 * {@link Configuration#get(String, Class)}, resolvers may be used on demand.
	 *
	 * @param resolver a {@link PropertyResolver} to access properties by name, but without a way to get all
	 *        of the properties as dictionary
	 * @param sourceProperties usually available from passed {@code resolver}, but may be useful as Map.
	 * @return
	 */
	public static Configuration getConfiguration(PropertyResolver resolver, Map<String, String> sourceProperties) {
		return new ConfigurationImpl(resolver, sourceProperties);
	}

}
