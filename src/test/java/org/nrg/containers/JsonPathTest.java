package org.nrg.containers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.JsonArray;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.json.JsonSmartJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;
import com.jayway.jsonpath.spi.mapper.JsonSmartMappingProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nrg.containers.model.xnat.InnerTestPojo;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonPathTest {
    private final String JSON = "[{\"foo\": \"bar\"}, {\"foo\": \"baz\"}]";
    private final String EQUALS_FILTER = "$.[?(@.foo == %s)].foo";
    private final String IN_FILTER = "$.[?(@.foo in [%s])].foo";
    private final String DOUBLE_QUOTES = "\"bar\"";
    private final String DOUBLE_QUOTES_EQUALS_FILTER = String.format(EQUALS_FILTER, DOUBLE_QUOTES);
    private final String DOUBLE_QUOTES_IN_FILTER = String.format(IN_FILTER, DOUBLE_QUOTES);
    private final String SINGLE_QUOTES = "'bar'";
    private final String SINGLE_QUOTES_EQUALS_FILTER = String.format(EQUALS_FILTER, SINGLE_QUOTES);
    private final String SINGLE_QUOTES_IN_FILTER = String.format(IN_FILTER, SINGLE_QUOTES);


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testJsonPathQuotesJackson() throws Exception {
        final Configuration jackson = Configuration.builder().jsonProvider(new JacksonJsonProvider()).mappingProvider(new JacksonMappingProvider()).build();
        final DocumentContext ctx = JsonPath.using(jackson).parse(JSON);

        final List<String> doubleQuoteEqualsResult = ctx.read(DOUBLE_QUOTES_EQUALS_FILTER);
        assertThat(doubleQuoteEqualsResult, contains("bar"));

        final List<String> singleQuoteEqualsResult = ctx.read(SINGLE_QUOTES_EQUALS_FILTER);
        assertThat(singleQuoteEqualsResult, is(doubleQuoteEqualsResult));

        final List<String> doubleQuoteInResult = ctx.read(DOUBLE_QUOTES_IN_FILTER);
        assertThat(doubleQuoteEqualsResult, is(doubleQuoteInResult));

        exception.expect(InvalidJsonException.class);
        ctx.read(SINGLE_QUOTES_IN_FILTER);
    }


    @Test
    public void testJsonPathQuotesJacksonJsonNode() throws Exception {
        final Configuration jacksonJsonNode = Configuration.builder().jsonProvider(new JacksonJsonNodeJsonProvider()).mappingProvider(new JacksonMappingProvider()).build();
        final DocumentContext ctx = JsonPath.using(jacksonJsonNode).parse(JSON);

        final ArrayNode doubleQuoteEqualsResult = ctx.read(DOUBLE_QUOTES_EQUALS_FILTER);
        assertThat(doubleQuoteEqualsResult.get(0).asText(), is("bar"));

        final ArrayNode singleQuoteEqualsResult = ctx.read(SINGLE_QUOTES_EQUALS_FILTER);
        assertThat(singleQuoteEqualsResult, is(doubleQuoteEqualsResult));

        final ArrayNode doubleQuoteInResult = ctx.read(DOUBLE_QUOTES_IN_FILTER);
        assertThat(doubleQuoteEqualsResult, is(doubleQuoteInResult));

        exception.expect(InvalidJsonException.class);
        ctx.read(SINGLE_QUOTES_IN_FILTER);
    }

    @Test
    public void testJsonPathQuotesGson() throws Exception {
        final Configuration gson = Configuration.builder().jsonProvider(new GsonJsonProvider()).mappingProvider(new GsonMappingProvider()).build();
        final DocumentContext ctx = JsonPath.using(gson).parse(JSON);

        final JsonArray doubleQuoteEqualsResult = ctx.read(DOUBLE_QUOTES_EQUALS_FILTER);
        assertThat(doubleQuoteEqualsResult.get(0).getAsString(), is("bar"));

        final JsonArray singleQuoteEqualsResult = ctx.read(SINGLE_QUOTES_EQUALS_FILTER);
        assertThat(singleQuoteEqualsResult, is(doubleQuoteEqualsResult));

        final JsonArray doubleQuoteInResult = ctx.read(DOUBLE_QUOTES_IN_FILTER);
        assertThat(doubleQuoteEqualsResult, is(doubleQuoteInResult));

        final JsonArray singleQuoteInResult = ctx.read(SINGLE_QUOTES_IN_FILTER);
        assertThat(singleQuoteInResult, is(doubleQuoteInResult));
    }

    @Test
    public void testJsonPathQuotesJsonOrg() throws Exception {
        final Configuration jsonOrg = Configuration.builder().jsonProvider(new JsonOrgJsonProvider()).mappingProvider(new JsonOrgMappingProvider()).build();
        final DocumentContext ctx = JsonPath.using(jsonOrg).parse(JSON);

        final org.json.JSONArray doubleQuoteEqualsResult = ctx.read(DOUBLE_QUOTES_EQUALS_FILTER);
        assertThat((String)doubleQuoteEqualsResult.get(0), is("bar"));

        final org.json.JSONArray singleQuoteEqualsResult = ctx.read(SINGLE_QUOTES_EQUALS_FILTER);
        assertThat(singleQuoteEqualsResult.get(0), is(doubleQuoteEqualsResult.get(0)));

        final org.json.JSONArray doubleQuoteInResult = ctx.read(DOUBLE_QUOTES_IN_FILTER);
        assertThat(doubleQuoteEqualsResult.get(0), is(doubleQuoteInResult.get(0)));

        final org.json.JSONArray singleQuoteInResult = ctx.read(SINGLE_QUOTES_IN_FILTER);
        assertThat(singleQuoteInResult.get(0), is(doubleQuoteInResult.get(0)));
    }

    @Test
    public void testJsonPathQuotesJsonSmart() throws Exception {
        final Configuration jsonSmart = Configuration.builder().jsonProvider(new JsonSmartJsonProvider()).mappingProvider(new JsonSmartMappingProvider()).build();
        final DocumentContext ctx = JsonPath.using(jsonSmart).parse(JSON);

        final net.minidev.json.JSONArray doubleQuoteEqualsResult = ctx.read(DOUBLE_QUOTES_EQUALS_FILTER);
        assertThat((String)doubleQuoteEqualsResult.get(0), is("bar"));

        final net.minidev.json.JSONArray singleQuoteEqualsResult = ctx.read(SINGLE_QUOTES_EQUALS_FILTER);
        assertThat(singleQuoteEqualsResult, is(doubleQuoteEqualsResult));

        final net.minidev.json.JSONArray doubleQuoteInResult = ctx.read(DOUBLE_QUOTES_IN_FILTER);
        assertThat(doubleQuoteEqualsResult, is(doubleQuoteInResult));

        final net.minidev.json.JSONArray singleQuoteInResult = ctx.read(SINGLE_QUOTES_IN_FILTER);
        assertThat(singleQuoteInResult, is(doubleQuoteInResult));
    }

    @Test
    public void testJsonPath() throws Exception {
        final String json = "{\"outerKey1\": {\"innerKey1\": \"value\", \"innerKey2\": \"foo\"}}";

        final Configuration jackson = Configuration.builder().jsonProvider(new JacksonJsonProvider()).mappingProvider(new JacksonMappingProvider()).build();
        final DocumentContext documentContext = JsonPath.using(jackson).parse(json);

        final String definite = documentContext.read("$.outerKey1.innerKey1");
        assertThat(definite, is("value"));

//        final List<String> indefinite = documentContext.read("$..key2");
//        assertThat(indefinite, is(Lists.newArrayList("value")));

        final InnerTestPojo expectedInner = new InnerTestPojo("value", "foo");
        assertThat(documentContext.read("$.outerKey1", new TypeRef<InnerTestPojo>() {}), is(expectedInner));

        final List<String> innerKey = JsonPath.parse(json).read("$.outerKey1[?(@.innerKey2 == 'foo')].innerKey1", new TypeRef<List<String>>() {});
        assertThat(innerKey, contains("value"));
        final List<InnerTestPojo> actualIndefiniteWPredicate = documentContext.read("$.outerKey1[?(@.innerKey2 == 'foo')]", new TypeRef<List<InnerTestPojo>>(){});
        assertThat(actualIndefiniteWPredicate, contains(expectedInner));
        final List<InnerTestPojo> emptyIndefiniteWPredicate = documentContext.read("$.outerKey1[?(@.innerKey2 != 'foo')]", new TypeRef<List<InnerTestPojo>>(){});
        assertThat(emptyIndefiniteWPredicate, is(empty()));
    }
}
