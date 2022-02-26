package com.datarest.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.*;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceType;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.core.mapping.SupportedHttpMethods;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.*;
import org.springframework.data.rest.webmvc.support.*;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.http.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.PUT;

@RepositoryRestController
@Primary
public class CustomRepositoryRestController implements ApplicationEventPublisherAware {

    private static final String BASE_MAPPING = "api/{repository}";
    private static final List<String> ACCEPT_PATCH_HEADERS = Arrays.asList(//
            RestMediaTypes.MERGE_PATCH_JSON.toString(), //
            RestMediaTypes.JSON_PATCH_JSON.toString(), //
            MediaType.APPLICATION_JSON_VALUE);

    private static final String ACCEPT_HEADER = "Accept";
    private static final String LINK_HEADER = "Link";

    private final RepositoryEntityLinks entityLinks;
    private final RepositoryRestConfiguration config;
    private final HttpHeadersPreparer headersPreparer;
    private final SelfLinkProvider linkProvider;
    private final ResourceStatus resourceStatus;


    private ApplicationEventPublisher publisher;

    @Autowired
    public CustomRepositoryRestController(Repositories repositories,
                                          RepositoryRestConfiguration config,
                                          RepositoryEntityLinks entityLinks,
                                          PagedResourcesAssembler<Object> assembler,
                                          HttpHeadersPreparer headersPreparer,
                                          SelfLinkProvider linkProvider) {

        this.entityLinks = entityLinks;
        this.config = config;
        this.headersPreparer = headersPreparer;
        this.linkProvider = linkProvider;
        this.resourceStatus = ResourceStatus.of(headersPreparer);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
     */
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * <code>OPTIONS /{repository}</code>.
     *
     * @param information
     * @return
     * @since 2.2
     */
    @RequestMapping(value = BASE_MAPPING, method = RequestMethod.OPTIONS)
    public ResponseEntity<?> optionsForCollectionResource(RootResourceInformation information) {

        HttpHeaders headers = new HttpHeaders();
        SupportedHttpMethods supportedMethods = information.getSupportedMethods();

        headers.setAllow(supportedMethods.getMethodsFor(ResourceType.COLLECTION).toSet());

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    /**
     * <code>HEAD /{repository}</code>
     *
     * @param resourceInformation
     * @return
     * @throws HttpRequestMethodNotSupportedException
     * @since 2.2
     */
    @RequestMapping(value = BASE_MAPPING, method = RequestMethod.HEAD)
    public ResponseEntity<?> headCollectionResource(RootResourceInformation resourceInformation,
                                                    DefaultedPageable pageable) throws HttpRequestMethodNotSupportedException {

        resourceInformation.verifySupportedMethod(HttpMethod.HEAD, ResourceType.COLLECTION);

        RepositoryInvoker invoker = resourceInformation.getInvoker();

        if (null == invoker) {
            throw new ResourceNotFoundException();
        }

        Links links = Links.of(getDefaultSelfLink()) //
                .and(getCollectionResourceLinks(resourceInformation, pageable));

        HttpHeaders headers = new HttpHeaders();
        headers.add(LINK_HEADER, links.toString());

        return new ResponseEntity<Object>(headers, HttpStatus.NO_CONTENT);
    }

    /**
     * <code>GET /{repository}</code> - Returns the collection resource (paged or unpaged).
     *
     * @param resourceInformation
     * @param pageable
     * @param sort
     * @return
     * @throws ResourceNotFoundException
     * @throws HttpRequestMethodNotSupportedException
     */
    @ResponseBody
    @RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET)
    public Iterable<?> getCollectionResource(@QuerydslPredicate RootResourceInformation resourceInformation,
                                             DefaultedPageable pageable,
                                             Sort sort)
            throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

        resourceInformation.verifySupportedMethod(HttpMethod.GET, ResourceType.COLLECTION);

        RepositoryInvoker invoker = resourceInformation.getInvoker();

        if (null == invoker) {
            throw new ResourceNotFoundException();
        }

        Iterable<?> results = pageable.getPageable() != null //
                ? invoker.invokeFindAll(pageable.getPageable()) //
                : invoker.invokeFindAll(sort);


        return results;
    }

