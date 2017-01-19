# Single Responsability Principle

[link](https://dzone.com/articles/shades-of-single-responsibility-principle)

Good discussion how to refactor a simple Person class with getInCcy methods. Clearly violates principal.

Then discussion moves to a PersonService with CRUD.

I would keep as ONE svc as the Responsibility is to maintain DB ops on a person.

Maybe break down into CQRS (read and write services).

Also better to separate object into 2 categories:
                a) That ‘know’ something (data holders-DOMAIN). Person POJO
                b) That do something (controllers). PersonService.

Instead of trying to think in terms of responsibilities, we should think in terms of *high cohesion* (better) versus *low cohesion* (worse). Cohesion is a continuum
