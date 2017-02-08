# Api's

## Fluent Api's Chaining
  See the Mailer eg form p83 from Book Fn Prog in Java.
  See how the use of return this, Plus the use of Consumer<FluentMailer> works.
``` java
FluentMailer.send(mailer ->
mailer.from("build@agiledeveloper.com")
  .to("venkats@agiledeveloper.com")
  .subject("build notification")
  .body("...much better..."));
```
