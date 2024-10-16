/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.spi.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PathTest {

	/**
	 * Tests that a request with null with not crash with NPE and it returns
	 * null.
	 */
	@Test
	public void replaceSlashesWithNull() {
		assertNull("Replaced", Path.replaceSlashes(null));
	}

	/**
	 * Tests that a request with nothing to be replaced will return the same
	 * string.
	 */
	@Test
	public void replaceSlashes01() {
		assertEquals("Replaced", "/foo/bar/", Path.replaceSlashes("/foo/bar/"));
	}

	/**
	 * Tests that a request that contains only a slash is replaced correctly.
	 */
	@Test
	public void replaceSlashes02() {
		assertEquals("Replaced", "/", Path.replaceSlashes("/"));
	}

	/**
	 * Tests that a request that contains only slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes03() {
		assertEquals("Replaced", "/", Path.replaceSlashes("/////"));
	}

	/**
	 * Tests that a request that starts slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes04() {
		assertEquals("Replaced", "/foo/bar", Path.replaceSlashes("///foo/bar"));
	}

	/**
	 * Tests that a request that ends with slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes05() {
		assertEquals("Replaced", "foo/bar/", Path.replaceSlashes("foo/bar///"));
	}

	/**
	 * Tests that a request that contains slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes06() {
		assertEquals("Replaced", "foo/bar", Path.replaceSlashes("foo////bar"));
	}

	/**
	 * Tests that a request that various cobinations of slashes is replaced
	 * correctly.
	 */
	@Test
	public void replaceSlashes07() {
		assertEquals("Replaced", "/foo/bar/car/", Path.replaceSlashes("/foo////bar//car//"));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" will return
	 * slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash01() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("/"));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" but also
	 * spaces will return slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash02() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("  /"));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" but also
	 * spaces will return slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash03() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("  /   "));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" but also
	 * spaces will return slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash04() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("/   "));
	}

	/**
	 * Test to show that all request access is secure, which means:<ul>
	 *     <li>no way to go out of chroot (no {@code ../../../etc/passwd})</li>
	 *     <li>no duplicate slashes</li>
	 *     <li>no {@code /././././}</li>
	 *     <li>no {@code path1/../path2} (which should be normalized to {@code path1})</li>
	 * </ul>
	 */
	@Test
	public void secureNormalization() {
		assertThat(Path.securePath(null), equalTo(""));
		assertThat(Path.securePath(""), equalTo(""));
		assertThat(Path.securePath("/"), equalTo("/"));
		assertThat(Path.securePath("."), equalTo(""));
		assertThat(Path.securePath(".."), nullValue());
		assertThat(Path.securePath("./."), equalTo(""));
		assertThat(Path.securePath("../.."), nullValue());
		assertThat(Path.securePath("..."), equalTo("..."));
		assertThat(Path.securePath("/a"), equalTo("/a"));
		assertThat(Path.securePath("a"), equalTo("a"));
		assertThat(Path.securePath("a/"), equalTo("a/"));
		assertThat(Path.securePath("a/b"), equalTo("a/b"));
		assertThat(Path.securePath("a/../b"), equalTo("b"));
		assertThat(Path.securePath("a/../b/../c"), equalTo("c"));
		assertThat(Path.securePath("a/../b/../c/../../"), nullValue());
	}

	@Test
	public void urlTest() throws MalformedURLException {
		try {
			System.out.println(new URL(""));
			fail("URL should have protocol");
		} catch (MalformedURLException expected) {
		}
		try {
			System.out.println(new URL("/"));
			fail("URL should have protocol");
		} catch (MalformedURLException expected) {
		}
		URL url;

		url = new URL("file:");
		assertThat(url.getPath(), equalTo(""));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/");
		assertThat(url.getPath(), equalTo("/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file://");
		assertThat(url.getPath(), equalTo(""));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), equalTo(""));

		url = new URL("file:///");
		assertThat(url.getPath(), equalTo("/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), equalTo(""));

		url = new URL("file:////");
		assertThat(url.getPath(), equalTo("////"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file://///");
		assertThat(url.getPath(), equalTo("/////"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a");
		assertThat(url.getPath(), equalTo("a"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a/");
		assertThat(url.getPath(), equalTo("a/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a//");
		assertThat(url.getPath(), equalTo("a//"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a///");
		assertThat(url.getPath(), equalTo("a///"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a////");
		assertThat(url.getPath(), equalTo("a////"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a/////");
		assertThat(url.getPath(), equalTo("a/////"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:a");
		assertThat(url.getPath(), equalTo("a"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/a");
		assertThat(url.getPath(), equalTo("/a"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/a/");
		assertThat(url.getPath(), equalTo("/a/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/a//");
		assertThat(url.getPath(), equalTo("/a//"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/a///");
		assertThat(url.getPath(), equalTo("/a///"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/a////");
		assertThat(url.getPath(), equalTo("/a////"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file:/");
		assertThat(url.getPath(), equalTo("/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file://a");
		assertThat(url.getPath(), equalTo(""));
		assertThat(url.getHost(), equalTo("a"));
		assertThat(url.getAuthority(), equalTo("a"));

		url = new URL("file://a/");
		assertThat(url.getPath(), equalTo("/"));
		assertThat(url.getHost(), equalTo("a"));
		assertThat(url.getAuthority(), equalTo("a"));

		url = new URL("file://a//");
		assertThat(url.getPath(), equalTo("//"));
		assertThat(url.getHost(), equalTo("a"));
		assertThat(url.getAuthority(), equalTo("a"));

		url = new URL("file://a///");
		assertThat(url.getPath(), equalTo("///"));
		assertThat(url.getHost(), equalTo("a"));
		assertThat(url.getAuthority(), equalTo("a"));

		url = new URL("file:/");
		assertThat(url.getPath(), equalTo("/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), nullValue());

		url = new URL("file://");
		assertThat(url.getPath(), equalTo(""));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), equalTo(""));

		url = new URL("file:///a");
		assertThat(url.getPath(), equalTo("/a"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), equalTo(""));

		url = new URL("file:///a/");
		assertThat(url.getPath(), equalTo("/a/"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), equalTo(""));

		url = new URL("file:///a//");
		assertThat(url.getPath(), equalTo("/a//"));
		assertThat(url.getHost(), equalTo(""));
		assertThat(url.getAuthority(), equalTo(""));
	}

	@Test
	public void fileTests() throws Exception {
		System.out.println(new File(new URL("file:/a").toURI()).getAbsoluteFile().toURI().toURL().toString());
		System.out.println(new File(new URL("file:/../a").toURI()).getCanonicalFile().toURI().toURL().toString());
		System.out.println(new File("a").getCanonicalFile().toURI().toURL().getProtocol());
	}

}
