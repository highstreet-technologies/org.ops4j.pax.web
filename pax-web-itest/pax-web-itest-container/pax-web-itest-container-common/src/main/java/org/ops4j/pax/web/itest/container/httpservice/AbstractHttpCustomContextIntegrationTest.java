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
package org.ops4j.pax.web.itest.container.httpservice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractHttpCustomContextIntegrationTest extends AbstractContainerTestBase {

	private Bundle installBundle;

	@Before
	public void setUp() throws Exception {
		configureAndWait(() -> installBundle = installAndStartBundle(sampleURI("http-custom-context")), events -> {
			return events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getUrlPatterns()[0].equals("/www/*")
					&& usesContexts(e.getData(), "custom"));
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (installBundle != null) {
			installBundle.stop();
			installBundle.uninstall();
		}
	}

	@Test
	public void testRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Session:'", resp -> resp.contains("Session:"))
				.doGETandExecuteTest("http://127.0.0.1:8181/c/s");
		// test image-serving
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/c/www/logo.png");
	}

}
