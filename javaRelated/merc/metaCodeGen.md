# Use of Code Gen and annotations

Pretty involved and not sure how this ties in with retrofit, which is def used see below (retrofitApiMethod)

```java
package com.mercuria.dali.annotationprocessors;

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
import java.lang.annotation.Annotation;
import java.time.ZonedDateTime;
import java.util.*;
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

                    TypeName originalReturnType = TypeName.get(ee.getReturnType());
                    TypeName returnType = adaptReturnType(originalReturnType, producesAnnotation);
                    String pathSpec = "/" + restMethodName + paths;
                    String parametersForCall = ee.getParameters().stream().map(ve1 -> ve1.getSimpleName().toString()).collect(Collectors.joining(", "));

                    MethodSpec.Builder jerseyMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(returnType)
                            .addAnnotations(getFilteredAnnotationSpecs(member.getAnnotationMirrors()));

                    if (doesNotHaveAnnotation(ee, Path.class)) {
                        jerseyMethod = jerseyMethod.addAnnotation(AnnotationSpec.builder(Path.class).addMember("value", "$S", pathSpec).build());
                    }

                    MethodSpec.Builder jerseyImplMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(returnType)
                            .addAnnotation(Override.class);

                    if (isStreamedResponse(originalReturnType)) {
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

                        boolean doesNotHaveJaxRsAnnotation = doesNotHaveAnnotation(ve, PathParam.class) && doesNotHaveAnnotation(ve, QueryParam.class);

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
        return doesNotHaveAnnotation(member, POST.class)
                && doesNotHaveAnnotation(member, GET.class)
                && doesNotHaveAnnotation(member, DELETE.class)
                && doesNotHaveAnnotation(member, PUT.class)
                && doesNotHaveAnnotation(member, OPTIONS.class);

    }

    private boolean doesNotHaveAnnotation(Element element, Class<? extends Annotation> annotationType) {
        return element.getAnnotation(annotationType) == null;
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

        if (doesNotHaveAnnotation(type, Path.class)) {
            builder = builder.addAnnotation(AnnotationSpec.builder(Path.class).addMember("value", "$S", "").build());
        }
        Optional<Class<? extends Annotation>> swaggerApi = classIfExists("io.swagger.annotations.Api");
        if (swaggerApi.isPresent() && doesNotHaveAnnotation(type, swaggerApi.get())) {
            builder = builder.addAnnotation(swaggerApi.get());
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
                //ensure we don't generate path param if it explicitly has a query param annotation
                .filter(ve -> doesNotHaveAnnotation(ve, QueryParam.class))
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

and client gen

```java
package com.mercuria.dali.annotationprocessors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import com.mercuria.dali.rest.*;
import com.squareup.javapoet.*;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.http.PATCH;
import retrofit2.http.Streaming;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.ws.rs.*;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercuria.dali.annotationprocessors.RestUtils.*;
import static javax.lang.model.SourceVersion.latestSupported;

@SupportedAnnotationTypes({
        "com.mercuria.dali.rest.RestClient"
})
public class RestClientGenerator extends AbstractProcessor {


