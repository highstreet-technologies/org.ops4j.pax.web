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
package org.ops4j.pax.web.itest.jetty.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class JettyConfigurationIntegrationTest extends AbstractContainerTestBase {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		// this will install a fragment attached to pax-web-jetty bundle, so it can find "jetty.xml" resource
		// used to alter the Jetty server
		MavenArtifactProvisionOption config = mavenBundle("org.ops4j.pax.web.samples", "config-fragment-jetty")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		Option[] authOptions = combine(serverOptions, config);
		return combine(authOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/test");
		});
	}

	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	@Test
	public void testServerVersion() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Response must not contain a jetty version", header ->
						header.map(entry -> entry.getKey()).allMatch(s -> !s.equalsIgnoreCase("Server"))
				)
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	@Test
	public void testWebIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test/wc/example");
	}

	@Test
	public void testWebJettyIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test/wc/example");
	}

	@Test
	public void testWebJetty() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	@Test
	public void testStaticContent() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<a href=\"/static-content/'",
						resp -> resp.contains("<a href=\"/static-content/"))
				.doGETandExecuteTest("http://localhost:8181/static-content/");
	}

}
