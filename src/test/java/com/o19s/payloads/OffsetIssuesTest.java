package com.o19s.payloads;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OffsetIssuesTest extends SolrTestCaseJ4 {
	

	
    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig-ps.xml", "schema-ps.xml");
    }
    
    @Test
    public void testPayloads() throws Exception {
    	
    	String json = FileUtils.readFileToString(new File("src/test/resources/solr/docs.json"), Charset.defaultCharset());
    	//System.out.println(json);
    	    	
    	updateJ(json( json ),
                params("commit", "true"));
  

    	//assertU(adoc((String[])params.toArray()));
    	
        // Add a sample doc
        assertU(adoc("content_payload", "Quick|testpayload brown fox",
                "id", "1"));
        assertU(commit());
        assertU(optimize());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        // if you override setUp or tearDown, you better call
        // the super classes version
        clearIndex();
        super.tearDown();
    }
}


