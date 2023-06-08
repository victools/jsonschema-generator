package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.members.RawField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.MemberScope;
import kotlin.Metadata;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.OptionalInt;

public class KotlinJacksonModule extends JacksonModule {
    /**
     * Look up an alternative name for a member in the constructor parameter list.
     * When the Kotlin compiler compiles a Kotlin data class, it creates a constructor with a parameter
     * for each field in the data class. The parameters have generic name such as "arg0", "arg1", etc.
     * In order to find the appropriate constructor parameter, the method first determines the index of
     * specified member within its type member list and uses the parameter with the same index.
     * In order to avoid erroneous overrides, this method verifies that the specified member is indeed of a
     * Kotlin class.
     *
     * @param member field/method to look-up alternative property name for
     * @return alternative property name or the base class implementation return value
     */
    @Override
    protected String getPropertyNameOverrideBasedOnJsonPropertyAnnotation(MemberScope<?, ?> member) {
        if (isKotlinType(member)) {
            OptionalInt memberIndex = getMemberIndex(member);
            if (memberIndex.isPresent()) {
                Parameter[] parameters = getConstructorParameters(member);
                Parameter parameter = parameters[memberIndex.getAsInt()];
                JsonProperty jsonPropertyAnnotation = parameter.getAnnotation(JsonProperty.class);
                if (jsonPropertyAnnotation != null) {
                    String nameOverride = jsonPropertyAnnotation.value();
                    if (isValidNameOverride(member, nameOverride)) {
                        return nameOverride;
                    }
                }
            }
        }
        return super.getPropertyNameOverrideBasedOnJsonPropertyAnnotation(member);

    }

    private OptionalInt getMemberIndex(MemberScope<?, ?> member) {
        List<RawField> memberFields = member.getDeclaringType().getMemberFields();
        for (int i = 0; i < memberFields.size(); i++) {
            if (memberFields.get(i).getName().equals(member.getName())) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private static Parameter[] getConstructorParameters(MemberScope<?, ?> member) {
        return member.getDeclaringType().getConstructors().get(0).getRawMember().getParameters();
    }

    private static boolean isKotlinType(MemberScope<?, ?> member) {
        return member.getDeclaringType().getErasedType().isAnnotationPresent(Metadata.class);
    }

}
