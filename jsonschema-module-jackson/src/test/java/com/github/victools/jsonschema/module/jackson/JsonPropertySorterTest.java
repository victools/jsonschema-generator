/*
 * Copyright 2020 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for the {@link JsonPropertySorter}.
 */
public class JsonPropertySorterTest extends AbstractTypeAwareTest {

    private List<MemberScope<?, ?>> memberList;

    public JsonPropertySorterTest() {
        super(AnnotatedTestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
        this.memberList = Arrays.asList(
                this.getTestClassField("x"),
                this.getTestClassMethod("getX"),
                this.getTestClassMethod("getA"),
                this.getTestClassMethod("getB"),
                this.getTestClassMethod("getC"),
                this.getTestClassMethod("getD"),
                this.getTestClassMethod("getE"),
                this.getTestClassMethod("getF"),
                this.getTestClassField("a"),
                this.getTestClassField("b"),
                this.getTestClassField("c"),
                this.getTestClassField("d"),
                this.getTestClassField("e"),
                this.getTestClassField("f"));
    }

    @Test
    public void testPropertySortingAlphabeticallyByDefault() {
        String sortingResult = this.memberList.stream()
                .sorted(new JsonPropertySorter(true))
                .map(MemberScope::getSchemaPropertyName)
                .collect(Collectors.joining(" "));
        Assertions.assertEquals("a c e b f d x getA() getC() getE() getB() getF() getD() getX()", sortingResult);
    }

    @JsonPropertyOrder(value = {"a", "c", "e", "b", "f"}, alphabetic = true)
    private static class AnnotatedTestClass extends TestSuperClass {

        private char x;
        private String a;
        private int b;
        private Boolean d;
        private double f;

        public char getX() {
            return this.x;
        }

        public String getA() {
            return this.a;
        }

        public int getB() {
            return this.b;
        }

        public Boolean getD() {
            return this.d;
        }

        public double getF() {
            return this.f;
        }
    }

    private static class TestSuperClass {

        private Object e;
        private long c;

        public long getC() {
            return this.c;
        }

        public Object getE() {
            return this.e;
        }
    }
}
