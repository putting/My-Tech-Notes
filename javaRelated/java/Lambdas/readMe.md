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
