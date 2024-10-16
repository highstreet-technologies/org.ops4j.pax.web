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
package org.ops4j.pax.web.itest.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author anierbeck
 */
public class VersionUtils {

	private static final String PROJECT_VERSION;
	private static final String MY_FACES_VERSION;
	private static final String KARAF_VERSION;

	static {
		try {
			final InputStream is = VersionUtils.class.getClassLoader().getResourceAsStream("version.properties");
			if (is != null) {
				final Properties properties = new Properties();
				properties.load(is);
				PROJECT_VERSION = properties.getProperty("version.pax-web", "").trim();
				MY_FACES_VERSION = properties.getProperty("version.myfaces", "").trim();
				KARAF_VERSION = properties.getProperty("version.karaf", "").trim();
			} else {
				PROJECT_VERSION = null;
				MY_FACES_VERSION = null;
				KARAF_VERSION = null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private VersionUtils() {
	}

	public static String getProjectVersion() {
		return PROJECT_VERSION;
	}

	public static String getMyFacesVersion() {
		return MY_FACES_VERSION;
	}

	public static String getKarafVersion() {
		return KARAF_VERSION;
	}

}
