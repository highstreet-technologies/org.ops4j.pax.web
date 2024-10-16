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

package org.ops4j.pax.web.itest.undertow.war;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.war.AbstractWebAppSessionCookieIntegrationTest;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class WebAppSessionCookieIntegrationTest extends AbstractWebAppSessionCookieIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebUndertow());
		Option[] infraOptions = combine(serverOptions, configAdmin());
		Option[] sessionOptions = combine(infraOptions,
				systemProperty("org.ops4j.pax.web.session.cookie.name").value("JSID"));
		return combine(sessionOptions, paxWebExtenderWar());
	}

}
