# A sample Controller
Most of the code is in an abstract controller - which implements CRUD with validation

```java
import com.amphora.libs.foundation.audit.revision.dto.entity.*;
import com.amphora.libs.foundation.audit.revision.rest.service.*;
import com.amphora.libs.foundation.project.dto.*;
import com.amphora.libs.foundation.project.service.*;
import com.amphora.pubs.rest.utils.controller.*;
import com.amphora.pubs.rest.utils.controller.config.*;
import com.amphora.pubs.rest.utils.controller.permissions.*;
import com.amphora.pubs.rest.utils.controller.validation.controller.*;
import com.amphora.pubs.rest.utils.controller.versioning.*;
import com.amphora.pubs.rest.utils.dto.*;
import com.amphora.pubs.rest.utils.dto.swagger.*;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.*;
import java.util.*;

import static com.amphora.libs.foundation.audit.revision.rest.service.AuditRedirectionService.AuditPathChooser.AuditPathParams.*;
import static com.amphora.libs.foundation.audit.revision.rest.service.AuditRedirectionService.*;
import static com.amphora.libs.foundation.project.controller.entitlements.ProjectEntitlements.*;

@RestController
@ControllerVersion(major = 1)
@RequestMapping("/api/project/v1/project")
@Api(tags = {"Project"})
@DtoRestControllerApiSet({
        HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.PATCH
})
public class ProjectController extends AbstractDtoRestController<ProjectController, ProjectDto, Integer> {

    /**
     * Project controller can not depend on revision-dao because it depends on all dao projects, causing entity management issues.
     * Instead, swagger-generated client should be used for accessing revision-controller endpoints.
     */
    private static final String PROJECT_ENTITY = AuditEntities.PROJECT.name();

    @Autowired
    SwaggerValidationConfig.Canary canary;


    /**
     * Builds the instance and attempts to extract the type of entity it serves.
     *
     * @param service instance of {@link ProjectDaoDtoMappingService} to delegate work to.
     */
    @Autowired
    public ProjectController(ProjectDaoDtoMappingService service) {
        super(service);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ //

    @ApiOperation(value = "GET project",
                  notes = "Retrieves a list of project",
                  response = ProjectDto.class,
                  responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, // HttpStatus.OK
                         message = "project found and returned"),
            @ApiResponse(code = 404, // HttpStatus.NOT_FOUND
                         message = "No project found")
    })
    @GetMapping
    @MethodPermissions(VIEW_PROJECT)
    public ResponseEntity<List<ProjectDto>> getAll(@RequestHeader HttpHeaders inputHeaders, @ApiParam(name = Swagger.Annotation.Const.Select.NAME,
                                                                                                      value = Swagger.Annotation.Const.Select.VALUE,
                                                                                                      type = Swagger.Annotation.Const.Select.TYPE,
                                                                                                      example = Swagger.Annotation.Const.Select.EXAMPLE)
    @RequestParam(name = Swagger.Annotation.Const.Select.NAME,
                  required = false,
                  defaultValue = "") String selected,
                                                   @ApiParam(name = Swagger.Annotation.Const.Ids.NAME,
                                                             value = Swagger.Annotation.Const.Ids.VALUE,
                                                             type = Swagger.Annotation.Const.Ids.TYPE,
                                                             example = Swagger.Annotation.Const.Ids.EXAMPLE)
                                                   @RequestParam(name = Swagger.Annotation.Const.Ids.NAME,
                                                                 required = false,
                                                                 defaultValue = "") String ids,
                                                   @ApiParam(name = Swagger.Annotation.Const.Where.NAME,
                                                             value = Swagger.Annotation.Const.Where.VALUE,
                                                             type = Swagger.Annotation.Const.Where.TYPE,
                                                             example = Swagger.Annotation.Const.Where.EXAMPLE)
                                                   @RequestParam(name = Swagger.Annotation.Const.Where.NAME,
                                                                 required = false,
                                                                 defaultValue = "") String constraints,
                                                   @ApiParam(name = Swagger.Annotation.Const.OrderBy.NAME,
                                                             value = Swagger.Annotation.Const.OrderBy.VALUE,
                                                             type = Swagger.Annotation.Const.OrderBy.TYPE,
                                                             example = Swagger.Annotation.Const.OrderBy.EXAMPLE)
                                                   @RequestParam(name = Swagger.Annotation.Const.OrderBy.NAME,
                                                                 required = false,
                                                                 defaultValue = "") String orderings,
                                                   @ApiParam(name = Swagger.Annotation.Const.Between.NAME,
                                                             value = Swagger.Annotation.Const.Between.VALUE,
                                                             type = Swagger.Annotation.Const.Between.TYPE,
                                                             example = Swagger.Annotation.Const.Between.EXAMPLE)
                                                   @RequestParam(name = Swagger.Annotation.Const.Between.NAME,
                                                                 required = false,
                                                                 defaultValue = "") String limits) {
        return super.getAllImpl(inputHeaders, selected,
                                ids,
                                constraints,
                                orderings,
                                limits);
    }
```

