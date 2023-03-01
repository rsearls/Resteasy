/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.resteasy.test.regex;

import jakarta.ws.rs.client.ClientBuilder;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.test.regex.resource.ProxyPathParamRegexResource;
import org.jboss.resteasy.test.regex.resource.RegexInterface;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for RESTEASY-3291
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PathParamRegexTest {

    static ResteasyClient client;

    @Before
    public void setUp() {
        client = (ResteasyClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(PathParamRegexTest.class.getSimpleName());
        war.addClass(RegexInterface.class);
        return TestUtil.finishContainerPrepare(war, null,
         ProxyPathParamRegexResource.class);
    }

    private String generatePathURL(String path) {
        return PortProviderUtil.generateURL(path, PathParamRegexTest.class.getSimpleName());
    }
    /**
     * As a control check that a simple query works.
     */
    @Test
    public void queryControlTest() {
        ResteasyWebTarget target = client.target(generatePathURL("/encoded/query?m=q p"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "QueryParamq%20p", entity);
        response.close();
    }

    /**
     * As a control check that a simple reqex using ? is processed correctly
     */
    @Test
    public void questionMarkControlTest() {
        ResteasyWebTarget target = client.target(generatePathURL("/w"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "simplew", entity);
        response.close();
    }

    /**
     * @tpTestDetails Checks whether question mark in regular expression in second path param is correctly evaluated.
     * @tpPassCrit Expected string is returned
     */
    @Test
    public void testQuestionMarkInMultiplePathParamRegex() {
        ResteasyWebTarget target = client.target(generatePathURL("/xpath/x"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "xpathx", entity);
        response.close();
    }

    /**
     * Path regex expression and query syntax can contain a ?.  Verify both scenarios
     * are handled properly.
     */
    @Test
    public void questionMarkAndQueryTest () {
        ResteasyWebTarget target = client.target(generatePathURL("/regex/query/x/cust"))
                .queryParam("m", "q p");
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "path=x:query=q p", entity);
        response.close();
    }

    /**
     * Test 2 params each using a regex expression that uses a ?.
     */
    @Test
    public void twoRegexQuestionMarkTest () {
        ResteasyWebTarget target = client.target(generatePathURL("/two/xZ/Y"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "lower=xZ:upper=Y", entity);
        response.close();
    }

    /**
     * Verify regex qualifier, *, is handled.
     */
    @Test
    public void asteriskQualiferTest () {
        ResteasyWebTarget target = client.target(generatePathURL("/asterisk/amw/xpath"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "string=amw:path=xpath", entity);
        response.close();
    }

    /**
     * Verify regex qualifier, {}, is handled correctly
     */
    @Test
    public void curlyBracketQualifierTest () {

        ResteasyWebTarget target = client.target(generatePathURL("/bracket/abc/xpath"));
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        assertEquals("Wrong string returned ", "string=abc:path=xpath", entity);
        response.close();
    }

}
