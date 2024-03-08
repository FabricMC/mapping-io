# Mapping-IO
Mapping-IO is a small and efficient library for working with deobfuscation mapping files. It has readers and writers for [numerous formats](./src/main/java/net/fabricmc/mappingio/format/MappingFormat.java), and provides extensive mapping manipulation facilities.

The API is inspired by ObjectWeb's [ASM](https://asm.ow2.io/): At its core, Mapping-IO is [visitor-based](./src/main/java/net/fabricmc/mappingio/MappingVisitor.java), but it also provides a [tree API](./src/main/java/net/fabricmc/mappingio/tree/) for in-memory storage and easier data manipulation.

Utilities for more sophisticated use cases can be found in the [mapping-io-extras](./mapping-io-extras/) module; they've been moved out from the core publication due to their additional dependencies.


## Usage
Reading and writing can be easily achieved via the [`MappingReader`](./src/main/java/net/fabricmc/mappingio/MappingReader.java) and [`MappingWriter`](./src/main/java/net/fabricmc/mappingio/MappingWriter.java) interfaces:
```java
MappingReader.read(inputPath, /* optional */ inputFormat,
		MappingWriter.create(outputPath, outputFormat));
```

The above example reads mappings from the input path directly into a `MappingWriter`, writing all contents to disk in the specified format.
Keep in mind that the conversion process might be lossy if the two formats' feature sets differ; see the comparison table [here](./src/main/java/net/fabricmc/mappingio/format/MappingFormat.java) for more details.

You can also read into a tree first:
```java
VisitableMappingTree tree = new MemoryMappingTree();

MappingReader.read(inputPath, inputFormat, tree);

tree.accept(MappingWriter.create(outputPath, outputFormat));
```

If the input format is known beforehand and more direct control over specific reading parameters is desired, the formats' readers (or writers) may also be invoked directly.

Mapping manipulation is achieved either via the tree API or specialized `MappingVisitor`s, hand-crafted or utilizing first-party ones found in the [adapter](./src/main/java/net/fabricmc/mappingio/adapter/) package.

For further information, please consult the project's Javadocs.


### Maven
Mapping-IO is available from the [FabricMC Maven](https://maven.fabricmc.net/net/fabricmc/mapping-io), version 0.4.2 and onwards can also be found on Maven Central.

Gradle snippet:
```gradle
repositories {
	mavenCentral()
}

dependencies {
	api 'net.fabricmc:mapping-io:${mappingio_version}'
}
```


## Comparison with other mapping libraries
Legend:
- ✔: Supported
- ❌: Not supported
- 🚧: Work in progress
- 🙅: No plans


### Format support
| Format                 | Mapping-IO                       | SrgUtils                                        | Lorenz                        |
| ---------------------- | -------------------------------- | ----------------------------------------------- | ----------------------------- |
| Tiny v1                | ✔                               | ✔ (❌: incomplete namespaces, metadata)        | ❌                            |
| Tiny v2                | ✔                               | ✔ (❌: incomplete namespaces; 🙅: variables)   | ❌                            |
| Enigma File            | ✔ (🚧: access modifiers)        | ❌                                              | ✔ (❌: access modifiers)     |
| Enigma Directory       | ✔ (🚧: access modifiers)        | ❌                                              | ❌                            |
| SRG                    | ✔ (🚧: packages)                | ✔                                              | ✔                            |
| XSRG                   | ✔ (🚧: packages)                | ✔                                              | ✔                            |
| CSRG                   | ✔ (🚧: packages)                | ✔                                              | ✔                            |
| TSRG                   | ✔ (🚧: packages)                | ✔                                              | ✔                            |
| TSRG2                  | ✔ (🚧: packages, static marker) | ✔                                              | ❌                            |
| Proguard               | ✔ (🚧: line numbers)            | ✔                                              | ✔ (❌: line numbers, writer) |
| JAM                    | ✔                               | ❌                                              | ✔                            |
| Recaf Simple           | ✔                               | ❌                                              | ❌                            |
| JOBF                   | ✔ (🚧: packages)                | ❌                                              | ❌                            |
| IntelliJ migration map | ✔ (🚧: packages)                | ❌                                              | ❌                            |


### Features
| Feature                                           | Mapping-IO | SrgUtils | Lorenz   |
| ------------------------------------------------- | ---------- | -------- | -------- |
| Multi-namespace support                           | ✔         | ❌       | ❌       |
| Built-in adapters for common operations           | ✔         | ❌       | ❌       |
| More memory-efficient alternative API             | ✔         | ❌       | ❌       |
| Error recovery                                    | 🚧        | ❌       | ❌       |
| Arbitrary metadata                                | 🚧        | ❌       | ❌       |
| Programmatic querying of format capabilities      | 🚧        | ❌       | ❌       |
| Validation and reporting of non-standard contents | 🚧        | ❌       | ❌       |
