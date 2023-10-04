package com.daiming.demo1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaValidateService {

    public ProcessingReport validate(JsonNode planNode, JsonNode schemaNode) throws ProcessingException {
        JsonSchema jsonSchema = JsonSchemaFactory.byDefault().getJsonSchema(schemaNode);
        return jsonSchema.validate(planNode);
    }
}
