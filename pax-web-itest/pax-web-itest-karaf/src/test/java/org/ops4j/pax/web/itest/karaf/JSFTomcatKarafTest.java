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

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author achim
 */
public class JSFTomcatKarafTest extends JSFBaseKarafTest {

	@Configuration
	public Option[] configuration() {
		Option[] serverOptions = combine(tomcatConfig(), jspConfig());
		// only the dependencies, because myfaces jars and commons-* jars are packaged within the WAB
		Option[] jsfOptions = combine(serverOptions, myfacesDependencies());
		return combine(jsfOptions, paxWebExtenderWar());
	}

}