    /**
     * <code>POST /{repository}</code> - Creates a new entity instances from the collection resource.
     *
     * @param resourceInformation
     * @param payload
     * @param assembler
     * @param acceptHeader
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @ResponseBody
    @RequestMapping(value = BASE_MAPPING, method = RequestMethod.POST)
    public ResponseEntity<?> postCollectionResource(RootResourceInformation resourceInformation,
                                                    PersistentEntityResource payload,
                                                    PersistentEntityResourceAssembler assembler,
                                                    @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
            throws HttpRequestMethodNotSupportedException {

        resourceInformation.verifySupportedMethod(HttpMethod.POST, ResourceType.COLLECTION);

        return createAndReturn(payload.getContent(), resourceInformation.getInvoker(), assembler,
                config.returnBodyOnCreate(acceptHeader));
    }

    /**
     * <code>OPTIONS /{repository}/{id}<code>
     *
     * @param information
     * @return
     * @since 2.2
     */
    @RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> optionsForItemResource(RootResourceInformation information) {

        HttpHeaders headers = new HttpHeaders();
        SupportedHttpMethods supportedMethods = information.getSupportedMethods();

        headers.setAllow(supportedMethods.getMethodsFor(ResourceType.ITEM).toSet());
        headers.put("Accept-Patch", ACCEPT_PATCH_HEADERS);

        return new ResponseEntity<Object>(headers, HttpStatus.OK);
    }

