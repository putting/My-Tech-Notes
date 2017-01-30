# Lambda Egs From Functional Programming in Java by Venkat
Some good ideas and well worth re-caping.

## Chapter 4: Lambdas
### Pass in predicates to filter results. p84 Fn prog in java
``` java
public static int totalAssetValues(final List<Asset> assets,
final Predicate<Asset> assetSelector) {
return assets.stream().filter(assetSelector).mapToInt(Asset::getValue).sum();
}
System.out.println("Total of bonds: " +
totalAssetValues(assets, asset -> asset.getType() == AssetType.BOND));
```

### DI Functions in replace of Classes/Services p87
``` java
  private Function<String, BigDecimal> priceFinder; //stored on class as Lookup svc
  new CalculateNAV(ticker -> new BigDecimal("6.01"));  //Can Stub/Test easily 
  //Using a proper YahooFinance and method getPrice injected easily
  final URL url =
    new URL("http://ichart.finance.yahoo.com/table.csv?s=" + ticker);
    final BufferedReader reader =
    new BufferedReader(new InputStreamReader(url.openStream()));
    final String data = reader.lines().skip(1).findFirst().get();
    final String[] dataItems = data.split(",");
    return new BigDecimal(dataItems[dataItems.length - 1])
    //called like this
    new CalculateNAV(YahooFinance::getPrice);
```
  
### Chain Functions together by Composing them. Like a Decorator Pattern
  They can then be applied in order.
  Its like having a collection of objects (or rather methods), but fluently built up as needed.
  
``` java 
//Could take a varargs of fns. Creates a Function filter. Not yet execed
  public void setFilters(final Function<Color, Color>... filters) {
    filter =
    Arrays.asList(filters).stream()
    .reduce((filter, next) -> filter.compose(next))
    .orElse(color -> color);
    //NB. How a Consumer can act instead of a util static print method
    final Camera camera = new Camera();
    final Consumer<String> printCaptured = (filterInfo) ->
    System.out.println(String.format("with %s: %s", filterInfo,
    camera.capture(new Color(200, 100, 200))));
    //eg running
    final Camera camera = new Camera();
    final Consumer<String> printCaptured = (filterInfo) ->
    System.out.println(String.format("with %s: %s", filterInfo,
    camera.capture(new Color(200, 100, 200))));
    //or even better with the colour method refs
    camera.setFilters(Color::brighter, Color::darker);
```
  
### Replacing Builder type patterns
  - Smell 1: Noisy. `Mailer mailer = new Mailer(); mailer.from("build@a.com"); mailer.to("build@b.com");`
  - Smell 2: What can we do with mailer. re-use it for another mail? `mailer.send(); `
  - Refactor 1: Chain (builder style) return **this** from each method. `new MailBuilder().from("build@a.com").to( etc....`
    - Smell 3: New reduces fluency
    - Smell 4: can use instance var of mailer for other stuff?
    - Solution
      - a) Private Constructor
      - b) Use chaining as above
      - c) send is a static method which accepts Consumer
``` java
      public static void send(final Consumer<FluentMailer> block) {
        final FluentMailer mailer = new FluentMailer();  //NB. This is the input param to the Consumer
        block.accept(mailer);
        System.out.println("sending...");
        }
    FluentMailer.send(mailer ->
      mailer.from("build@agiledeveloper.com")
      .to("venkats@agiledeveloper.com")
      .subject("build notification")
      .body("...much better...")); 
```
### Handling Checked Exceptions p86
  - You wrap a lambda with Try, returning a msg - BUT requires inspecting return for errors.
  - ReThrow with Runtime EX. I think static helpers may be useful.
  - define own Thrwable Functioonal Interfaces: `UseInstance<T, X extends Throwable>`. See later chapter p95 Higher Order
  - Parallel/Concurrent much more tricky.

## Chapter 5: Resources  
We’ll use lambda expressions to implement the execute around method pattern, which gives us better control over sequencing of operations.

### Closing resources properly without relying on try() using Execute Around approach.
``` java
//Use of higher-order fn. FileWriterEAM has static constructor and close. Hence this Builder method:
  public static void use(final String fileName,
  final UseInstance<FileWriterEAM, IOException> block) throws IOException {
  final FileWriterEAM writerEAM = new FileWriterEAM(fileName);
  try {
  block.accept(writerEAM);
  } finally {
  writerEAM.close();
  }
  @FunctionalInterface
  public interface UseInstance<T, X extends Throwable> {
  void accept(T instance) throws X;
  }
  }
  //Running
  FileWriterEAM.use("eam.txt", writerEAM -> writerEAM.writeStuff("sweet"));
``` 
### Managing Locks. Again Excute Around
It is especially important to have timeouts on Locks.otherwise Deadlock etc..
- Nice utlility method for running items with a lock.
``` java
    public class Locker {
    public static void runLocked(Lock lock, Runnable block) {
    lock.lock();
    try {
    block.run();
    } finally {
 //Running
    runLocked(lock, () -> {/*...critical code ... */});
```
- Tests (useful utilities on p102) which asserts that the correct class/method through the ex.
    Eg given `assertThrows(RodCutterException.class, () -> rodCutter.maxProfit(0));`
    