## The Framework abstract class
Just a small sample to give a flavour. A lot of the work is including the SQL like params from the framework.
Otherwise consistent response types and multi failure errors....
```java
public abstract class AbstractDtoRestController<C extends AbstractDtoRestController<C, E, ID>, E extends AbstractDto<E, ID>, ID extends Comparable<ID> & Serializable>
        implements ValidatableType, PermissionsHolder<HttpServletRequest>, EventSource<C> {
 @Getter
    public class DtoEvent extends SourcedEvent<C, HttpMethod> implements Cloneable {

        /**
         * Indicator of what Http method is being called.
         */
        private final HttpMethodSelector        methodSelector;
        ......
        private DtoEvent(@NonNull DtoEvent origin) {
            super((C) AbstractDtoRestController.this, origin.getPayload());
            this.methodSelector    = origin.getMethodSelector();
            this.callPointSelector = origin.getCallPointSelector(); 
            ''''
@Autowired
    AbstractServiceManifest  serviceManifest;

    @Autowired
    Validator                validator;

    @Autowired
    MultiFailureErrorHandler errorHandler;

    @Autowired
    SharedEventBus           eventBus;

    @Autowired
    PoolStore<String>        poolStore;

    @Autowired
    private ServletContext context;

    private final Class<E>                           entityType;
    private final RestControllerPermissionsHolder<C> permissionsHolder;
    private final AbstractDtoRestService<E, ID>      service;

    private          Optional<? extends Validator>  extraValidator;
    private          SwaggerValidationConfig.Canary swaggerValidationCanary;
    @Getter
    private volatile ControllerVersion              version;

    /**
     * Builds the instance and attempts to extract the type of entity it serves.
     *
     * @param service instance of {@link AbstractDtoRestService} to delegate work to.
     */
    protected AbstractDtoRestController(AbstractDtoRestService<E, ID> service) {
        Objects.requireNonNull(service);
        final Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) {
            entityType = null; // not defined, need to override dtoType()
        } else {
            final Type genericType = ((ParameterizedType) superClass).getActualTypeArguments()[1];
            if (genericType instanceof Class) {
                entityType = (Class<E>) genericType;
            } else {
                entityType = null;
            }
        }
        this.permissionsHolder = new RestControllerPermissionsHolder<>();
        this.service           = service;
        log.info("Instantiated DTO REST Controller for {}", this.service.dtoType().getSimpleName());
    }

    @PostConstruct
    private void init() {
        extraValidator          = getExtraValidator();
        swaggerValidationCanary = getSwaggerValidationCanary();
        version                 = ControllerVersioning.get().get(this.getClass());
        assert swaggerValidationCanary != null;
        if (permissionsHolder != null) {
            permissionsHolder.setController((C) this);
            permissionsHolder.setContextPath(context.getContextPath());
        }
    }

    @PreDestroy
    private void destroy() {
        version                 = null;
        swaggerValidationCanary = null;
        extraValidator          = null;
        if (permissionsHolder != null) {
            permissionsHolder.setController(null);
        }
    }

    @Override
    public void validate() {
        typedValidate();
    }
    ...
    public <S extends AbstractDtoRestService<E, ID>> S getService() {
        return (S) service;
    }

    /**
     * Gets the type of entities that this controller serves.
     *
     * @return type of data transfer object given as {@link Class}.
     */
    public Class<E> dtoType() {
        if (entityType != null) {
            return entityType;
        } else {
            throw new UnsupportedOperationException(String.format("Class %s is not being instantiated with a concrete type for the generic parameter. " +
                                                                  "%s must override dtoType() and return the appropriate entity class object",
                                                                  AbstractDtoRestController.class.getCanonicalName(),
                                                                  getClass().getCanonicalName()));
        }
    }


private ResponseEntity<List<E>> getAllCommonImpl(HttpHeaders inputHeaders, String selected, String ids, String constraints,
                                                     String orderings, String limits, Optional<Pageable> pageable) {
        DtoEvent entryEvent = null;
        final var outputHeaders = responseHeaders(this::responseHeadersForGetAll);
        try (var t = new MultiFailureThrower().withHttpHeaders(outputHeaders);
             var p = poolStore.getIf(HTTP_HEADERS_STORE_NAME, n -> new SingleObjectPool<>())
                     .new Proxy()) {
            assert t != null;
            p.set(outputHeaders);
            final var konstraints = EntityConstraints.none()
                    .withSelect(selected)
                    .withIds(ids)
                    .withWhere(constraints)
                    .withOrderBy(orderings)
                    .withBetween(limits);
            (entryEvent = new DtoEvent(HttpMethod.GET).withMethodSelector(HttpMethodSelector.ALL)
                    .withInputHeaders(inputHeaders)
                    .withConstraints(konstraints)).raise();
            final List<E> ret = this.getService().getAll(konstraints, pageable);
            final ResponseEntity<List<E>> result;
            result = ResponseEntity.ok(ret);
            return responseWithHeaders(entryEvent, result, outputHeaders);
        } catch (Exception ex) {
            new DtoEvent(HttpMethod.GET).withMethodSelector(HttpMethodSelector.ALL)
                    .withError(ex, entryEvent)
                    .raise();
            throw ex;
        }
    }
    
    
```

