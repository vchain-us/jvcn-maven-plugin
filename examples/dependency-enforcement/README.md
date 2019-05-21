# Dependency Enforcement Example
JVCN can be used to enforce maven dependencies. 

In this project, [CodeNotary](https://www.codenotary.io) is used to make sure
all project dependencies are trusted.

<hr>

A closer look into `pom.xml` shows that there are a couple of things going on:

* The `jvcn-maven plugin` is hooked into the maven build process to automatically
verify your dependencies at build time.
* The plugin is set-up to verify all dependencies against a signer with the 
public-key `0xC498EEEbDcBd4047b7393573dB21099cE42638be`. 
* It is also configured to stop the build if it encounters a single non-trusted
dependency.

In this setup, there is a single dependency: `us.vchain:jvcn:0.0.1`. This dependency
was signed by `0xC498EEEbDcBd4047b7393573dB21099cE42638be` - the build passes.

By adding another (unknown) dependency to the project, you can see build 
enforcement in action. Add the following snippet to the dependencies list in 
`pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.2.5</version>
</dependency>
```

The build fails, because the postgresql dependency was not signed by 
`0xC498EEEbDcBd4047b7393573dB21099cE42638be`.
