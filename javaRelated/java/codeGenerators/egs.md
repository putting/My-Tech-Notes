# Sample code Genrators. To be investigated more fully

## REST web services and clients using a @REST annotation

## REST Service Generator
```java
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

## RestClient Generator

```java
@SupportedAnnotationTypes({
        "com.mercuria.dali.rest.RestClient"
})
public class RestClientGenerator extends AbstractProcessor {


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

        boolean clientTypeSpecified = !restClientAnnotation.interfaceClass().isEmpty();
        ClassName clientInterface = clientTypeSpecified ?
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

                    String parametersForCall = ee.getParameters().stream().map(ve1 -> ve1.getSimpleName().toString()).collect(Collectors.joining(", "));

                    MethodSpec.Builder retrofitApiMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addAnnotation(determineRetrofitMethodType(member, restPath(rootPathAnnotation, pathAnnotation)))
                            .returns(retrofitInterfaceReturnType(returnType));

                    TypeName clientReturnType = clientReturnType(member, returnType);

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
                        webClientMethod = webClientMethod.addStatement("return $T.streamValues(LOGGER, responseSupplier, objectMapper, $T.class)", RetrofitHelper.class, ClassName.bestGuess(streamed.originalType()));
                        retrofitApiMethod = retrofitApiMethod.addAnnotation(Streaming.class);
                    } else if (statementShouldReturn && isResponseWrapper(returnType)) {
                        webClientMethod = webClientMethod.addStatement("return $T.getValue(LOGGER, responseSupplier, false).getBody()", RetrofitHelper.class);
                    } else {
                        webClientMethod = webClientMethod.addStatement((statementShouldReturn ? "return " : "") + "$T.getValue(LOGGER, responseSupplier, $L)", RetrofitHelper.class, isOptional(returnType));
                    }

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
                        Context context = ve.getAnnotation(Context.class);

                        if (queryParam != null) {
                            retrofitApiMethod = retrofitApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName)
                                    .addAnnotation(AnnotationSpec.builder(retrofit2.http.Query.class).addMember("value", "$S", queryParam.value()).build()).build());
                        } else if (pathParam != null) {
                            retrofitApiMethod = retrofitApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName)
                                    .addAnnotation(AnnotationSpec.builder(retrofit2.http.Path.class).addMember("value", "$S", pathParam.value()).build()).build());
                        } else if (context == null) {
                            retrofitApiMethod = retrofitApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName)
                                    .addAnnotation(AnnotationSpec.builder(retrofit2.http.Body.class).build()).build());
                        }

                        clientApiMethod.addParameter(ParameterSpec.builder(parameterType, parameterName).build());
                        webClientMethod.addParameter(ParameterSpec.builder(parameterType, parameterName).build());
                        directClientMethod.addParameter(ParameterSpec.builder(parameterType, parameterName).build());
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

        if (!clientTypeSpecified) {
            JavaFile.builder(pkg.toString(), clientInterfaceType.build()).build().writeTo(filer);
        }
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

    private TypeName clientReturnType(Element member, TypeName returnType) {
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

        } else {
            return returnType;
        }
    }

    private String restPath(Path rootPathAnnotation, Path pathAnnotation) {
        String firstPart = rootPathAnnotation == null ? "" : rootPathAnnotation.value();
        String secondPart = pathAnnotation == null ? "" : pathAnnotation.value();
        return firstPart + secondPart;
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
                .addStatement("this.clientApi = clientApi")
                .addStatement("this.objectMapper = objectMapper")
                .build();
    }

    private ParameterizedTypeName retrofitInterfaceReturnType(TypeName returnType) {
        final TypeName actualReturnType;
        if (returnType.isPrimitive()) {
            actualReturnType = returnType.box();
        } else if (TypeName.VOID.equals(returnType)) {
            actualReturnType = ClassName.get(ResponseBody.class);
        } else if (ClassName.get(javax.ws.rs.core.Response.class).equals(returnType)) {
            actualReturnType = ClassName.get(ResponseBody.class);
        } else {
            actualReturnType = returnType;
        }
        return ParameterizedTypeName.get(ClassName.get(Call.class), actualReturnType);
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

## Also some helper for web service clients
```java
public final class RetrofitWebServiceClientBuilder<T, C> {

    private final String baseUrl;
    private final Class<T> serviceType;
    private final Logger logger;
    private final BiFunction<T, ObjectMapper, C> builder;

    private Interceptor authInterceptor = null;
    private ObjectMapper objectMapper = new ObjectMapper();
    private long timeout = TimeUnit.SECONDS.toMillis(30);

    public RetrofitWebServiceClientBuilder(String baseUrl, Logger logger, Class<T> serviceType, BiFunction<T, ObjectMapper, C> builder) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : (baseUrl + "/");
        this.logger = logger;
        this.serviceType = serviceType;
        this.builder = builder;
    }

    public RetrofitWebServiceClientBuilder<T, C> withBasicAuth(String userName, String password) {
        this.authInterceptor = chain -> {
            String credential = Credentials.basic(userName, password);
            return chain.proceed(chain.request()
                    .newBuilder()
                    .header("Authorization", credential)
                    .build());
        };
        return this;
    }

    public RetrofitWebServiceClientBuilder<T, C> withObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public RetrofitWebServiceClientBuilder<T, C> withTimeout(long time, TimeUnit timeUnit) {
        this.timeout = timeUnit.toMillis(time);
        return this;
    }

    public C build() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(message -> {
            if (logger.isTraceEnabled()) {
                logger.trace(message);
            } else if (logger.isDebugEnabled()) {
                logger.debug(message);
            } else if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        });
        if (logger.isTraceEnabled()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else if (logger.isDebugEnabled()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        } else if (logger.isInfoEnabled()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .readTimeout(this.timeout, TimeUnit.MILLISECONDS);
        if (this.authInterceptor != null) {
            clientBuilder = clientBuilder.addInterceptor(this.authInterceptor);
        }
        T api = new Retrofit.Builder()
                .client(clientBuilder.build())
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(this.objectMapper))
                .build()
                .create(serviceType);
        return this.builder.apply(api, this.objectMapper);
    }

}

```
