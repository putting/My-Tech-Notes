# Lambda Egs

## Pass in predicates to filter results. p84 Fn prog in java
``` java
public static int totalAssetValues(final List<Asset> assets,
final Predicate<Asset> assetSelector) {
return assets.stream().filter(assetSelector).mapToInt(Asset::getValue).sum();
}
System.out.println("Total of bonds: " +
totalAssetValues(assets, asset -> asset.getType() == AssetType.BOND));
```

## DI Functions in replace of Classes/Services p87
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
  
## Chain Functions together by Composing them. Like a Decorator Pattern
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
  
## Replacing Builder type patterns
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
        final FluentMailer mailer = new FluentMailer();
        block.accept(mailer);
        System.out.println("sending...");
        }
```
    
  
  
