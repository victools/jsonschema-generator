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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    private static final String ORIGIN1 = "value1";
    private static final String ORIGIN2 = "value2";
    private static final String ORIGIN3 = "value3";

    private SchemaGeneratorConfigPart<String> instance;

    @Before
    public void setUp() {
        this.instance = new SchemaGeneratorConfigPart<>();
    }

    @Test
    public void testInstanceAttributeOverride() {
        InstanceAttributeOverride<String> instanceOverride = (node, reference, t, context, config) -> node.put("$comment", reference);
        Assert.assertSame(this.instance, this.instance.withInstanceAttributeOverride(instanceOverride));

        List<InstanceAttributeOverride<String>> instanceOverrideList = this.instance.getInstanceAttributeOverrides();
        Assert.assertEquals(1, instanceOverrideList.size());
        Assert.assertSame(instanceOverride, instanceOverrideList.get(0));
    }

    @Test
    public void testIgnoreCheck() {
        BeanDescription context = Mockito.mock(BeanDescription.class);
        Assert.assertFalse(this.instance.shouldIgnore(ORIGIN1, context));

        Assert.assertSame(this.instance, this.instance.withIgnoreCheck((origin, c) -> ORIGIN1.equals(origin)));
        Assert.assertTrue(this.instance.shouldIgnore(ORIGIN1, context));
        Assert.assertFalse(this.instance.shouldIgnore(ORIGIN2, context));

        Assert.assertSame(this.instance, this.instance.withIgnoreCheck((origin, c) -> ORIGIN2.equals(origin)));
        Assert.assertTrue(this.instance.shouldIgnore(ORIGIN1, context));
        Assert.assertTrue(this.instance.shouldIgnore(ORIGIN2, context));
        Assert.assertFalse(this.instance.shouldIgnore(ORIGIN3, context));
    }

    @Test
    public void testNullableCheck() {
        BeanDescription context = Mockito.mock(BeanDescription.class);
        Assert.assertNull(this.instance.isNullable(ORIGIN1, null, context));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((origin, type, c) -> ORIGIN1.equals(origin) ? true : null));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null, context));
        Assert.assertNull(this.instance.isNullable(ORIGIN2, null, context));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((origin, type, c) -> ORIGIN2.equals(origin) ? false : null));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null, context));
        Assert.assertFalse(this.instance.isNullable(ORIGIN2, null, context));
        Assert.assertNull(this.instance.isNullable(ORIGIN3, null, context));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((origin, type, c) -> ORIGIN1.equals(origin)));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null, context));
        Assert.assertFalse(this.instance.isNullable(ORIGIN2, null, context));
        Assert.assertFalse(this.instance.isNullable(ORIGIN3, null, context));

        Assert.assertSame(this.instance, this.instance.withNullableCheck((origin, type, c) -> ORIGIN2.equals(origin)));
        Assert.assertTrue(this.instance.isNullable(ORIGIN1, null, context));
        Assert.assertTrue(this.instance.isNullable(ORIGIN2, null, context));
        Assert.assertFalse(this.instance.isNullable(ORIGIN3, null, context));
    }

    @Test
    public void testTargetTypeOverride() {
        TypeFactory typeFactory = new ObjectMapper().getSerializationConfig().getTypeFactory();
        JavaType type1 = typeFactory.constructType(Number.class);
        JavaType type2 = typeFactory.constructType(BigDecimal.class);
        JavaType type3 = typeFactory.constructType(BigInteger.class);
        BeanDescription context = Mockito.mock(BeanDescription.class);

        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type1, context));

        ConfigFunction<String, JavaType, JavaType> resolver1 = (origin, type, c) -> ORIGIN1.equals(origin) && type == type1 ? type2 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver1));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(ORIGIN1, type1, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type2, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type1, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type2, context));

        ConfigFunction<String, JavaType, JavaType> resolver2 = (origin, type, c) -> ORIGIN1.equals(origin) && type == type1 ? type3 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver2));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(ORIGIN1, type1, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type2, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type1, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type2, context));

        ConfigFunction<String, JavaType, JavaType> resolver3 = (origin, type, c) -> ORIGIN2.equals(origin) && type == type2 ? type1 : null;
        Assert.assertSame(this.instance, this.instance.withTargetTypeOverrideResolver(resolver3));
        Assert.assertSame(type2, this.instance.resolveTargetTypeOverride(ORIGIN1, type1, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN1, type2, context));
        Assert.assertNull(this.instance.resolveTargetTypeOverride(ORIGIN2, type1, context));
        Assert.assertSame(type1, this.instance.resolveTargetTypeOverride(ORIGIN2, type2, context));
    }

    @Test
    public void testPropertyNameOverride() {
        String name1 = "name1";
        String name2 = "name2";
        BeanDescription context = Mockito.mock(BeanDescription.class);

        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name1, context));

        ConfigFunction<String, String, String> resolver1 = (origin, dn, c) -> ORIGIN1.equals(origin) && dn.equals(name1) ? name2 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver1));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(ORIGIN1, name1, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name2, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name1, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name2, context));

        ConfigFunction<String, String, String> resolver2 = (origin, dn, c) -> ORIGIN1.equals(origin) && dn.equals(name1) ? "name3" : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver2));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(ORIGIN1, name1, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name2, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name1, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name2, context));

        ConfigFunction<String, String, String> resolver3 = (origin, dn, c) -> ORIGIN2.equals(origin) && dn.equals(name2) ? name1 : null;
        Assert.assertSame(this.instance, this.instance.withPropertyNameOverrideResolver(resolver3));
        Assert.assertSame(name2, this.instance.resolvePropertyNameOverride(ORIGIN1, name1, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN1, name2, context));
        Assert.assertNull(this.instance.resolvePropertyNameOverride(ORIGIN2, name1, context));
        Assert.assertSame(name1, this.instance.resolvePropertyNameOverride(ORIGIN2, name2, context));
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
            Function<ConfigFunction<String, JavaType, R>, SchemaGeneratorConfigPart<String>> addConfig,
            ConfigFunction<String, JavaType, R> resolveValue) {
        Assert.assertNull(resolveValue.apply(ORIGIN1, null, null));

        Assert.assertSame(this.instance, addConfig.apply((origin, type, c) -> ORIGIN1.equals(origin) ? value1 : null));
        Assert.assertEquals(value1, resolveValue.apply(ORIGIN1, null, null));
        Assert.assertNull(resolveValue.apply(ORIGIN2, null, null));

        Assert.assertSame(this.instance, addConfig.apply((origin, type, c) -> ORIGIN1.equals(origin) ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(ORIGIN1, null, null));
        Assert.assertNull(resolveValue.apply(ORIGIN2, null, null));
        Assert.assertNull(resolveValue.apply(ORIGIN3, null, null));

        Assert.assertSame(this.instance, addConfig.apply((origin, type, c) -> ORIGIN2.equals(origin) ? value2 : null));
        Assert.assertEquals(value1, resolveValue.apply(ORIGIN1, null, null));
        Assert.assertEquals(value2, resolveValue.apply(ORIGIN2, null, null));
        Assert.assertNull(resolveValue.apply(ORIGIN3, null, null));
    }
}
