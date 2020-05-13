# RestUtils

```java
package com.mercuria.dali.annotationprocessors;

import com.mercuria.dali.rest.ResponseWrapper;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class RestUtils {

    private RestUtils() {
    }

    static <A> Optional<Class<? extends A>> classIfExists(String className) {
        try {
            return Optional.of((Class<? extends A>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }

    }

    static boolean classExists(String classname) {
        return classIfExists(classname).isPresent();
    }

    static String joinUrlParts(String part1, String part2) {
        if (part1.isEmpty()) {
            return part2;
        } else if (part2.isEmpty()) {
            return part1;
        } else {
            return (part1 + "/" + part2).replaceAll("/+", "/");
        }
    }

    static List<AnnotationSpec> getFilteredAnnotationSpecs(List<? extends AnnotationMirror> annotationMirrors) {
        return annotationMirrors.stream()
                .map(AnnotationSpec::get)
                .filter(annotationSpec -> !(annotationSpec.type.toString().startsWith("com.mercuria.dali") || annotationSpec.type.toString().startsWith("org.skife.jdbi")))
                .collect(Collectors.toList());
    }

    static boolean isInterface(TypeElement elem) {
        return elem.getSuperclass().getKind() == TypeKind.NONE;
    }

    static Optional<TypeName> getOptionalType(TypeName maybeOptional) {
        if (maybeOptional instanceof ParameterizedTypeName
                && ((ParameterizedTypeName) maybeOptional).rawType.equals(ClassName.get(Optional.class))) {
            return Optional.of(((ParameterizedTypeName) maybeOptional).typeArguments.get(0));
        } else {
            return Optional.empty();
        }
    }

    static boolean isResponseWrapper(TypeName typeName) {
        if (typeName instanceof ParameterizedTypeName) {
            return ((ParameterizedTypeName) typeName).rawType.equals(ClassName.get(ResponseWrapper.class));
        } else {
            return false;
        }
    }

    static boolean isOptional(TypeName returnType) {
        if (returnType instanceof ParameterizedTypeName) {
            return ((ParameterizedTypeName) returnType).rawType.equals(ClassName.get(Optional.class));
        } else {
            return false;
        }
    }
}

```
