package com.github.victools.jsonschema.module.jackson;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JsonPropertyOrderIntegrationTest {

	private static ObjectMapper OBJECT_MAPPER;

	@BeforeAll
	static void beforeAll() {
		OBJECT_MAPPER = new ObjectMapper();
	}

	@Test
	public void testJsonPropertyOrderWithChildAnnotations() throws Exception {
		// given
		final SchemaGenerator generator = new SchemaGenerator(testConfig());
		// when
		JsonNode result = generator.generateSchema(JsonPropertyOrderIntegrationTest.TestObject.class);
		// then
		final String actualSchemaAsString = OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
				.writeValueAsString(result);
		final String expectedSchemaAsString = loadTestJson("jsonpropertyorder-method-with-child-annotations-integration-test-result.json");
		Assertions.assertEquals(expectedSchemaAsString, actualSchemaAsString);
	}

	private static SchemaGeneratorConfig testConfig() {
		final JacksonModule module = new JacksonModule(
				JacksonOption.RESPECT_JSONPROPERTY_ORDER,
				JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS
		);
		return new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
				.with(
						Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
						Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS
				)
				.with(module)
				.build();
	}

	private static String loadTestJson(String resourcePath) throws IOException {
		try (InputStream inputStream = JsonPropertyOrderIntegrationTest.class.getResourceAsStream(resourcePath)) {
			return OBJECT_MAPPER.readTree(inputStream).toPrettyString();
		}
	}

	@JsonPropertyOrder({ "1_first", "3_third" })
	public static class TestContainer {

		@JsonPropertyDescription("My string description")
		@JsonProperty("1_first")
		private String firstString;
		@JsonPropertyDescription("My integer description")
		@JsonProperty("3_third")
		private Integer thirdInteger;
	}

	@JsonPropertyOrder({ "1_first", "2_second" , "3_third"})
	public static class TestObject {

		private TestContainer container;
		private String secondString;

		@JsonIgnore
		public TestContainer getContainer() {
			return container;
		}

		@JsonPropertyDescription("My string description")
		@JsonProperty("1_first")
		public String getString() {
			return container.firstString;
		}

		@JsonPropertyDescription("My second string description")
		@JsonProperty("2_second")
		public String getString2() {
			return secondString;
		}

		@JsonPropertyDescription("My integer description")
		@JsonProperty("3_third")
		public Integer getInteger() {
			return container.thirdInteger;
		}
	}

}
