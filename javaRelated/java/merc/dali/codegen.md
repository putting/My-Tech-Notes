# Code Gen Summary

## Dali Jdbi Generic class
```java
public abstract class JdbiDaoProvider<T> implements Provider<T> {

    private final Class<T> type;
    @Inject
    private DBI dbi;

    public JdbiDaoProvider() {
        Class<? extends JdbiDaoProvider> providerClass = getClass();
        ParameterizedType genericSuperclass = (ParameterizedType) providerClass.getGenericSuperclass();
        this.type = (Class) genericSuperclass.getActualTypeArguments()[0];
    }

    @Override
    public T get() {
        return dbi.onDemand(type);
    }
}
```

## So Daos then uses this guice provider
The Dao's are injected by a lookup pattern of some sort
```java
@ProvidedBy(DataExclusionDao.Provider.class)
public interface DataExclusionDao {

    static class Provider extends JdbiDaoProvider<DataExclusionDao> {
    }

    @SqlBatch(DataExclusionDboMapper.SQL_INSERT)
    void insertExcludedData(@BindObjectAsMap List<DataExclusionDbo> costs);

    @SqlQuery("select * from " + DataExclusionDboMapper.TABLE_NAME)
    @RegisterMapper(DataExclusionDboMapper.class)
    List<DataExclusionDbo> getExcludedData();
 }
```

### Immutables
```java
@Value.Immutable
@Jdbi(tableName = "control.DataExclusion")
@JsonSerialize(as = ImmutableDataExclusionDbo.class)
@JsonDeserialize(as = ImmutableDataExclusionDbo.class)
public interface DataExclusionDbo {

    @Value.Default
    @JdbiOptions(inColumnList = false)
    default long id() {return -1;}

    String sourceSystemKey();
 }
```

