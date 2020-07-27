# Java Service Provider Interface

Java 6 has introduced a feature for discovering and loading implementations matching a given interface: Service Provider Interface (SPI).
An aoi which can provide to client code a ist of implementations.

## Google AutoService
This is an annotation processor library that helps us with generating Java Service Provider Interface (SPI) configuration files.
Google AutoService is an open source code generator tool, developed under the Google Auto project. There are also two other tools besides AutoService: AutoValue and AutoFactory.

The purpose of this library is to save effort and time and, at the same time, to prevent misconfiguration

https://www.baeldung.com/google-autoservice

The eg given is a bunch of Ccy providers, which is listed to the client and xrates given.

https://www.baeldung.com/java-spi

## Comment
Simple means for lazily providing implementations. **PROVIDERS being th operative word**

## Java has lots of these providers

- CurrencyNameProvider: provides localized currency symbols for the Currency class.
- LocaleNameProvider: provides localized names for the Locale class.
- TimeZoneNameProvider: provides localized time zone names for the TimeZone class.
- DateFormatProvider: provides date and time formats for a specified locale.
- NumberFormatProvider: provides monetary, integer and percentage values for the NumberFormat class.
- Driver: as of version 4.0, the JDBC API supports the SPI pattern. Older versions uses the Class.forName() method to load drivers.
- PersistenceProvider: provides the implementation of the JPA API.
- JsonProvider: provides JSON processing objects.
- JsonbProvider: provides JSON binding objects.
- Extention: provides extensions for the CDI container.
- ConfigSourceProvider: provides a source for retrieving configuration properties.
