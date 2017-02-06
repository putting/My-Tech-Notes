#API Design Principles

##Refs
- [link with pass](https://dzone.com/storage/assets/3188681-dzone-guidetomodernjava.pdf)
- [relative](./3188681-dzone-guidetomodernjava.pdf)

Some really good tips for good api design with simple code egs:

- DO NOT RETURN null TO INDICATE THE ABSENCE OF A VALUE

	DO THIS:<p>
	`public Optional<String> getComment() {
 	return Optional.ofNullable(comment);
 	}`
	DON’T DO THIS:<p>
	`public String getComment() {
 	return comment; // comment is nullable
 	}`
- DO NOT USE ARRAYS TO PASS VALUES TO AND FROM THE API

	DO THIS:<p>
	`public Stream<String> comments() {
		return Stream.of(comments);
 	}`
	DON’T DO THIS:<p>
	`public String[] comments() {
 	return comments; // Exposes the backing array!
 	}`

- CONSIDER ADDING STATIC INTERFACE METHODS TO PROVIDE A SINGLE ENTRY POINT FOR OBJECT CREATION

	DO THIS:<p>
	`Point point = Point.of(1,2);`
	DON’T DO THIS:<p>
	`Point point = new PointImpl(1,2);`

- FAVOR COMPOSITION WITH FUNCTIONAL INTERFACES AND LAMBDAS OVER INHERITANCE

	DO THIS:<p>
	`Reader reader = Reader.builder()
	 .withErrorHandler(IOException::printStackTrace)
	 .build();`
	DON’T DO THIS:<p>
	`Reader reader = new AbstractReader() {
	 @Override
	 public void handleError(IOException ioe) {
	 ioe. printStackTrace();
	 }
	 };`

- ENSURE THAT YOU ADD THE @FunctionalInterface ANNOTATION TO FUNCTIONAL INTERFACES

	DO THIS:<p>
	`@FunctionalInterface
	public interface CircleSegmentConstructor {
	 CircleSegment apply(Point cntr, Point p, double ang);
	 // abstract methods cannot be added
	 } `
	DON’T DO THIS:<p>
	`public interface CircleSegmentConstructor {
	 CircleSegment apply(Point cntr, Point p, double ang);
	 // abstract methods may be accidently added later
	} `

- AVOID OVERLOADING METHODS WITH FUNCTIONAL INTERFACES AS PARAMETERS

	DO THIS:<p>
	 `public interface Point {
	 addRenderer(Function<Point, String> renderer);
	 addLogCondition(Predicate<Point> logCondition);
	 }`
	DON’T DO THIS:<p>
	 `public interface Point {
	 add(Function<Point, String> renderer);
	 add(Predicate<Point> logCondition);
	 }`

- AVOID OVERUSING DEFAULT METHODS IN INTERFACES

	DO THIS:<p>
	`public interface Line {
	 Point start();
	 Point end();
	 int length();
	 }`
	DON’T DO THIS:<p>
	 `public interface Line {
	 Point start();
	 Point end();
	 default int length() {
	 int deltaX = start().x() - end().x();
	 int deltaY = start().y() - end().y();
	 return (int) Math.sqrt(
	 deltaX * deltaX + deltaY * deltaY
	 );
	 }
	 }`
- ENSURE THAT THE API METHODS CHECK THE PARAMETER INVARIANTS BEFORE THEY ARE ACTED UPON

	DO THIS:<p>
	`public void addToSegment(Segment segment, Point point) {
	 Objects.requireNonNull(segment);
	 Objects.requireNonNull(point);
	 segment.add(point);
	}`

	DON’T DO THIS:<p>
	`public void addToSegment(Segment segment, Point point) {
	 segment.add(point);
	} `

