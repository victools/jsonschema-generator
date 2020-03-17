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

import com.fasterxml.classmate.ResolvedType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link SchemaGeneratorConfigPart} class.
 */
public class SchemaGeneratorConfigPartTest {

    private FieldScope field1;
    private FieldScope field2;
    private FieldScope field3;

    private SchemaGeneratorConfigPart<FieldScope> instance;

    @Before
    public void setUp() {
        this.instance = new SchemaGeneratorConfigPart<>();
        this.field1 = Mockito.mock(FieldScope.class);
        this.field2 = Mockito.mock(FieldScope.class);
        this.field3 = Mockito.mock(FieldScope.class);
    }

    @Test
    public void testInstanceAttributeOverride() {
        InstanceAttributeOverride<FieldScope> instanceOverride = (node, field) -> node.put("$comment", "test");
        Assert.assertSame(this.instance, this.instance.withInstanceAttributeOverride(instanceOverride));

        List<InstanceAttributeOverride<FieldScope>> instanceOverrideList = this.instance.getInstanceAttributeOverrides();
        Assert.assertEquals(1, instanceOverrideList.size());
        Assert.assertSame(instanceOverride, instanceOverrideList.get(0));
    }

    @Test
    public void testIgnoreCheck() {
        Assert.assertFalse(this.instance.shouldIgnore(this.field1));

        Assert.assertSame(this.instance, this.instance.withIgnoreCheck(member -> member == this.field1));
        Assert.assertTrue(this.instance.shouldIgnore(this.field1));
        Assert.assertFalse(this.instance.shouldIgnore(this.field2));

        Assert.assertSame(this.instance, this.instance.withIgnoreCheck(member -> member == this.field2));
        Assert.assertTrue(this.instance.shouldIgnore(this.field1));
        Assert.assertTrue(this.instance.shouldIgnore(this.field2));
        Assert.assertFalse(this.instance.shouldIgnore(this.field3));
    }

    @Test
    public void testRequiredCheck() {
        Assert.assertFalse(this.instance.isRequired(this.field1));

        Assert.assertSame(this.instance, this.instance.withRequiredCheck(member -> member == this.field1));
        Assert.assertTrue(this.instance.isRequired(this.field1));
        Assert.assertFalse(this.instance.isRequired(this.field2));

        Assert.assertSame(this.instance, this.instance.withRequiredCheck(member -> member == this.field2));
        Assert.assertTrue(this.instance.isRequired(this.field1));
        Assert.assertTrue(this.instance.isRequired(this.field2));
        Assert.assertFalse(this.instance.isRequired(this.field3));
    }

    @Test
    public void testNullableCheck() {
        Assert.assertNull(this.instance.isNullable(this.field1));

        Assert.assertSame(this.instance, this.instance.withNullableCheck(member -> member == this.field1 ? true : null));
        Assert.assertTrue(this.instance.isNullable(this.field1));
        Assert.assertNull(this.instance.isNullable(this.field2));

        Assert.assertSame(this.instance, this.instance.withNullableCheck(member -> member == this.field2 ? false : null));
        Assert.assertTrue(this.instance.isNullable(this.field1));
        Assert.assertFalse(this.instance.isNullable(this.field2));
        Assert.assertNull(this.instance.isNullable(this.field3));

        Assert.assertSame(this.instance, this.instance.withNullableCheck(member -> member == this.field1));
        Assert.assertTrue(this.instance.isNullable(this.field1));
        Assert.assertFalse(this.instance.isNullable(this.field2));
        Assert.assertFalse(this.instance.isNullable(this.field3));

        Assert.assertSame(this.instance, this.instance.withNullableCheck(member -> member == this.field2));
        Assert.assertTrue(this.instance.isNullable(this.field1));
        Assert.assertTrue(this.instance.isNullable(this.field2));
        Assert.assertFalse(this.instance.isNullable(this.field3));
    }

