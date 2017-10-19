# Reflection & Generics (with Pipeline egs)

## java/lang.reflect.Type
All should be instances of a class type.
- ParameterizedType
- GenericArrayType
- WildcardType
- TypeVariable

## Class<T>
Is the .class of a generic type.

##Egs
```java
public class DataType<T> {

    private final Type type;

    private final Annotation qualifier;

    public static <T> DataType<T> of(Class<T> type) {
        return new DataType(type, null);
    }

    public static <T> DataType<T> of(Class<T> type, Annotation annotation) {
        return new DataType(type, annotation);
    }

    DataType(Type type, Annotation annotation) {
        this.type = type;
        this.qualifier = annotation;
    }

    public Type getType() {
        return type;
    }

    public Annotation getQualifier() {
        return qualifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataType<?> dataType = (DataType<?>) o;

        if (type != null ? !type.equals(dataType.type) : dataType.type != null) return false;
        return qualifier != null ? qualifier.equals(dataType.qualifier) : dataType.qualifier == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataType{" +
                "type=" + type +
                ", qualifier=" + qualifier +
                '}';
    }

    public boolean isVoid() {
        return type == Void.TYPE;
    }
}
```
and is used in holding state here:
```java
public class PipelineContext {

    private final PipelineContext parent;

//    @JsonDeserialize(keyUsing = PipelineKeyDeserializer.class)
    private final Map<DataType<?>, Object> state = new LinkedHashMap<>();

    private Throwable exception;

    public static PipelineContext newInstance() {
        return new PipelineContext(null);
    }


    public static <X> PipelineContext forInput(DataType<X> key, X value) {
        PipelineContext pc = new PipelineContext(null);
        pc.put(key, value);
        return pc;
    }

    public static <X> PipelineContext forInput(DataType<X> key, X value, PipelineContext parent) {
        PipelineContext pc = new PipelineContext(parent);
        pc.put(key, value);
        return pc;
    }

    public <X> PipelineContext newChildInstance(DataType<X> key, X value) {
        PipelineContext pc = new PipelineContext(this);
        pc.put(key, value);
        return pc;
    }

    public PipelineContext(PipelineContext parent) {
        this.parent = parent;
    }

    public PipelineContext combine(PipelineContext other) {

        state.putAll(other.state);
        return new PipelineContext(other);
    }

    <T> void put(DataType<T> key, T value) {
        if (state.containsKey(key)) {
            throw new IllegalStateException("Pipeline context already contains key " + key);
        }
        state.put(key, value);
    }

    public <T> T get(Class<T> type) {
        return get(DataType.of(type));
    }

    public <T> T get(TypeReference<T> type) {
        return (T)get(new DataType(type.getType(), null));
    }

    public <T> T find(TypeReference<T> type, Class<? extends Annotation> annotationType) {
        return (T) state.entrySet().stream()
                .filter(e -> e.getKey().getType().equals(type.getType()) && annotationType.isInstance(e.getKey().getQualifier()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pipeline context does not contain type " + type));
        // TODO - maybe fall back to getting from the parent?
    }

    public <T> T get(Type type) {
        return (T)get(new DataType(type, null));
    }

    public <T> T get(Class<T> type, PipelineAnnotation qualifier) {
        return get(DataType.of(type, qualifier));
    }


    <T> T get(DataType<T> key) {
        if (!state.containsKey(key)) {
            if (parent != null) {
                return parent.get(key);
            } else {
                throw new IllegalStateException("Pipeline context does not contain key " + key);
            }
        }
        return (T)state.get(key);
    }

    void setException(Throwable exception) {
        this.exception = exception;
    }

    public Throwable getException() {
        return exception;
    }

    public Map<DataType<?>, Object> getState() {
        return Collections.unmodifiableMap(state);
    }

    public Collection<? extends DataType> getDataTypes() {
        return state.keySet();
    }

    public String prettyPrint() {

        StringBuilder sb = new StringBuilder();

        if (parent != null) {
            parent.prettyPrint();
        }

        sb.append("State:");
        AtomicInteger atomicInteger = new AtomicInteger(0);
        state.entrySet().forEach(s -> {
            sb.append("\n");
            sb.append(atomicInteger.incrementAndGet() + " = ");
            sb.append(s);
        });

        sb.append("exception:");
        sb.append(exception);

        return sb.toString();
    }

}
```
and the core logic for processing a Pipeline is here:
```java
public class Pipeline<I> {

    private final String name;
    private final List<Object> stages;
    private final DataType<I> inputType;
    private final List<Consumer<PipelineContext>> stageInvokers;
    private final PipelineContext parentContext;

    Pipeline(String name, PipelineContext parentContext, Class<I> inputType, Object... stages) {
        this.name = name;
        this.parentContext = parentContext;
        this.stages = new ArrayList<>(Arrays.asList(stages));
        this.inputType = DataType.of(inputType);
        this.stageInvokers = buildInvokers();
    }

    public static <T> Pipeline<T> withInput(String name, Class<T> inputType, Object... stages) {
        return new Pipeline<>(name, null, inputType, stages);
    }

    public static <T> Pipeline<T> withInput(String name, PipelineContext parentContext, Class<T> inputType, Object... stages) {
        return new Pipeline<>(name, parentContext, inputType, stages);
    }

    public static Pipeline<Void> withoutInput(String name, Object... stages) {
        return new Pipeline<>(name, null, Void.TYPE, stages);
    }

    private List<Consumer<PipelineContext>> buildInvokers() {

        List<Consumer<PipelineContext>> stageInvokers = new ArrayList<>();

        Set<DataType> keys = new LinkedHashSet<>();
        if (parentContext != null) {
            keys.addAll(parentContext.getDataTypes());
        }
        if (!inputType.isVoid()) {
            keys.add(inputType);
        }
        for (Object stage : stages) {
            Class<?> stageClass = stage.getClass();
            List<Method> methods = getClassPipelineMethods(stageClass);
            if (methods.isEmpty()) {
                throw new RuntimeException("Stage class " + stageClass.getName() + " does not have a method with a PipelineStage annotation");
            }
            if (methods.size() > 1) {
                throw new RuntimeException("Stage class " + stageClass.getName() + " has " + methods.size() + " methods with a PipelineStage annotation");
            }
            Method method = methods.get(0); //FIXME: What if a class has multiple Pipeline Methods???
            AnnotatedType[] parameterTypes = method.getAnnotatedParameterTypes();
            List<DataType> parameterKeys = new ArrayList<>();
            for (int param = 0; param < parameterTypes.length; param++) {
                AnnotatedType parameterType = parameterTypes[param];
//                PipelineAnnotation[] parameterAnnotations = parameterType.getAnnotationsByType(PipelineAnnotation.class);
                List<Annotation> parameterAnnotations = getPipelineAnnotations(parameterType);
                if (parameterAnnotations.size() > 1) {
                    throw new RuntimeException("Method " + stageClass.getName() + "." + method.getName() + "() parameter " + param + " has multiple pipeline annotations");
                }
                Annotation qualifier = parameterAnnotations.size() == 0 ? null : parameterAnnotations.get(0);
                DataType parameterKey = new DataType(parameterType.getType(), qualifier);
                if (!(keys.contains(parameterKey))) {
                    throw new RuntimeException("Method " + stageClass.getName() + "." + method.getName() + "() parameter " + param + " has type " + parameterKey + " but no preceding pipeline stage returns this");
                }
                parameterKeys.add(parameterKey);

            }
            AnnotatedType returnType = method.getAnnotatedReturnType();
            List<Annotation> returnAnnotations = getPipelineAnnotations(returnType);
            if (returnAnnotations.size() > 1) {
                throw new RuntimeException("Method " + stageClass.getName() + "." + method.getName() + "() has multiple pipeline annotations on return type");
            }
            Annotation qualifier = returnAnnotations.isEmpty() ? null : returnAnnotations.get(0);
            DataType returnKey = new DataType(returnType.getType(), qualifier);
            if (!returnType.equals(Void.TYPE)) {
                if (keys.contains(returnType)) {
                    throw new RuntimeException("Multiple stages of pipeline return the same annotated type " + returnKey);
                }
                keys.add(returnKey);
            }

            stageInvokers.add(pipelineContext -> {
                List<Object> inputParameters = (List<Object>) parameterKeys.stream().map(pipelineContext::get).collect(Collectors.toList());
                try {
                    Object output = method.invoke(stage, inputParameters.toArray());
                    if (!returnType.equals(Void.TYPE)) {
                        pipelineContext.put(returnKey, output);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    pipelineContext.setException(e.getCause());
                }
            });
        }

        return stageInvokers;
    }


    public PipelineContext execute(I inputParameter) {
        PipelineContext context = PipelineContext.forInput(inputType, inputParameter, parentContext);
        for (Consumer<PipelineContext> stageInvoker : stageInvokers) {
            if (context.getException() == null) {
                stageInvoker.accept(context);
            }
        }
        return context;
    }

    private List<Annotation> getPipelineAnnotations(AnnotatedType annotatedType) {
        return Stream.of(annotatedType.getAnnotations())
                .filter(annotation -> annotation.getClass().getAnnotationsByType(PipelineAnnotation.class) != null)
                .collect(Collectors.toList());
    }

    private List<Method> getClassPipelineMethods(Class<?> stageClass) {

        return Arrays.asList(stageClass.getDeclaredMethods(), stageClass.getSuperclass().getDeclaredMethods())
                .stream()
                .flatMap(m -> Arrays.stream(m))
                .filter(method -> method.getAnnotation(PipelineStage.class) != null)
                .collect(Collectors.toList());
    }

    public String prettyPrint() {

        StringBuilder sb = new StringBuilder();

        if (parentContext != null) {
            sb.append("ParentContext passed into Pipeline " + name + "\n");
            sb.append(parentContext.prettyPrint());
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("Input to Pipeline " + name + " : ");
        sb.append(inputType);
        sb.append("\n");
        sb.append("Stages: " + "\n");
        for (int i=0; i < stages.size(); i++) {
            sb.append("\t");
            sb.append(i + " = ");
            sb.append(getClassPipelineMethods(stages.get(i).getClass()));
            sb.append("\n");
        }

        return sb.toString();
    }

}
```
