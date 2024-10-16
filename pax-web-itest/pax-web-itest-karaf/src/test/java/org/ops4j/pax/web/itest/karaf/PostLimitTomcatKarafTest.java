/*
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
package org.ops4j.pax.web.itest.karaf;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
public class PostLimitTomcatKarafTest extends PostLimitBaseKarafTest {

	@Configuration
	public Option[] configuration() {
		// this will install a fragment attached to pax-web-tomcat bundle, so it can find "tomcat-server.xml" resource
		// used to configure the Tomcat server
		return combine(tomcatConfig(), mavenBundle("org.ops4j.pax.web.samples", "limit-post-config-fragment-tomcat")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart());
	}

	@Before
	@Override
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-limited-post", () -> {
			// I'm not refreshing, so fragments need to be installed before their hosts
			// this will install a fragment attached to war-limited-post WAB to add Tomcat-specific filter
			// according to https://tomcat.apache.org/tomcat-9.0-doc/config/filter.html#Failed_Request_Filter
			context.installBundle(sampleURI("war-limited-post-fragment-tomcat"));
			installAndStartWebBundle("war-limited-post", "/war-limited-post");
		});
	}

	@Override
	protected int getPostSizeExceededHttpResponseCode() {
		return HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
	}

}
