# Via an annotation any Interface can be turned into a JAX-WS class

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.mercuria.dali.rest.*;
import com.squareup.javapoet.*;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercuria.dali.annotationprocessors.RestUtils.*;
import static javax.lang.model.SourceVersion.latestSupported;

@SupportedAnnotationTypes({
        "com.mercuria.dali.rest.Rest"
})
public class RestGenerator extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Rest.class)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing Rest services for " + elem.getSimpleName(), elem);
            try {
                if (isInterface((TypeElement) elem)) {
                    generateCode((TypeElement) elem, this.processingEnv.getFiler());
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can only add @Rest to an interface");
                }
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return latestSupported();
    }

    private void generateCode(TypeElement type, Filer filer) throws IOException {

        Elements elementUtils = processingEnv.getElementUtils();
        PackageElement pkg = elementUtils.getPackageOf(type);

        TypeName serviceType = TypeName.get(type.asType());

        Rest rest = type.getAnnotation(Rest.class);

        AnnotationSpec generated = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", RestGenerator.class.getCanonicalName())
                .addMember("date", "$S", ZonedDateTime.now())
                .build();

        ClassName interfaceClassName = ClassName.get(pkg.toString(), type.getSimpleName().toString() + "RestService");
        TypeSpec.Builder jerseyServiceType = createRestServiceInterface(serviceType, type, generated, interfaceClassName);

        ClassName className = ClassName.bestGuess(type.getSimpleName().toString() + "RestServiceImpl");
        TypeSpec.Builder jerseyServiceImplType = createRestServiceImpl(className, serviceType, generated, interfaceClassName);

        if (classExists("com.google.inject.ImplementedBy")) {
            jerseyServiceType.addAnnotation(
                    AnnotationSpec.builder(ClassName.get("com.google.inject", "ImplementedBy")).addMember("value", "$T.class", className).build()
            );
        }

        boolean requiresObjectMapper = false;
        for (Element member : type.getEnclosedElements()) {
            switch (member.getKind()) {
                case METHOD: {
                    ExecutableElement ee = (ExecutableElement) member;
                    if (ee.getModifiers().contains(Modifier.STATIC))
                        continue;
                    if (ee.getModifiers().contains(Modifier.PROTECTED) || ee.getModifiers().contains(Modifier.PRIVATE))
                        continue;

                    String methodName = ee.getSimpleName().toString();
                    String restMethodName = methodName(methodName);

                    String paths = generateMethodPath(ee);

                    Produces producesAnnotation = ee.getAnnotation(Produces.class);

                    TypeName returnType = adaptReturnType(TypeName.get(ee.getReturnType()), producesAnnotation);
                    String pathSpec = "/" + restMethodName + paths;
                    String parametersForCall = ee.getParameters().stream().map(ve1 -> ve1.getSimpleName().toString()).collect(Collectors.joining(", "));

                    MethodSpec.Builder jerseyMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(returnType)
                            .addAnnotations(getFilteredAnnotationSpecs(member.getAnnotationMirrors()));

                    if (ee.getAnnotation(Path.class) == null) {
                        jerseyMethod = jerseyMethod.addAnnotation(AnnotationSpec.builder(Path.class).addMember("value", "$S", pathSpec).build());
                    }

                    MethodSpec.Builder jerseyImplMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(returnType)
                            .addAnnotation(Override.class);

                    if (returnType.withoutAnnotations().equals(ClassName.get(Response.class))) {
                        //result is streamed
                        requiresObjectMapper = true;
                        jerseyImplMethod = jerseyImplMethod.addStatement("return $T.stream(objectMapper, this.service.get().$L($L))", RestHelper.class, methodName, parametersForCall);
                    } else if (isResponseWrapper(returnType)) {
                        jerseyImplMethod = jerseyImplMethod.addStatement("return new $T(this.service.get().$L($L))", ResponseWrapper.class, methodName, parametersForCall);
                    } else {
                        jerseyImplMethod = jerseyImplMethod.addStatement((statementReturns(returnType) ? "return " : "") + "this.service.get().$L($L)", methodName, parametersForCall);
                    }

                    boolean hasBody = false;

                    for (VariableElement ve : ee.getParameters()) {
                        TypeName parameterType = TypeName.get(ve.asType());
                        String parameterName = ve.getSimpleName().toString();
                        ParameterSpec.Builder parameterSpecBuilder = ParameterSpec.builder(parameterType, parameterName)
                                .addAnnotations(getFilteredAnnotationSpecs(ve.getAnnotationMirrors()));

                        boolean doesNotHaveJaxRsAnnotation = ve.getAnnotation(PathParam.class) == null && ve.getAnnotation(QueryParam.class) == null;

                        if (doesNotHaveJaxRsAnnotation && shouldBePathParameter(parameterType, isEnum(ve))) {
                            jerseyMethod = jerseyMethod.addParameter(parameterSpecBuilder
                                    .addAnnotation(AnnotationSpec.builder(PathParam.class).addMember("value", "$S", parameterName).build()).build());
                        } else if (doesNotHaveJaxRsAnnotation && shouldBeQueryParameter(parameterType, isEnum(ve))) {
                            jerseyMethod = jerseyMethod.addParameter(parameterSpecBuilder
                                    .addAnnotation(AnnotationSpec.builder(QueryParam.class).addMember("value", "$S", parameterName).build()).build());
                        } else {
                            hasBody = true;
                            jerseyMethod = jerseyMethod.addParameter(parameterSpecBuilder.build());
                        }
                        jerseyImplMethod = jerseyImplMethod.addParameter(parameterType, parameterName);
                    }

                    if (doesNotHaveJaxRsMethodType(member)) {
                        jerseyMethod = jerseyMethod.addAnnotation(determineJerseyMethodType(methodName, hasBody));
                    }

                    jerseyServiceType = jerseyServiceType.addMethod(jerseyMethod.build());
                    jerseyServiceImplType = jerseyServiceImplType.addMethod(jerseyImplMethod.build());
                }
            }
        }
        jerseyServiceImplType = jerseyServiceImplType.addMethods(createConstructorsTakingService(serviceType, requiresObjectMapper));
        if (requiresObjectMapper) {
            jerseyServiceImplType = jerseyServiceImplType.addField(ObjectMapper.class, "objectMapper", Modifier.FINAL, Modifier.PRIVATE);
        }

        JavaFile jerseyFile = JavaFile.builder(pkg.toString(), jerseyServiceType.build()).build();
        jerseyFile.writeTo(filer);

        JavaFile jerseyImplFile = JavaFile.builder(pkg.toString(), jerseyServiceImplType.build()).build();
        jerseyImplFile.writeTo(filer);
    }

    private boolean doesNotHaveJaxRsMethodType(Element member) {
        return Objects.isNull(member.getAnnotation(POST.class))
                && Objects.isNull(member.getAnnotation(GET.class))
                && Objects.isNull(member.getAnnotation(DELETE.class))
                && Objects.isNull(member.getAnnotation(PUT.class))
                && Objects.isNull(member.getAnnotation(OPTIONS.class));

    }

    private TypeSpec.Builder createRestServiceImpl(ClassName typeName, TypeName serviceType, AnnotationSpec generated, ClassName interfaceClassName) {
        return TypeSpec.classBuilder(typeName)
                .addAnnotation(generated)
                .addSuperinterface(interfaceClassName)
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), serviceType), "service", Modifier.FINAL, Modifier.PRIVATE).build())
                .addAnnotation(AnnotationSpec.builder(Singleton.class).build());
    }

    private TypeSpec.Builder createRestServiceInterface(TypeName serviceType, TypeElement type, AnnotationSpec generated, ClassName interfaceClassName) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(interfaceClassName)
                .addAnnotation(generated)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Produces.class).addMember("value", "$S", "application/json").build())
                .addAnnotation(AnnotationSpec.builder(Consumes.class).addMember("value", "$S", "application/json").build())
                .addAnnotation(AnnotationSpec.builder(RestClient.class).addMember("interfaceClass", "$S", serviceType).build())
                .addAnnotations(getFilteredAnnotationSpecs(type.getAnnotationMirrors()));

        if (type.getAnnotation(Path.class) == null) {
            builder = builder.addAnnotation(AnnotationSpec.builder(Path.class).addMember("value", "$S", "").build());
        }
        if (classExists("io.swagger.annotations.Api")) {
            builder = builder.addAnnotation(ClassName.get("io.swagger.annotations", "Api"));
        }


        return builder;
    }

    private boolean isEnum(VariableElement ve) {
        Element element = processingEnv.getTypeUtils().asElement(ve.asType());
        return element != null && element.getKind() == ElementKind.ENUM;
    }

    private TypeName adaptReturnType(TypeName typeName, Produces producesAnnotation) {
        if (typeName.equals(TypeName.VOID)) {
            return typeName;
        } else {
            if (Objects.nonNull(producesAnnotation) && !Arrays.asList(producesAnnotation.value()).contains(MediaType.APPLICATION_JSON)) {
                //produces something other than json, so we shouldn't wrap
                return typeName;
            }
            if (isStreamedResponse(typeName)) {
                return ClassName.get(Response.class).annotated(
                        AnnotationSpec.builder(Streamed.class).addMember("originalType", "$S", ((ParameterizedTypeName) typeName).typeArguments.get(0).toString()).build());
            }
            if (typeName.isPrimitive()) {
                return ParameterizedTypeName.get(ClassName.get(ResponseWrapper.class), typeName.box())
                        .annotated(AnnotationSpec.builder(Primative.class).build());
            } else {
                return ParameterizedTypeName.get(ClassName.get(ResponseWrapper.class), typeName);
            }

        }
    }

    private boolean isResponseWrapper(TypeName typeName) {
        return typeName instanceof ParameterizedTypeName && ((ParameterizedTypeName) typeName).rawType.equals(ClassName.get(ResponseWrapper.class));
    }

    private boolean isStreamedResponse(TypeName typeName) {
        return typeName instanceof ParameterizedTypeName && ((ParameterizedTypeName) typeName).rawType.equals(ClassName.get(Stream.class));
    }

    private boolean statementReturns(TypeName returnType) {
        return !returnType.equals(TypeName.VOID);
    }

    private List<MethodSpec> createConstructorsTakingService(TypeName serviceType, boolean requiresObjectMapper) {
        MethodSpec.Builder injectedConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), serviceType), "service")
                .addStatement("this.service = service")
                .addAnnotation(AnnotationSpec.builder(Inject.class).build());
        if (requiresObjectMapper) {
            injectedConstructor = injectedConstructor.addParameter(ObjectMapper.class, "objectMapper")
                    .addStatement("this.objectMapper = objectMapper");
        }

        MethodSpec.Builder simpleConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(serviceType, "service")
                .addStatement("this.service = () -> service");
        if (requiresObjectMapper) {
            simpleConstructor = simpleConstructor.addParameter(ObjectMapper.class, "objectMapper")
                    .addStatement("this.objectMapper = objectMapper");
        }
        return Arrays.asList(injectedConstructor.build(), simpleConstructor.build());
    }

    private String methodName(String methodName) {
        if (methodName.startsWith("get")) {
            String withoutGet = methodName.replaceFirst("get", "");
            return Character.toLowerCase(withoutGet.charAt(0)) + withoutGet.substring(1);
        } else {
            return methodName;
        }
    }

    private String generateMethodPath(ExecutableElement ee) {
        String parameterPath = ee.getParameters().stream()
                .filter(ve -> shouldBePathParameter(TypeName.get(ve.asType()), isEnum(ve)))
                .map(ve -> ve.getSimpleName().toString())
                .map(s -> "{" + s + "}")
                .collect(Collectors.joining("/"));
        return parameterPath.length() == 0 ? parameterPath : "/" + parameterPath;
    }

    private static final ImmutableSet<TypeName> PARAMETER_TYPES =
            ImmutableSet.of(
                    ClassName.get(String.class)
            );

    private boolean shouldBePathParameter(TypeName parameterType, boolean isEnum) {
        boolean isPrimitive = parameterType.isPrimitive() || parameterType.isBoxedPrimitive();
        return isPrimitive
                || isEnum
                || PARAMETER_TYPES.contains(parameterType)
                || parameterType instanceof ClassName && ((ClassName) parameterType).packageName().startsWith("java.time");
    }

    private boolean shouldBeQueryParameter(TypeName parameterType, boolean isEnum) {
        return getOptionalType(parameterType).map(parameterType1 -> shouldBePathParameter(parameterType1, isEnum)).orElse(Boolean.FALSE);
    }

    private AnnotationSpec determineJerseyMethodType(String methodName, boolean hasBody) {
        final Class<?> annotationClass;
        String normalizedMethodName = methodName.toLowerCase();
        if (normalizedMethodName.contains("delete") || normalizedMethodName.contains("remove")) {
            annotationClass = DELETE.class;
        } else if (normalizedMethodName.contains("update")) {
            annotationClass = PUT.class;
        } else if (normalizedMethodName.contains("insert") || normalizedMethodName.contains("upsert") || hasBody) {
            annotationClass = POST.class;
        } else {
            annotationClass = GET.class;
        }
        return AnnotationSpec.builder(annotationClass).build();
    }
}

```
