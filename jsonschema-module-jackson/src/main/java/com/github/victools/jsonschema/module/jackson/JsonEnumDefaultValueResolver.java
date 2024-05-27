package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Field;
import java.util.Arrays;

public class JsonEnumDefaultValueResolver {

    public static String apply(TypeScope typeScope) {
        if (typeScope.getType().getErasedType().isEnum()) {
            return Arrays.stream(typeScope.getType().getErasedType().getDeclaredFields())
                    .filter(enumValue -> enumValue.isAnnotationPresent(JsonEnumDefaultValue.class))
                    .findFirst()
                    .map(Field::getName)
                    .orElse(null);
        }
        return null;
    }
}
