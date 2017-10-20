# Maven

## Checking the maven version in code
Contains some good pattern matching code

```java
public class MavenConsistencyCheckTest {
    private static final String EXPECTED_MAVEN_VERSION = "3.5.0";

    @Test
//    @Ignore("Breaks intellij version 3.3.9. Do we all really want to upgrade?")
    public void checkAllMavenVersionsAreConsistent() throws Exception {
        checkMavenVersionInFile("../.mvn/wrapper/maven-wrapper.properties", ".*apache-maven-([0-9.]+)-bin.zip");
        checkMavenVersionInFile("../../.gitlab-ci.yml", ".*image: maven:([0-9.]+)-jdk-8");

        String mavenVersion = System.getProperty("build.maven.version");
        assumeNotNull(mavenVersion);
//        assertThat(mavenVersion).describedAs("Build is running using the wrong version of Maven, swtich to " + EXPECTED_MAVEN_VERSION).isEqualTo(EXPECTED_MAVEN_VERSION);
    }

    // This is a duplicate of some code in dali, if you copy it again then apply the boy scout rule!
    private void checkMavenVersionInFile(String path, String patternContainingVersion) throws IOException {
        Pattern versionMatcher = Pattern.compile(patternContainingVersion);

        List<Matcher> matchingLines = Files.readLines(new File(path), Charset.defaultCharset())
                .stream()
                .map(versionMatcher::matcher)
                .filter(Matcher::matches)
                .collect(toList());

        assertThat(matchingLines).describedAs(path + " does not have any lines matching " + patternContainingVersion).isNotEmpty();
        assertThat(matchingLines).allMatch(m -> m.group(1).equals(EXPECTED_MAVEN_VERSION), "does not match maven version " + EXPECTED_MAVEN_VERSION);
    }
}
```