    @Test
    public void testTargetTypeOverride() {
        ResolvedType type1 = Mockito.mock(ResolvedType.class);
        ResolvedType type2 = Mockito.mock(ResolvedType.class);
        ResolvedType type3 = Mockito.mock(ResolvedType.class);

        Assert.assertNull(this.instance.resolveTargetTypeOverrides(this.field1));

        ConfigFunction<FieldScope, ResolvedType> resolver1 = member -> member == this.field1 ? type1 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver1));
        Assert.assertEquals(Collections.singletonList(type1), this.instance.resolveTargetTypeOverrides(this.field1));
        Assert.assertNull(this.instance.resolveTargetTypeOverrides(this.field2));
        Assert.assertNull(this.instance.resolveTargetTypeOverrides(this.field3));

        ConfigFunction<FieldScope, ResolvedType> resolver2 = member -> member == this.field1 ? type2 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver2));
        Assert.assertEquals(Collections.singletonList(type1), this.instance.resolveTargetTypeOverrides(this.field1));
        Assert.assertNull(this.instance.resolveTargetTypeOverrides(this.field2));
        Assert.assertNull(this.instance.resolveTargetTypeOverrides(this.field3));

        ConfigFunction<FieldScope, List<ResolvedType>> resolver3 = member -> member == this.field2 ? Arrays.asList(type2, type3) : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverridesResolver(resolver3));
        Assert.assertEquals(Collections.singletonList(type1), this.instance.resolveTargetTypeOverrides(this.field1));
        Assert.assertEquals(Arrays.asList(type2, type3), this.instance.resolveTargetTypeOverrides(this.field2));
        Assert.assertNull(this.instance.resolveTargetTypeOverrides(this.field3));
    }

    @Test
    public void testPropertyNameOverride() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "name3";

        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.field1));

        ConfigFunction<FieldScope, String> resolver1 = member -> member == this.field1 ? name1 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver1));
        Assert.assertSame(name1, this.instance.resolvePropertyNameOverride(this.field1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.field2));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.field3));

        ConfigFunction<FieldScope, String> resolver2 = member -> member == this.field1 ? name2 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver2));
        Assert.assertSame(name1, this.instance.resolvePropertyNameOverride(this.field1));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.field2));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.field3));

        ConfigFunction<FieldScope, String> resolver3 = member -> member == this.field2 ? name3 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver3));
        Assert.assertSame(name1, this.instance.resolvePropertyNameOverride(this.field1));
        Assert.assertSame(name3, this.instance.resolvePropertyNameOverride(this.field2));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.field3));
    }

    @Test
    public void testTitle() {
        this.testFirstDefinedValueConfig("title1", "title2",
                this.instance::withTitleResolver, this.instance::resolveTitle);
    }

    @Test
    public void testDescription() {
        this.testFirstDefinedValueConfig("description1", "description2",
                this.instance::withDescriptionResolver, this.instance::resolveDescription);
    }

    @Test
    public void testEnum() {
        this.testFirstDefinedValueConfig(Arrays.asList("val1", "val2"), Arrays.asList("val3"),
                this.instance::withEnumResolver, this.instance::resolveEnum);
    }

    @Test
    public void testAdditionalProperties() {
        this.testFirstDefinedValueConfig(String.class, Void.TYPE,
                this.instance::withAdditionalPropertiesResolver, this.instance::resolveAdditionalProperties);
    }

    @Test
    public void testStringMinLength() {
        this.testFirstDefinedValueConfig(1, 2,
                this.instance::withStringMinLengthResolver, this.instance::resolveStringMinLength);
    }

    @Test
    public void testStringMaxLength() {
        this.testFirstDefinedValueConfig(3, 4,
                this.instance::withStringMaxLengthResolver, this.instance::resolveStringMaxLength);
    }

    @Test
    public void testStringFormat() {
        this.testFirstDefinedValueConfig("uri", "uri-reference",
                this.instance::withStringFormatResolver, this.instance::resolveStringFormat);
    }

    @Test
    public void testNumberInclusiveMinimum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::withNumberInclusiveMinimumResolver, this.instance::resolveNumberInclusiveMinimum);
    }

    @Test
    public void testNumberExclusiveMinimum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::withNumberExclusiveMinimumResolver, this.instance::resolveNumberExclusiveMinimum);
    }

    @Test
    public void testNumberInclusiveMaximum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::withNumberInclusiveMaximumResolver, this.instance::resolveNumberInclusiveMaximum);
    }

    @Test
    public void testNumberExclusiveMaximum() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::withNumberExclusiveMaximumResolver, this.instance::resolveNumberExclusiveMaximum);
    }

    @Test
    public void testNumberMultipleOf() {
        this.testFirstDefinedValueConfig(BigDecimal.ONE, BigDecimal.TEN,
                this.instance::withNumberMultipleOfResolver, this.instance::resolveNumberMultipleOf);
    }

    @Test
    public void testArrayMinItems() {
        this.testFirstDefinedValueConfig(1, 2,
                this.instance::withArrayMinItemsResolver, this.instance::resolveArrayMinItems);
    }

    @Test
    public void testArrayMaxItems() {
        this.testFirstDefinedValueConfig(1, 2,
                this.instance::withArrayMaxItemsResolver, this.instance::resolveArrayMaxItems);
    }

    @Test
    public void testArrayUniqueItems() {
        this.testFirstDefinedValueConfig(Boolean.TRUE, Boolean.FALSE,
                this.instance::withArrayUniqueItemsResolver, this.instance::resolveArrayUniqueItems);
    }

    private <R> void testFirstDefinedValueConfig(R value1, R value2,
            Function<ConfigFunction<FieldScope, R>, SchemaGeneratorConfigPart<FieldScope>> addConfig,
            ConfigFunction<FieldScope, R> resolveValue) {
        Assert.assertNull(resolveValue.apply(this.field1));

        Assert.assertSame(this.instance, addConfig.apply(member -> member == this.field1 ? value1 : null));
        Assert.assertEquals(value1, resolveValue.apply(this.field1));
        Assert.assertNull(resolveValue.apply(this.field2));

        Assert.assertSame(this.instance, addConfig.apply(member -> member == this.field1 ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(this.field1));
        Assert.assertNull(resolveValue.apply(this.field2));
        Assert.assertNull(resolveValue.apply(this.field3));

        Assert.assertSame(this.instance, addConfig.apply(member -> member == this.field2 ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(this.field1));
        Assert.assertEquals(value2, resolveValue.apply(this.field2));
        Assert.assertNull(resolveValue.apply(this.field3));
    }
}
