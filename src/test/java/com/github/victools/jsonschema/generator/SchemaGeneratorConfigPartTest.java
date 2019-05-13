/*
 * Copyright 2019 VicTools.
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

package com.github.victools.jsonschema.generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link SchemaGeneratorConfigPart} class.
 */
public class SchemaGeneratorConfigPartTest {

    private static final String ORIGIN1 = "value1";
    private static final String ORIGIN2 = "value2";
    private static final String ORIGIN3 = "value3";

    private SchemaGeneratorConfigPart<String> instance;

    @Before
    public void setUp() {
        this.instance = new SchemaGeneratorConfigPart<>();
    }

    @Test
    public void testIgnoreCheck() {
        Assert.assertFalse(this.instance.shouldIgnore(ORIGIN1));

        this.instance.addIgnoreCheck(origin -> ORIGIN1.equals(origin));
        Assert.assertTrue(this.instance.shouldIgnore(ORIGIN1));
        Assert.assertFalse(this.instance.shouldIgnore(ORIGIN2));

        this.instance.addIgnoreCheck(origin -> ORIGIN2.equals(origin));
        Assert.assertTrue(this.instance.shouldIgnore(ORIGIN1));
        Assert.assertTrue(this.instance.shouldIgnore(ORIGIN2));
        Assert.assertFalse(this.instance.shouldIgnore(ORIGIN3));
    }