    /**
     * <code>HEAD /{repository}/{id}</code>
     *
     * @param resourceInformation
     * @param id
     * @return
     * @throws HttpRequestMethodNotSupportedException
     * @since 2.2
     */
    @RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<?> headForItemResource(RootResourceInformation resourceInformation,
                                                 @BackendId Serializable id,
                                                 PersistentEntityResourceAssembler assembler) throws HttpRequestMethodNotSupportedException {

        return getItemResource(resourceInformation, id).map(it -> {

            Links links = assembler.toModel(it).getLinks();

            HttpHeaders headers = headersPreparer.prepareHeaders(resourceInformation.getPersistentEntity(), it);
            headers.add(LINK_HEADER, links.toString());

            return new ResponseEntity<Object>(headers, HttpStatus.NO_CONTENT);

        }).orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * <code>GET /{repository}/{id}</code> - Returns a single entity.
     *
     * @param resourceInformation
     * @param id
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getItemResource(RootResourceInformation resourceInformation,
                                             @BackendId Serializable id,
                                             final PersistentEntityResourceAssembler assembler,
                                             @RequestHeader HttpHeaders headers)
            throws HttpRequestMethodNotSupportedException {

        return getItemResource(resourceInformation, id).map(it -> {

            PersistentEntity<?, ?> entity = resourceInformation.getPersistentEntity();

            return resourceStatus.getStatusAndHeaders(headers, it, entity).toResponseEntity(() -> it);

        }).orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * <code>PUT /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
     *
     * @param resourceInformation
     * @param payload
     * @param id
     * @param assembler
     * @param eTag
     * @param acceptHeader
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> putItemResource(RootResourceInformation resourceInformation,
                                             PersistentEntityResource payload,
                                             @BackendId Serializable id,
                                             PersistentEntityResourceAssembler assembler,
                                             ETag eTag,
                                             @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
            throws HttpRequestMethodNotSupportedException {

        resourceInformation.verifySupportedMethod(HttpMethod.PUT, ResourceType.ITEM);

        if (payload.isNew()) {
            resourceInformation.verifyPutForCreation();
        }

        RepositoryInvoker invoker = resourceInformation.getInvoker();
        Object objectToSave = payload.getContent();
        eTag.verify(resourceInformation.getPersistentEntity(), objectToSave);

        return payload.isNew() ? createAndReturn(objectToSave, invoker, assembler, config.returnBodyOnCreate(acceptHeader))
                : saveAndReturn(objectToSave, invoker, PUT, assembler, config.returnBodyOnUpdate(acceptHeader));
    }

    /**
     * <code>PATCH /{repository}/{id}</code> - Updates an existing entity or creates one at exactly that place.
     *
     * @param resourceInformation
     * @param payload
     * @param id
     * @param assembler
     * @param eTag,
     * @param acceptHeader
     * @return
     * @throws HttpRequestMethodNotSupportedException
     * @throws ResourceNotFoundException
     * @throws ETagDoesntMatchException
     */
    @RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<?> patchItemResource(RootResourceInformation resourceInformation,
                                               PersistentEntityResource payload,
                                               @BackendId Serializable id,
                                               PersistentEntityResourceAssembler assembler,
                                               ETag eTag,
                                               @RequestHeader(value = ACCEPT_HEADER, required = false) String acceptHeader)
            throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

        resourceInformation.verifySupportedMethod(HttpMethod.PATCH, ResourceType.ITEM);

        Object domainObject = payload.getContent();

        eTag.verify(resourceInformation.getPersistentEntity(), domainObject);

        return saveAndReturn(domainObject, resourceInformation.getInvoker(), PATCH, assembler,
                config.returnBodyOnUpdate(acceptHeader));
    }

    /**
     * <code>DELETE /{repository}/{id}</code> - Deletes the entity backing the item resource.
     *
     * @param resourceInformation
     * @param id
     * @param eTag
     * @return
     * @throws ResourceNotFoundException
     * @throws HttpRequestMethodNotSupportedException
     * @throws ETagDoesntMatchException
     */
    @RequestMapping(value = BASE_MAPPING + "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteItemResource(RootResourceInformation resourceInformation, @BackendId Serializable id,
                                                ETag eTag) throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {

        resourceInformation.verifySupportedMethod(HttpMethod.DELETE, ResourceType.ITEM);

        RepositoryInvoker invoker = resourceInformation.getInvoker();
        Optional<Object> domainObj = invoker.invokeFindById(id);

        return domainObj.map(it -> {

            PersistentEntity<?, ?> entity = resourceInformation.getPersistentEntity();

            eTag.verify(entity, it);

            publisher.publishEvent(new BeforeDeleteEvent(it));
            invoker.invokeDeleteById(entity.getIdentifierAccessor(it).getIdentifier());
            publisher.publishEvent(new AfterDeleteEvent(it));

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        }).orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * Merges the given incoming object into the given domain object.
     *
     * @param domainObject
     * @param invoker
     * @param httpMethod
     * @return
     */
    private ResponseEntity<?> saveAndReturn(Object domainObject,
                                            RepositoryInvoker invoker,
                                            HttpMethod httpMethod,
                                            PersistentEntityResourceAssembler assembler,
                                            boolean returnBody) {

        publisher.publishEvent(new BeforeSaveEvent(domainObject));
        Object obj = invoker.invokeSave(domainObject);
        publisher.publishEvent(new AfterSaveEvent(obj));

        PersistentEntityResource resource = assembler.toFullResource(obj);
        HttpHeaders headers = headersPreparer.prepareHeaders(Optional.of(resource));

        if (PUT.equals(httpMethod)) {
            addLocationHeader(headers, assembler, obj);
        }

        if (returnBody) {
            return CustomControllerUtils.toResponseEntity(HttpStatus.OK, headers, obj);
        } else {
            return CustomControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT, headers);
        }
    }

    /**
     * Triggers the creation of the domain object and renders it into the response if needed.
     *
     * @param domainObject
     * @param invoker
     * @return
     */
    private ResponseEntity<?> createAndReturn(Object domainObject,
                                              RepositoryInvoker invoker,
                                              PersistentEntityResourceAssembler assembler,
                                              boolean returnBody) {

        publisher.publishEvent(new BeforeCreateEvent(domainObject));
        Object savedObject = invoker.invokeSave(domainObject);
        publisher.publishEvent(new AfterCreateEvent(savedObject));

        Optional<PersistentEntityResource> resource = Optional
                .ofNullable(returnBody ? assembler.toFullResource(savedObject) : null);

        HttpHeaders headers = headersPreparer.prepareHeaders(resource);
        addLocationHeader(headers, assembler, savedObject);

        return CustomControllerUtils.toResponseEntity(HttpStatus.CREATED, headers, savedObject);
    }

    /**
     * Sets the location header pointing to the resource representing the given instance. Will make sure we properly
     * expand the URI template potentially created as self link.
     *
     * @param headers   must not be {@literal null}.
     * @param assembler must not be {@literal null}.
     * @param source    must not be {@literal null}.
     */
    private void addLocationHeader(HttpHeaders headers, PersistentEntityResourceAssembler assembler, Object source) {

        String selfLink = linkProvider.createSelfLinkFor(source).withSelfRel().expand().getHref();
        headers.setLocation(UriTemplate.of(selfLink).expand());
    }

    /**
     * Returns the object backing the item resource for the given {@link RootResourceInformation} and id.
     *
     * @param resourceInformation
     * @param id
     * @return
     * @throws HttpRequestMethodNotSupportedException
     * @throws {@link                                 ResourceNotFoundException}
     */
    private Optional<Object> getItemResource(RootResourceInformation resourceInformation, Serializable id)
            throws HttpRequestMethodNotSupportedException, ResourceNotFoundException {

        resourceInformation.verifySupportedMethod(HttpMethod.GET, ResourceType.ITEM);

        return resourceInformation.getInvoker().invokeFindById(id);
    }

    private Link getDefaultSelfLink() {
        return Link.of(ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
    }

    private Links getCollectionResourceLinks(RootResourceInformation resourceInformation, DefaultedPageable pageable) {

        ResourceMetadata metadata = resourceInformation.getResourceMetadata();
        SearchResourceMappings searchMappings = metadata.getSearchResourceMappings();

        Links links = Links.of(Link.of(ProfileController.getPath(this.config, metadata), ProfileResourceProcessor.PROFILE_REL));

        return searchMappings.isExported() //
                ? links.and(entityLinks.linkFor(metadata.getDomainType()).slash(searchMappings.getPath())
                .withRel(searchMappings.getRel()))
                : links;
    }
}