### The magic is in the way the Mapper are created dynamic call from the Immutables. Known as Dali Processors
They map result sets into the immutable obs
```java
package com.mercuria.dali.annotationprocessors;

import com.mercuria.dali.jdbi.DaliHelper;
import com.mercuria.dali.jdbi.Jdbi;
import com.mercuria.dali.jdbi.JdbiOptions;
import com.mercuria.dali.jdbi.JdbiStoreAsJson;
import com.squareup.javapoet.*;
import org.immutables.value.Value;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javax.lang.model.SourceVersion.latestSupported;

/**
 * Created by mshaylor on 08/07/2016.
 */
@SupportedAnnotationTypes({
        "com.mercuria.dali.jdbi.Jdbi"
})
public class JdbiProcessor extends AbstractProcessor {

    enum DaliType {
        primitive,
        string,
        jdbiobject,
        jsonobject
    }

    private static final JdbiOptions DEFAULT_OPTIONS = new JdbiOptions() {
        //Would be nice to pull the defaults from the annotation but this is obsecenely difficult
        //as the retention is source level.

        @Override
        public Class<? extends Annotation> annotationType() {
            return JdbiOptions.class;
        }

        @Override
        public String columnName() {
            return "";
        }

        @Override
        public boolean ignore() {
            return false;
        }

        @Override
        public boolean inColumnList() {
            return true;
        }

        @Override
        public boolean optional() {
            return false;
        }

        @Override
        public boolean trim() {
            return false;
        }
    };

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        Messager messager = processingEnv.getMessager();

        for (Element elem : env.getElementsAnnotatedWith(Jdbi.class)) {
            try {
                generateCode((TypeElement) elem);
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                messager.printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
                throw new DaliJDBIException(e);
            }
        }

        return true;
    }

    private void generateCode(TypeElement elem) throws IOException {

        PackageElement pkg = processingEnv.getElementUtils().getPackageOf(elem);

        Jdbi jdbiAnnotation = elem.getAnnotation(Jdbi.class);

        AnnotationSpec generated = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", JdbiProcessor.class.getCanonicalName())
                .addMember("date", "$S", ZonedDateTime.now())
                .build();

        TypeName typeName = TypeName.get(elem.asType());
        String simpleName = elem.getSimpleName().toString();
        TypeSpec.Builder mapperType = TypeSpec.classBuilder(simpleName + "Mapper")
                .addAnnotation(generated)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(ResultSetMapper.class), typeName));

        MethodSpec.Builder mapMethodSpec = MethodSpec.methodBuilder("map")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addParameter(TypeName.INT, "index")
                .addParameter(ResultSet.class, "r")
                .addParameter(StatementContext.class, "ctx")
                .addException(SQLException.class)
                .addStatement("Immutable$L.Builder builder = Immutable$L.builder()", simpleName, simpleName)
                .addStatement("$T dh = new $T(r, ctx)", DaliHelper.class, DaliHelper.class);

        List<JavaDbName> columnList = new ArrayList<>();

        for (Element member : elem.getEnclosedElements()) {
            //Check for method annotation
            JdbiOptions jdbiOptions = member.getAnnotation(JdbiOptions.class);
            Value.Derived derivedAnnotation = member.getAnnotation(Value.Derived.class);
            if (jdbiOptions == null) jdbiOptions = DEFAULT_OPTIONS;
            if (jdbiOptions.ignore() || derivedAnnotation != null) {
                continue;
            }
            JdbiStoreAsJson jdbiStoreAsJson = member.getAnnotation(JdbiStoreAsJson.class);


            switch (member.getKind()) {
                case METHOD:
                    ExecutableElement ee = (ExecutableElement) member;
                    //Static stuff should not be included!
                    if (ee.getModifiers().contains(Modifier.STATIC))
                        continue;

                    //Protected methods are assumed not to be JDBI related
                    //Typically these are for @Value.Check() or similar.
                    if (ee.getModifiers().contains(Modifier.PROTECTED) || ee.getModifiers().contains(Modifier.PRIVATE))
                        continue;

                    String javaName = member.getSimpleName().toString();

                    String formattedDbName;
                    if (jdbiOptions.columnName().isEmpty()) {
                        formattedDbName = jdbiAnnotation.javaCaseFormat().to(jdbiAnnotation.dbCaseFormat(), javaName);
                    } else {
                        formattedDbName = jdbiOptions.columnName();
                    }

                    if (jdbiOptions.inColumnList()) {
                        columnList.add(new JavaDbName(formattedDbName, javaName));
                    }
                    TypeName returnType = getReturnType(ee);
                    DaliAndRegularType daliType = getDaliType(ee.getReturnType(), jdbiOptions, jdbiStoreAsJson);
                    switch (daliType.getDaliType()) {
                        case primitive:
                            mapMethodSpec.addStatement("builder.$L(r.get$L($S))", javaName, daliType.getType(), formattedDbName);
                            break;
                        case jdbiobject:
                            mapMethodSpec.addStatement("dh.setEntity(($T) builder::$L, $S, $L, $L.class)",
                                    ParameterizedTypeName.get(ClassName.get(Consumer.class), returnType),
                                    javaName,
                                    formattedDbName,
                                    jdbiOptions.optional(),
                                    returnType);
                            break;
                        case jsonobject:
                            mapMethodSpec.addStatement("dh.setEntityViaJson(($T) builder::$L, $S, $L, $L.class)",
                                    ParameterizedTypeName.get(ClassName.get(Consumer.class), returnType),
                                    javaName,
                                    formattedDbName,
                                    jdbiOptions.optional(),
                                    returnType);
                            break;
                        case string:
                            mapMethodSpec.addStatement("dh.setString(($T) builder::$L, $S, $L, $L)",
                                    ParameterizedTypeName.get(ClassName.get(Consumer.class), returnType),
                                    javaName,
                                    formattedDbName,
                                    jdbiOptions.optional(),
                                    jdbiOptions.trim());
                            break;
                        default:
                            String msg = String.format("Can't handle dali type: %s", returnType);
                            throw new DaliJDBIException(msg);
                    }
            }
        }
        mapMethodSpec.addStatement("return builder.build()");

        mapperType.addMethod(mapMethodSpec.build());

        addFields(mapperType, columnList);

        if (!jdbiAnnotation.tableName().isEmpty()) {
            addFieldsWithTableName(jdbiAnnotation, mapperType);
        }

        JavaFile directClientFile = JavaFile.builder(pkg.toString(), mapperType.build()).build();
        directClientFile.writeTo(processingEnv.getFiler());
    }

    private void addFieldsWithTableName(Jdbi jdbiAnnotation, TypeSpec.Builder mapperType) {
        String tableName = jdbiAnnotation.tableName();
        mapperType.addField(FieldSpec
                .builder(String.class, "TABLE_NAME", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$S", tableName)
                .build());

        mapperType.addField(FieldSpec
                .builder(String.class, "SQL_INSERT", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$S + TABLE_NAME + $S + DB_COLUMNS + $S + BIND_COLUMNS + $S", "insert into ", "(", ") values (", ")")
                .build());

        mapperType.addField(FieldSpec
                .builder(String.class, "SQL_UPDATE_PREFIX", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$S + TABLE_NAME + $S + SET_COLUMNS", "UPDATE ", " SET ")
                .build());
    }

    private void addFields(TypeSpec.Builder mapperType, List<JavaDbName> columnList) {
        mapperType.addField(FieldSpec
                .builder(String.class, "DB_COLUMNS", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$S", columnList.stream().map(JavaDbName::getFormattedDbName).collect(Collectors.joining(", ")))
                .build());

        mapperType.addField(FieldSpec
                .builder(String.class, "BIND_COLUMNS", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$S", columnList.stream().map(JavaDbName::getJavaName).collect(Collectors.joining(", :", ":", "")))
                .build());

        mapperType.addField(FieldSpec
                .builder(String.class, "SET_COLUMNS", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .initializer("$S", columnList.stream().map(jdbn -> jdbn.getFormattedDbName() + "=:" + jdbn.getJavaName()).collect(Collectors.joining(", ")))
                .build());
    }

    private TypeName getReturnType(ExecutableElement ee) {
        TypeName typeName = TypeName.get(ee.getReturnType());
        if (typeName instanceof ParameterizedTypeName) {
            if (((ParameterizedTypeName) typeName).rawType.equals(ClassName.get(Optional.class))) {
                return ((ParameterizedTypeName) typeName).typeArguments.get(0);
            }
        }
        return typeName;
    }


    private DaliAndRegularType getDaliType(TypeMirror retType, JdbiOptions jdbiOptions, JdbiStoreAsJson jdbiStoreAsJson) {
        DaliType daliType;
        String type;
        switch (retType.getKind()) {
            case INT:
                daliType = DaliType.primitive;
                type = "Int";
                break;
            case DOUBLE:
                daliType = DaliType.primitive;
                type = "Double";
                break;
            case LONG:
                daliType = DaliType.primitive;
                type = "Long";
                break;
            case BOOLEAN:
                daliType = DaliType.primitive;
                type = "Boolean";
                break;
            case ARRAY:
                if (!retType.toString().equals(byte[].class.getCanonicalName())) {
                    String msg = String.format("Can't handle array type: %s", retType);
                    throw new DaliJDBIException(msg);
                }
                // byte[] falls through and is treated as Object
            case DECLARED:
                type = retType.toString();
                if (type.startsWith(Optional.class.getCanonicalName())) {
                    type = type.substring(Optional.class.getCanonicalName().length() + 1, type.length() - 1);
                }
                if (jdbiStoreAsJson != null) {
                    daliType = DaliType.jsonobject;
                } else if (String.class.getCanonicalName().equals(type)) {
                    daliType = DaliType.string;
                } else {
                    daliType = DaliType.jdbiobject;
                }
                break;
            default:
                String msg = String.format("Can't handle return type: %s", retType);
                throw new DaliJDBIException(msg);
        }

        if (jdbiOptions.trim() && daliType != DaliType.string) {
            String msg = String.format("Can't trim type: %s", type);
            throw new DaliJDBIException(msg);
        }
        return new DaliAndRegularType(daliType, type);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return latestSupported();
    }
}
```


