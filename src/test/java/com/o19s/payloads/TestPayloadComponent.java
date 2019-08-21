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
        assertU(adoc("content_ocr", "Quick|testpayload brown fox",
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
}
