package com.o19s.payloads;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.util.TestHarness;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

public class TestPayloadComponent extends SolrTestCaseJ4 {
    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        // if you override setUp or tearDown, you better call
        // the super classes version
        clearIndex();
        super.tearDown();
    }

    @Test
    public void testPayloads() {
        // Add a sample doc
        assertU(adoc("content_payload", "Quick|testpayload brown fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify payload component functionality",
                sumLRF.makeRequest("quick"),
                "//arr[@name='quick']/str[.='testpayload']");
    }

    @Test
    public void testBase64Encoder() {
        // Add a sample doc
        assertU(adoc("content_payload_b64", "Quick|SSBsb3ZlIHBheWxvYWRzIQ== brown fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("df", "content_payload_b64");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify Base64 payload is decoded correctly",
                sumLRF.makeRequest("quick"),
                "//arr[@name='quick']/str[.='I love payloads!']");
    }

    /**
     * As of Solr 7.1 the WDF strips payloads if a token contains any punctuation.
     * This test verifies that behavior and when it stops working it hopefully means the issue is resolved in Solr!
     */
    @Test
    public void testPayloadStrip() {
        // Add a sample doc
        assertU(adoc("content_payload", "Quick-stunning|testpayload brown fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify existence of WDF stripping payloads",
                sumLRF.makeRequest("quick stunning"),
                "not(//lst[@name='content_payload']/node())");
    }

    /**
     * This test is the same as testPayloadStrip but it uses this plugins PayloadBufferFilter.
     *
     * The expected behavior is to see the payloads remain even if the WDF splits tokens up.
     */
    @Test
    public void testPayloadBuffer() {
        // Add a sample doc
        assertU(adoc("content_payload_buffered", "Quick-stunning|testpayload amazing,small|second brown|next fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("df", "content_payload_buffered");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        /**
         * testpayload should be applied to quick & stunning as they're part of the same token before the WDF
         * next should be applied to brown
         * fox should have no payload
         */
        assertQ("Verify buffer filter logic",
                sumLRF.makeRequest("quick stunning amazing small brown fox"),
                "//arr[@name='quick']/str[.='testpayload']",
                "//arr[@name='stunning']/str[.='testpayload']",
                "//arr[@name='amazing']/str[.='second']",
                "//arr[@name='small']/str[.='second']",
                "//arr[@name='brown']/str[.='next']",
                "not(//arr[@name='fox'])");
    }

    @Test
    public void testMultipleHits() {
        // Add a sample doc
        assertU(adoc("content_payload_buffered", "one|one one|two one|three",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("df", "content_payload_buffered");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Testing multiple hits on single token",
                sumLRF.makeRequest("one"),
                "//arr[@name='one']/str[.='one']",
                "//arr[@name='one']/str[.='two']",
                "//arr[@name='one']/str[.='three']");
    }

    @Test
    public void testMultivaluedSupport() {
        // Add a sample doc
        assertU(adoc("content_payload_buffered", "one|one",
                "content_payload_buffered", "one|two",
                "content_payload_buffered", "one|three",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("df", "content_payload_buffered");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Testing multivalued support",
                sumLRF.makeRequest("one"),
                "//arr[@name='one']/str[.='one']",
                "//arr[@name='one']/str[.='two']",
                "//arr[@name='one']/str[.='three']");
    }

    @Test
    public void testPhraseMatch() {
        // Add a sample doc
        assertU(adoc("content_payload", "Quick|testpayload brown fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify payload phrase match",
                sumLRF.makeRequest("\"quick brown\""),
                "//arr[@name='quick']/str[.='testpayload']");
    }

    @Test
    public void testPhraseNoMatch() {
        // Add a sample doc
        assertU(adoc("content_payload", "Quick|testpayload brown fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify phrase error doesn't match",
                sumLRF.makeRequest("brown \"quick fox\""),
                "not(//arr[@name='quick']/str[.='testpayload'])");
    }

    @Test
    public void testPhraseExtra() {
        // Add a sample doc
        assertU(adoc("content_payload", "Quick brown fox quick|extra",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify phrase tokens don't match outside quote",
                sumLRF.makeRequest("\"quick brown\""),
                "not(//arr[@name='quick']/str[.='extra'])");
    }

    @Test
    public void testPhraseSlop() {
        // Add a sample doc
        assertU(adoc("content_payload", "Quick|one brown|extra fox|two",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("defType", "edismax");
        args.put("qf", "content_payload");
        args.put("qs", "1");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("", 0, 200, args);

        assertQ("Verify slop works",
                sumLRF.makeRequest("\"quick fox\""),
                "//arr[@name='quick']/str[.='one']",
                "//arr[@name='fox']/str[.='two']");
    }

    @Test
    public void testPunctuation() {
        // Add a sample doc
        assertU(adoc("content_payload_buffered", "Apostrophe's,|apo period.|period comma,|comma junk",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("df", "content_payload_buffered");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Testing punctuation cases",
                sumLRF.makeRequest("apostrophe's period. comma, junk"),
                "//arr[@name='apostrophe']/str[.='apo']",
                "//arr[@name='period']/str[.='period']",
                "//arr[@name='comma']/str[.='comma']",
                "not(//arr[@name='junk'])");
    }
}
