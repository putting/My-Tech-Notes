# Notes on Getter-Setter on @Entity ORM objects

[good discussion of how entities should be](http://stackoverflow.com/questions/6033905/create-the-perfect-jpa-entity)

Ideally we don't want to have setters as we should go for immutability.

However JPA/Hibernate will require access to populate.

I think you can use `@Accesss private myField1` to allow hibernate to access directly.
Otherwise if we do getters/setters on properties hibernate & app can BOTH update.
Then use setMyfield2 where proper setter like validation is required.
