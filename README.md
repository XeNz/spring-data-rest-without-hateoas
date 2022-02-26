# spring-data-rest-without-hateoas

## What does this do?

Expose the same spring-data-rest functionality, but with a few changes:
- responses are representatations of entities, no `HATEOAS / HAL` wrappers are included 
- endpoints branch off from `/api/{repository}` path
- Monkey patched (https://en.wikipedia.org/wiki/Monkey_patch) `org.springframework.data.rest.webmvc.config.DelegatingHandlerMapping` due to not being able to edit package-private classes