    @Test
    public void testNullableCheck() {
        Assert.assertNull(this.instance.isNullable(ORIGIN1, null));

        Assert.assertSame(this.instance, this.instance.addNullableCheck((origin, type) -> ORIGIN1.equals(origin) ? true : null));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null));
        Assert.assertNull(this.instance.isNullable(ORIGIN2, null));

        Assert.assertSame(this.instance, this.instance.addNullableCheck((origin, type) -> ORIGIN2.equals(origin) ? false : null));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null));
        Assert.assertFalse(this.instance.isNullable(ORIGIN2, null));
        Assert.assertNull(this.instance.isNullable(ORIGIN3, null));

        Assert.assertSame(this.instance, this.instance.addNullableCheck((origin, type) -> ORIGIN1.equals(origin)));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null));
        Assert.assertFalse(this.instance.isNullable(ORIGIN2, null));
        Assert.assertFalse(this.instance.isNullable(ORIGIN3, null));

        Assert.assertSame(this.instance, this.instance.addNullableCheck((origin, type) -> ORIGIN2.equals(origin)));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null));
        Assert.assertTrue(this.instance.isNullable(ORIGIN2, null));
        Assert.assertFalse(this.instance.isNullable(ORIGIN3, null));
    }

    @Test
    public void testTargetTypeOverride() {
        JavaType type1 = new JavaType(Number.class, TypeVariableContext.EMPTY_SCOPE);
        JavaType type2 = new JavaType(BigDecimal.class, TypeVariableContext.EMPTY_SCOPE);
        JavaType type3 = new JavaType(BigInteger.class, TypeVariableContext.EMPTY_SCOPE);

        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type1));

        BiFunction<String, JavaType, JavaType> resolver1 = (origin, type) -> ORIGIN1.equals(origin) && type == type1 ? type2 : null;
        Assert.assertSame(this.instance, this.instance.addTargetTypeOverrideResolver(resolver1));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(ORIGIN1, type1));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type2));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type1));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type2));

        BiFunction<String, JavaType, JavaType> resolver2 = (origin, type) -> ORIGIN1.equals(origin) && type == type1 ? type3 : null;
        Assert.assertSame(this.instance, this.instance.addTargetTypeOverrideResolver(resolver2));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(ORIGIN1, type1));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type2));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type1));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type2));

        BiFunction<String, JavaType, JavaType> resolver3 = (origin, type) -> ORIGIN2.equals(origin) && type == type2 ? type1 : null;
        Assert.assertSame(this.instance, this.instance.addTargetTypeOverrideResolver(resolver3));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(ORIGIN1, type1));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type2));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type1));
        Assert.assertSame(type1, this.instance.resolveTargetTypeOverride(ORIGIN2, type2));
    }

    @Test
    public void testPropertyNameOverride() {
        String name1 = "name1";
        String name2 = "name2";

        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name1));

        BiFunction<String, String, String> resolver1 = (origin, defaultName) -> ORIGIN1.equals(origin) && defaultName.equals(name1) ? name2 : null;
        Assert.assertSame(this.instance, this.instance.addPropertyNameOverrideResolver(resolver1));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(ORIGIN1, name1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name2));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name2));

        BiFunction<String, String, String> resolver2 = (origin, defaultName) -> ORIGIN1.equals(origin) && defaultName.equals(name1) ? "name3" : null;
        Assert.assertSame(this.instance, this.instance.addPropertyNameOverrideResolver(resolver2));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(ORIGIN1, name1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name2));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name2));

        BiFunction<String, String, String> resolver3 = (origin, defaultName) -> ORIGIN2.equals(origin) && defaultName.equals(name2) ? name1 : null;
        Assert.assertSame(this.instance, this.instance.addPropertyNameOverrideResolver(resolver3));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(ORIGIN1, name1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name2));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name1));
        Assert.assertSame(name1, this.instance.resolvePropertyNameOverride(ORIGIN2, name2));
    }

    @Test
    public void testTitle() {
        this.testFirstDefinedValueConfig("title1", "title2",
                this.instance::addTitleResolver, this.instance::resolveTitle);
    }

    @Test
    public void testDescription() {
        this.testFirstDefinedValueConfig("description1", "description2",
                this.instance::addDescriptionResolver, this.instance::resolveDescription);
    }

    @Test
    public void testEnum() {
        this.testFirstDefinedValueConfig(Arrays.asList("val1", "val2"), Arrays.asList("val3"),
                this.instance::addEnumResolver, this.instance::resolveEnum);
    }

    @Test
    public void testStringMinLength() {
        this.testFirstDefinedValueConfig(1, 2,
                this.instance::addStringMinLengthResolver, this.instance::resolveStringMinLength);
    }

    @Test
    public void testStringMaxLength() {
        this.testFirstDefinedValueConfig(3, 4,
                this.instance::addStringMaxLengthResolver, this.instance::resolveStringMaxLength);
    }

    @Test
    public void testStringFormat() {
        this.testFirstDefinedValueConfig("uri", "uri-reference",
                this.instance::addStringFormatResolver, this.instance::resolveStringFormat);
    }

    @Test
    public void testNumberInclusiveMinimum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::addNumberInclusiveMinimumResolver, this.instance::resolveNumberInclusiveMinimum);
    }

    @Test
    public void testNumberExclusiveMinimum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::addNumberExclusiveMinimumResolver, this.instance::resolveNumberExclusiveMinimum);
    }

    @Test
    public void testNumberInclusiveMaximum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::addNumberInclusiveMaximumResolver, this.instance::resolveNumberInclusiveMaximum);
    }

    @Test
    public void testNumberExclusiveMaximum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::addNumberExclusiveMaximumResolver, this.instance::resolveNumberExclusiveMaximum);
    }

    @Test
    public void testNumberMultipleOf() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::addNumberMultipleOfResolver, this.instance::resolveNumberMultipleOf);
    }

    @Test
    public void testArrayMinItems() {
        this.testFirstDefinedValueConfig(1, 2,
                this.instance::addArrayMinItemsResolver, this.instance::resolveArrayMinItems);
    }

    @Test
    public void testArrayMaxItems() {
        this.testFirstDefinedValueConfig(1, 2,
                this.instance::addArrayMaxItemsResolver, this.instance::resolveArrayMaxItems);
    }

    @Test
    public void testArrayUniqueItems() {
        this.testFirstDefinedValueConfig(Boolean.TRUE, Boolean.FALSE,
                this.instance::addArrayUniqueItemsResolver, this.instance::resolveArrayUniqueItems);
    }

    private <R> void testFirstDefinedValueConfig(R value1, R value2, Function<BiFunction<String, JavaType, R>, SchemaGeneratorConfigPart<String>> addConfig, BiFunction<String, JavaType, R> resolveValue) {
        Assert.assertNull(resolveValue.apply(ORIGIN1, null));

        Assert.assertSame(this.instance, addConfig.apply((origin, type) -> ORIGIN1.equals(origin) ? value1 : null));
        Assert.assertEquals(value1, resolveValue.apply(ORIGIN1, null));
        Assert.assertNull(resolveValue.apply(ORIGIN2, null));

        Assert.assertSame(this.instance, addConfig.apply((origin, type) -> ORIGIN1.equals(origin) ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(ORIGIN1, null));
        Assert.assertNull(resolveValue.apply(ORIGIN2, null));
        Assert.assertNull(resolveValue.apply(ORIGIN3, null));

        Assert.assertSame(this.instance, addConfig.apply((origin, type) -> ORIGIN2.equals(origin) ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(ORIGIN1, null));
        Assert.assertEquals(value2, resolveValue.apply(ORIGIN2, null));
        Assert.assertNull(resolveValue.apply(ORIGIN3, null));
    }
}
