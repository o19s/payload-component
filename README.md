# payload-component

This component plugin will return the payloads for terms that matched in your query. You can see it in action as part of this demonstration project: https://github.com/o19s/pdf-discovery-demo/.

Example document:

```
{
  "id": "my sample doc",
  "payload_content": "Look|ignored at this|wow"
}
```

Querying for `payload_content:this` would generate a response like the following:

```
{
  "response":{
    "docs":[
      {
        "id":"my sample doc",
        "payload_content":"Look|ignored at this|wow",
      }
    ]
  },
  "payloads":{
    "my sample_doc":{
      "payload_content":{
        "this":[
          "wow"
        ]
      }
    }
  }
}     
```
Since `wow` was a payload of the `this` token, and `this` was in the query, `wow` comes back in the payloads response.

## Why?
This project was originally conceived as a solution for storing bounding boxes with terms for OCR highlighting.

See it in action at http://github.com/o19s/pdf-discovery-demo.

## Requirements
- Solr 7.x
- A field type that utilizes payloads

## Usage
- Add the payload component to the last-components section of your search handler
- Configure a field type that utilizes payloads
- Pass `pl=on` in your query params for queries in which you want to extract payload matches
- To learn how to configure you Solr review the test `schema.xml` and `solrconfig.xml` at https://github.com/o19s/payload-component/tree/master/src/test/resources/solr/collection1/conf

## Building
Building requires JDK 8 and Maven.  Once you're setup just run:

`mvn package` to generate the latest jar in the target folder.

## Todo
- Support later Solr versions
- Allow for passing in custom fields to match against
- Support some basic term statistics in the response
