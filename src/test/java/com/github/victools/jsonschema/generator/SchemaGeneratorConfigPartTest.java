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
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedMember;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
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

    private ResolvedMember<Field> origin1;
    private ResolvedMember<Field> origin2;
    private ResolvedMember<Field> origin3;
    private ResolvedTypeWithMembers declaringType;

    private SchemaGeneratorConfigPart<ResolvedMember<Field>> instance;

    @Before
    public void setUp() {
        this.instance = new SchemaGeneratorConfigPart<>();
        this.origin1 = Mockito.mock(ResolvedMember.class);
        this.origin2 = Mockito.mock(ResolvedMember.class);
        this.origin3 = Mockito.mock(ResolvedMember.class);
        this.declaringType = Mockito.mock(ResolvedTypeWithMembers.class);
    }

    @Test
    public void testInstanceAttributeOverride() {
        InstanceAttributeOverride<ResolvedMember<Field>> instanceOverride = (node, r, t, d, c) -> node.put("$comment", "test");
        Assert.assertSame(this.instance, this.instance.withInstanceAttributeOverride(instanceOverride));

        List<InstanceAttributeOverride<ResolvedMember<Field>>> instanceOverrideList = this.instance.getInstanceAttributeOverrides();
        Assert.assertEquals(1, instanceOverrideList.size());
        Assert.assertSame(instanceOverride, instanceOverrideList.get(0));
    }

    @Test
    public void testIgnoreCheck() {
        Assert.assertFalse(this.instance.shouldIgnore(this.origin1, this.declaringType));

        Assert.assertSame(this.instance, this.instance.withIgnoreCheck((o, p) -> o == this.origin1));
        Assert.assertTrue(this.instance.shouldIgnore(this.origin1, this.declaringType));
        Assert.assertFalse(this.instance.shouldIgnore(this.origin2, this.declaringType));

        Assert.assertSame(this.instance, this.instance.withIgnoreCheck((o, p) -> o == this.origin2));
        Assert.assertTrue(this.instance.shouldIgnore(this.origin1, this.declaringType));
        Assert.assertTrue(this.instance.shouldIgnore(this.origin2, this.declaringType));
        Assert.assertFalse(this.instance.shouldIgnore(this.origin3, this.declaringType));
    }

    @Test
    public void testNullableCheck() {
        Assert.assertNull(this.instance.isNullable(this.origin1, null, this.declaringType));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((o, t, p) -> o == this.origin1 ? true : null));
        Assert.assertTrue(this.instance.isNullable(this.origin1, null, this.declaringType));
        Assert.assertNull(this.instance.isNullable(this.origin2, null, this.declaringType));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((o, t, p) -> o == this.origin2 ? false : null));
        Assert.assertTrue(this.instance.isNullable(this.origin1, null, this.declaringType));
        Assert.assertFalse(this.instance.isNullable(this.origin2, null, this.declaringType));
        Assert.assertNull(this.instance.isNullable(this.origin3, null, this.declaringType));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((o, t, p) -> o == this.origin1));
        Assert.assertTrue(this.instance.isNullable(this.origin1, null, this.declaringType));
        Assert.assertFalse(this.instance.isNullable(this.origin2, null, this.declaringType));
        Assert.assertFalse(this.instance.isNullable(this.origin3, null, this.declaringType));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((o, t, p) -> o == this.origin2));
        Assert.assertTrue(this.instance.isNullable(this.origin1, null, this.declaringType));
        Assert.assertTrue(this.instance.isNullable(this.origin2, null, this.declaringType));
        Assert.assertFalse(this.instance.isNullable(this.origin3, null, this.declaringType));
    }

    @Test
    public void testTargetTypeOverride() {
        ResolvedType type1 = Mockito.mock(ResolvedType.class);
        ResolvedType type2 = Mockito.mock(ResolvedType.class);
        ResolvedType type3 = Mockito.mock(ResolvedType.class);

        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin1, type1, this.declaringType));

        ConfigFunction<ResolvedMember<Field>, ResolvedType, ResolvedType> resolver1 = (o, t, p) -> o == this.origin1 && t == type1 ? type2 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver1));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(this.origin1, type1, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin1, type2, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin2, type1, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin2, type2, this.declaringType));

        ConfigFunction<ResolvedMember<Field>, ResolvedType, ResolvedType> resolver2 = (o, t, p) -> o == this.origin1 && t == type1 ? type3 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver2));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(this.origin1, type1, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin1, type2, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin2, type1, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin2, type2, this.declaringType));

        ConfigFunction<ResolvedMember<Field>, ResolvedType, ResolvedType> resolver3 = (o, t, p) -> o == this.origin2 && t == type2 ? type1 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver3));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(this.origin1, type1, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin1, type2, this.declaringType));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(this.origin2, type1, this.declaringType));
        Assert.assertSame(type1, this.instance.resolveTargetTypeOverride(this.origin2, type2, this.declaringType));
    }

    @Test
    public void testPropertyNameOverride() {
        String name1 = "name1";
        String name2 = "name2";

        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin1, name1, this.declaringType));

        ConfigFunction<ResolvedMember<Field>, String, String> resolver1 = (o, d, p) -> o == this.origin1 && d.equals(name1) ? name2 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver1));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(this.origin1, name1, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin1, name2, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin2, name1, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin2, name2, this.declaringType));

        ConfigFunction<ResolvedMember<Field>, String, String> resolver2 = (o, d, p) -> o == this.origin1 && d.equals(name1) ? "name3" : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver2));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(this.origin1, name1, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin1, name2, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin2, name1, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin2, name2, this.declaringType));

        ConfigFunction<ResolvedMember<Field>, String, String> resolver3 = (o, d, p) -> o == this.origin2 && d.equals(name2) ? name1 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver3));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(this.origin1, name1, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin1, name2, this.declaringType));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(this.origin2, name1, this.declaringType));
        Assert.assertSame(name1, this.instance.resolvePropertyNameOverride(this.origin2, name2, this.declaringType));
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
            Function<ConfigFunction<ResolvedMember<Field>, ResolvedType, R>, SchemaGeneratorConfigPart<ResolvedMember<Field>>> addConfig,
            ConfigFunction<ResolvedMember<Field>, ResolvedType, R> resolveValue) {
        Assert.assertNull(resolveValue.apply(this.origin1, null, this.declaringType));

        Assert.assertSame(this.instance, addConfig.apply((o, t, p) -> o == this.origin1 ? value1 : null));
        Assert.assertEquals(value1, resolveValue.apply(this.origin1, null, this.declaringType));
        Assert.assertNull(resolveValue.apply(this.origin2, null, this.declaringType));

        Assert.assertSame(this.instance, addConfig.apply((o, t, p) -> o == this.origin1 ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(this.origin1, null, this.declaringType));
        Assert.assertNull(resolveValue.apply(this.origin2, null, this.declaringType));
        Assert.assertNull(resolveValue.apply(this.origin3, null, this.declaringType));

        Assert.assertSame(this.instance, addConfig.apply((o, t, p) -> o == this.origin2 ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(this.origin1, null, this.declaringType));
        Assert.assertEquals(value2, resolveValue.apply(this.origin2, null, this.declaringType));
        Assert.assertNull(resolveValue.apply(this.origin3, null, this.declaringType));
    }
}