    private static final String QUERY_PARAMS = "queryParams";
    private static final String HEADERS = "headers";
    private static final ParameterizedTypeName STRING_TO_STRING_MAP = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), ClassName.get(String.class));

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (classExists("retrofit2.Retrofit")) {
            for (Element elem : roundEnv.getElementsAnnotatedWith(RestClient.class)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing Rest Client services for " + elem.getSimpleName(), elem);
                try {
                    generateCode((TypeElement) elem, this.processingEnv.getFiler());
                } catch (Exception e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
                    throw new RuntimeException(e);
                }
            }
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Retrofit not on classpath, so not generating client.");
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
        Path rootPathAnnotation = type.getAnnotation(Path.class);

        RestClient restClientAnnotation = type.getAnnotation(RestClient.class);

        AnnotationSpec generated = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", RestClientGenerator.class.getCanonicalName())
                .addMember("date", "$S", ZonedDateTime.now())
                .build();


        String classNameAsBase = type.getSimpleName().toString();
        if (classNameAsBase.endsWith("RestService")) {
            classNameAsBase = classNameAsBase.replaceAll("RestService$", "");
        }
        String retrofitApiClassName = "Retrofit" + classNameAsBase + "ClientApi";
        TypeSpec.Builder retrofitApiType = TypeSpec.interfaceBuilder(retrofitApiClassName)
                .addJavadoc("This interface is generated for Retrofit to use.")
                .addAnnotation(generated);

        ClassName retrofitApi = ClassName.get(pkg.toString(), retrofitApiClassName);

        boolean needsToImplementInterface = !restClientAnnotation.interfaceClass().isEmpty();
        ClassName clientInterface = needsToImplementInterface ?
                ClassName.bestGuess(restClientAnnotation.interfaceClass()) :
                ClassName.get(pkg.toString(), classNameAsBase + "Client");

        TypeSpec.Builder clientInterfaceType = TypeSpec.interfaceBuilder(clientInterface)
                .addAnnotation(generated)
                .addModifiers(Modifier.PUBLIC);

        String retrofitClientName = "Retrofit" + classNameAsBase + "Client";
        TypeSpec.Builder webClientType = TypeSpec.classBuilder(retrofitClientName)
                .addAnnotation(generated)
                .addSuperinterface(clientInterface)
                .addMethods(factoryMethod(retrofitApi, retrofitClientName))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(createClientConstructor(retrofitApi))
                .addField(FieldSpec.builder(retrofitApi, "clientApi", Modifier.FINAL, Modifier.PRIVATE).build())
                .addField(ObjectMapper.class, "objectMapper", Modifier.FINAL, Modifier.PRIVATE)
                .addField(int.class, "maxRetries", Modifier.FINAL, Modifier.PRIVATE)
                .addField(loggerField(retrofitClientName));

        for (Element member : type.getEnclosedElements()) {
            switch (member.getKind()) {
                case METHOD: {
                    ExecutableElement ee = (ExecutableElement) member;
                    if (ee.getModifiers().contains(Modifier.STATIC))
                        continue;
                    if (ee.getModifiers().contains(Modifier.PROTECTED) || ee.getModifiers().contains(Modifier.PRIVATE))
                        continue;

                    Path pathAnnotation = member.getAnnotation(Path.class);
                    if (pathAnnotation == null) {
                        continue;
                    }

                    String methodName = member.getSimpleName().toString();

                    TypeName returnType = TypeName.get(ee.getReturnType());

                    boolean includeHeaderMap = (member.getAnnotation(IncludeHeaderMap.class) != null);
                    boolean includeQueryMap = (member.getAnnotation(IncludeQueryMap.class) != null);
                    if (includeHeaderMap && needsToImplementInterface) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot use @IncludeHeaderMap with @Rest, as the generated client will not implement the parent interface");
                    }
                    if (includeQueryMap && needsToImplementInterface) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot use @IncludeQueryMap with @Rest, as the generated client will not implement the parent interface");
                    }
                    String parametersForCall =
                            Streams.concat(
                                    ee.getParameters().stream()
                                            .filter(variableElement -> parameterShouldBeIncludedInClient(variableElement, type))
                                            .map(param -> param.getSimpleName().toString()),
                                    includeHeaderMap ? Stream.of(HEADERS) : Stream.empty(),
                                    includeQueryMap ? Stream.of(QUERY_PARAMS) : Stream.empty()
                            )
                                    .collect(Collectors.joining(", "));

                    MethodSpec.Builder retrofitApiMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addAnnotation(determineRetrofitMethodType(member, restPath(rootPathAnnotation, pathAnnotation)))
                            .returns(retrofitInterfaceReturnType(returnType));

                    TypeName clientReturnType = clientReturnType(member, returnType, needsToImplementInterface);

                    MethodSpec.Builder clientApiMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(clientReturnType);

                    MethodSpec.Builder webClientMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .returns(clientReturnType)
                            .addStatement("$T responseSupplier = () -> this.clientApi.$L($L)", ParameterizedTypeName.get(ClassName.get(Supplier.class), retrofitInterfaceReturnType(returnType)), methodName, parametersForCall);

                    boolean statementShouldReturn = statementShouldReturn(returnType);
                    Streamed streamed = member.getAnnotation(Streamed.class);
                    if (Objects.nonNull(streamed)) {
                        webClientMethod = webClientMethod.addStatement("return $T.streamValues(LOGGER, responseSupplier, objectMapper, $T.class, this.maxRetries)", RetrofitHelper.class, ClassName.bestGuess(streamed.originalType()));
                    } else if (statementShouldReturn && isResponseWrapper(returnType)) {
                        webClientMethod = webClientMethod.addStatement("return $T.getValue(LOGGER, responseSupplier, false, this.maxRetries).getBody()", RetrofitHelper.class);
                    } else if (isJaxRsResponse(clientReturnType)) {
                        webClientMethod = webClientMethod.addStatement("throw new $T($S)", UnsupportedOperationException.class, "It is not supported to return a jax-rs response object, however this method is required to implement the interface.");
                    } else {
                        webClientMethod = webClientMethod.addStatement((statementShouldReturn ? "return " : "") + "$T.getValue(LOGGER, responseSupplier, $L, this.maxRetries)", RetrofitHelper.class, isOptional(returnType));
                    }
                    //always add the streaming annotation - it only comes into effect when the return type is response body anyway.
                    retrofitApiMethod = retrofitApiMethod.addAnnotation(Streaming.class);

                    MethodSpec.Builder directClientMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .returns(clientReturnType)
                            .addStatement((statementShouldReturn ? "return " : "") + "this.service.$L($L)", methodName, parametersForCall);

                    for (VariableElement ve : ee.getParameters()) {
                        TypeName parameterType = TypeName.get(ve.asType());
                        String parameterName = ve.getSimpleName().toString();

                        QueryParam queryParam = ve.getAnnotation(QueryParam.class);
                        PathParam pathParam = ve.getAnnotation(PathParam.class);
                        boolean parameterShouldBeIncludedInClient = parameterShouldBeIncludedInClient(ve, type);

                        if (queryParam != null) {
                            retrofitApiMethod = retrofitApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName)
                                    .addAnnotation(AnnotationSpec.builder(retrofit2.http.Query.class).addMember("value", "$S", queryParam.value()).build()).build());
                        } else if (pathParam != null) {
                            retrofitApiMethod = retrofitApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName)
                                    .addAnnotation(AnnotationSpec.builder(retrofit2.http.Path.class).addMember("value", "$S", pathParam.value()).build()).build());
                        } else if (parameterShouldBeIncludedInClient) {
                            retrofitApiMethod = retrofitApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName)
                                    .addAnnotation(AnnotationSpec.builder(retrofit2.http.Body.class).build()).build());
                        }

                        if (parameterShouldBeIncludedInClient) {
                            clientApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName).build());
                            webClientMethod.addParameter(ParameterSpec.builder(parameterType, parameterName).build());
                            directClientMethod.addParameter(ParameterSpec.builder(parameterType, parameterName).build());
                        }
                    }
                    if (includeHeaderMap) {
                        retrofitApiMethod = retrofitApiMethod.addParameter(
                                ParameterSpec.builder(STRING_TO_STRING_MAP, HEADERS)
                                        .addAnnotation(AnnotationSpec.builder(retrofit2.http.HeaderMap.class).build())
                                        .build()
                        );
                        clientApiMethod = clientApiMethod.addParameter(ParameterSpec.builder(STRING_TO_STRING_MAP, HEADERS).build());
                        webClientMethod = webClientMethod.addParameter(ParameterSpec.builder(STRING_TO_STRING_MAP, HEADERS).build());
                    }
                    if (includeQueryMap) {
                        retrofitApiMethod = retrofitApiMethod.addParameter(
                                ParameterSpec.builder(STRING_TO_STRING_MAP, QUERY_PARAMS)
                                        .addAnnotation(AnnotationSpec.builder(retrofit2.http.QueryMap.class)
                                                .addMember("encoded", "$L", member.getAnnotation(IncludeQueryMap.class).encoded())
                                                .build())
                                        .build()
                        );
                        clientApiMethod = clientApiMethod.addParameter(ParameterSpec.builder(STRING_TO_STRING_MAP, QUERY_PARAMS).build());
                        webClientMethod = webClientMethod.addParameter(ParameterSpec.builder(STRING_TO_STRING_MAP, QUERY_PARAMS).build());
                    }
                    retrofitApiType = retrofitApiType.addMethod(retrofitApiMethod.build());
                    webClientType = webClientType.addMethod(webClientMethod.build());
                    clientInterfaceType = clientInterfaceType.addMethod(clientApiMethod.build());
                }
            }
        }

        JavaFile.builder(pkg.toString(), retrofitApiType.build()).build()
                .writeTo(filer);

        JavaFile.builder(pkg.toString(), webClientType.build()).build()
                .writeTo(filer);

        if (!needsToImplementInterface) {
            JavaFile.builder(pkg.toString(), clientInterfaceType.build()).build().writeTo(filer);
        }
    }

    private boolean parameterShouldBeIncludedInClient(VariableElement variableElement, TypeElement typeElement) {
        boolean hasContextAnnotation = variableElement.getAnnotation(Context.class) != null;
        boolean hasSuspendedAnnotation = variableElement.getAnnotation(Suspended.class) != null;
        return !(hasContextAnnotation || hasSuspendedAnnotation || variableTypeIsMentionedInAnnotation(typeElement, variableElement));
    }

    private boolean variableTypeIsMentionedInAnnotation(TypeElement typeElement, VariableElement variableElement) {
        TypeName variableType = TypeName.get(variableElement.asType());

        //pretty nasty stuff, but you can't just get the class out of the annotation
        //http://hauchee.blogspot.co.uk/2015/12/compile-time-annotation-processing-getting-class-value.html
        return typeElement.getAnnotationMirrors().stream()
                .filter(am -> {
                    TypeElement element = (TypeElement) am.getAnnotationType().asElement();
                    return ClassName.get(element).equals(ClassName.get(RestClient.class));
                })
                .flatMap(am -> {
                    Optional<? extends AnnotationValue> paramTypesToIgnore = am.getElementValues()
                            .entrySet()
                            .stream()
                            .filter(ee -> ee.getKey().getSimpleName().toString().equals("paramTypesToIgnore"))
                            .map(Map.Entry::getValue)
                            .findAny();

                    List<AnnotationValue> annotationValues = (List<AnnotationValue>) paramTypesToIgnore.map(av -> av.getValue()).orElse(Collections.emptyList());
                    return annotationValues.stream();
                })
                .anyMatch(av -> variableType.equals(TypeName.get((TypeMirror) av.getValue())));
    }

    private List<MethodSpec> factoryMethod(ClassName retrofitApi, String retrofitClientClass) {
        ClassName clientClassName = ClassName.bestGuess(retrofitClientClass);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(RetrofitWebServiceClientBuilder.class), retrofitApi, clientClassName);
        MethodSpec create = MethodSpec.methodBuilder("createBuilder")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType)
                .addParameter(String.class, "baseUrl")
                .addStatement("return new $T(baseUrl, LOGGER, $T.class, $L::new)", returnType, retrofitApi, retrofitClientClass)
                .build();
        MethodSpec build = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .returns(clientClassName)
                .addParameter(String.class, "baseUrl")
                .addStatement("return createBuilder(baseUrl).build()", returnType, retrofitApi, retrofitClientClass)
                .build();
        return Arrays.asList(create, build);
    }

    private TypeName clientReturnType(Element member, TypeName returnType, boolean needsToImplementInterface) {
        if (isResponseWrapper(returnType)) {
            TypeName typeName = ((ParameterizedTypeName) returnType).typeArguments.get(0);
            if (Objects.nonNull(member.getAnnotation(Primative.class))) {
                return typeName.unbox();
            } else {
                return typeName;
            }
        } else if (Objects.nonNull(member.getAnnotation(Streamed.class))) {
            Streamed streamed = member.getAnnotation(Streamed.class);
            return ParameterizedTypeName.get(ClassName.get(Stream.class), ClassName.bestGuess(streamed.originalType()));
        } else if (isJaxRsResponse(returnType) && !needsToImplementInterface) {
            return ClassName.get(ResponseBody.class);
        } else {
            return returnType;
        }
    }

    private String restPath(Path rootPathAnnotation, Path pathAnnotation) {
        String firstPart = rootPathAnnotation == null ? "" : rootPathAnnotation.value();
        String secondPart = pathAnnotation == null ? "" : pathAnnotation.value();
        return joinUrlParts(firstPart, secondPart);
    }


    private FieldSpec loggerField(String typeName) {
        return FieldSpec.builder(Logger.class, "LOGGER", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$T.getLogger($S)", LoggerFactory.class, "com.mercuria.dali.rest." + typeName)
                .build();
    }

    private boolean statementShouldReturn(TypeName returnType) {
        return !returnType.equals(TypeName.VOID);
    }

    private MethodSpec createClientConstructor(TypeName clientApi) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(clientApi, "clientApi")
                .addParameter(ObjectMapper.class, "objectMapper")
                .addParameter(int.class, "maxRetries")
                .addStatement("this.clientApi = clientApi")
                .addStatement("this.objectMapper = objectMapper")
                .addStatement("this.maxRetries = maxRetries")
                .build();
    }

    private ParameterizedTypeName retrofitInterfaceReturnType(TypeName returnType) {
        final TypeName actualReturnType;
        if (returnType.isPrimitive()) {
            actualReturnType = returnType.box();
        } else if (TypeName.VOID.equals(returnType)) {
            actualReturnType = ClassName.get(ResponseBody.class);
        } else if (isJaxRsResponse(returnType)) {
            actualReturnType = ClassName.get(ResponseBody.class);
        } else {
            actualReturnType = returnType;
        }
        return ParameterizedTypeName.get(ClassName.get(Call.class), actualReturnType);
    }

    private boolean isJaxRsResponse(TypeName returnType) {
        return ClassName.get(javax.ws.rs.core.Response.class).equals(returnType);
    }

    private AnnotationSpec determineRetrofitMethodType(Element member, String pathSpec) {
        final Class<?> annotationClass;
        if (member.getAnnotation(DELETE.class) != null) {
            annotationClass = retrofit2.http.DELETE.class;
        } else if (member.getAnnotation(PUT.class) != null) {
            annotationClass = retrofit2.http.PUT.class;
        } else if (member.getAnnotation(GET.class) != null) {
            annotationClass = retrofit2.http.GET.class;
        } else if (member.getAnnotation(POST.class) != null) {
            annotationClass = retrofit2.http.POST.class;
        } else if (member.getAnnotation(PATCH.class) != null) {
            annotationClass = retrofit2.http.PATCH.class;
        } else if (member.getAnnotation(OPTIONS.class) != null) {
            annotationClass = retrofit2.http.OPTIONS.class;
        } else {
            throw new IllegalStateException("Cannot determine type");
        }
        return AnnotationSpec.builder(annotationClass).addMember("value", "$S", pathSpec).build();

    }

}

```
