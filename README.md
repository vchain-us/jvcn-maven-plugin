# JVCN Maven Plugin
> Maven dependency verification & enforcement via the vChain Code Notary
platform.

This [package](https://github.com/vchain-us/jvcn-maven-plugin) provides a maven plugin for verifying the integrity of your
project dependencies through [code notary](https://www.codenotary.io).

## License
This software is released under [GPL3](https://www.gnu.org/licenses/gpl-3.0.en.html).

## Usage

### Add the plugin dependency to your maven project
```xml
<build>
   ...
   <plugins>
      ...
      <plugin>
         <groupId>us.vchain</groupId>
         <artifactId>jvcn-maven-plugin</artifactId>
         <version>0.0.1</version>
         <executions>
            <execution>
               <id>audit</id>
               <phase>validate</phase>
               <goals>
                  <goal>audit</goal>
               </goals>
            </execution>
         </executions>
      </plugin>
      ... 
   </plugins>
   ...
</build>
```

### Notarize your dependencies

If the build fails stating that some (or all) dependencies could not be verified, perform the following steps to notarize them:
1. Download the dependencies JARs from the Maven repository to your machine
2. Navigate to the [CodeNotary dashboard](https://dashboard.codenotary.io/), create an account if you don't already have one and sign each JAR.
3. Re-run the build. This time all dependencies should be reported as trusted and the build should succeed again.

## Plugin configuration
The plugin provides some configuration options:

##### failOnError (default: true)
Fails the maven build if a single dependency is not signed with the
status `TRUSTED` on the code notary platform.

##### transitive (default: false)
Analyses the entire dependency graph of your project. By default, only direct
dependencies are analysed.

##### requiredSigner (default: none)
Enforces that all checked dependencies must be signed by the provided signer.

## Requirements
The library requires a Java 8 JVM.
