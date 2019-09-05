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

        assertQ("Verify payload component functionality",
                sumLRF.makeRequest("quick stunning"),
                "not(//lst[@name='content_payload']/node())");
    }

    /**
     * This test is the same as testPayloadStrip but it uses this plugins PayloadBufferFilter.
     *
     * The expected behavior is to see the payload remain.
     */
    @Test
    public void testPayloadBuffer() {
        // Add a sample doc
        assertU(adoc("content_payload_buffered", "Quick-stunning|testpayload brown|next fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());

        HashMap<String,String> args = new HashMap<>();
        args.put("df", "content_payload_buffered");
        args.put("pl", "true");

        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory("standard", 0, 200, args);

        assertQ("Verify payload component functionality",
                sumLRF.makeRequest("quick stunning brown fox"),
                "//arr[@name='quick']/str[.='testpayload']",
                "//arr[@name='stunning']/str[.='testpayload']",
                "//arr[@name='brown']/str[.='next']",
                "//arr[@name='fox']/str[.='']");
    }
}
