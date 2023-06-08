package com.github.victools.jsonschema.module.jackson

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.victools.jsonschema.generator.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class KotlinJacksonModuleTest {
    data class TestJsonProperty(
            @JsonProperty("my_text") val text: String,
            @JsonProperty("my_number") val number: Int)

    @Test
    fun `naming override in kotlin data class with JsonProperty`() {
        val config = SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinJacksonModule())
                .build()

        val generator = SchemaGenerator(config)
        val result = generator.generateSchema(TestJsonProperty::class.java)
        val propertiesNode = result[config.getKeyword(SchemaKeyword.TAG_PROPERTIES)]
        val propertyNames: MutableSet<String> = TreeSet()
        propertiesNode.fieldNames().forEachRemaining { e: String -> propertyNames.add(e) }
        Assertions.assertTrue(propertyNames.contains("my_text"))
        Assertions.assertTrue(propertyNames.contains("my_number"))
    }

}
