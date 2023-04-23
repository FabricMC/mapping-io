# Writing mappings

## Prerequisites
For the sake of this tutorial, let's assume you have made the following renames in your project:
```
class_1              → GoldBlock
├── field_1          → hardness
├── method_1         → getBlockType
├── method_2         → addTag
│   ├── argument_1   → tag
│   ├── argument_2   → overwriteExistingTags
│   ├── variable_1   → counter
```


## Providing the data
In order to export your mappings to a file, you first have to create a new `MemoryMappingTree` instance:
```java
MemoryMappingTree tree = new MemoryMappingTree();
```
All our following operations will be done using this tree.


### Header data
Before we can tell our tree instance which renames we've made, we have to provide some basic metadata. This is usually what ends up in the mapping files' first row, like here for the `Tiny` format:
```
v1	official	intermediary
```
Some columns, like the `v1` here, are part of the individual mapping formats' specifications and are being taken care of by their respective `Writer`s automatically. Other parts, like the `official` and `intermediary` here, have to be specified by us library consumers manually. They represent the names of our source and target mappings.
Generally it's recommended to go with `source` for the original source names and `target` for your new names, but it doesn't really matter. Passing this information to our tree can be done the following way:
```java
tree.visitHeader();
tree.visitNamespaces("source", Arrays.asList("target"));
```


### Mapping data
Right now, our tree's content is empty, so let's start by adding our first mapping. We always have to start with the classes, as they are highest up in the hierarchy. Note that, since we've previously written header data, we first have to tell the tree that we're adding actual mapping content now:
```java
tree.visitContent();
tree.visitClass("somepackage/class_1");
```
This adds our class to the tree's internal state, from where we can perform perform further actions, like providing a new name and/or adding a comment etc.
Since we do in fact have a new name to apply, we can tell the tree to assign that to the current (last visited) class:
```java
tree.visitDstName(MappedElementKind.CLASS, 0, "GoldBlock");
```
`0` is the index of the namespace we wish to add this name to (see [Header Data](#header-data)), in our case that is the "target" namespace/column.


#### Fields and methods
From here on, it's basically the same concept: Visit the corresponding parent class first (which we've already done above), then all of its fields and methods you wish to add renames for:
```java
// Fields
tree.visitField("field_1", fieldDescriptorOf("field_1"));
tree.visitDstName(MappedElementKind.FIELD, 0, "hardness");

// Methods
tree.visitMethod("method_1", methodDescriptorOf("method_1"));
tree.visitDstName(MappedElementKind.METHOD, 0, "getBlockType");
tree.visitMethod("method_1", methodDescriptorOf("method_2"));
tree.visitDstName(MappedElementKind.METHOD, 0, "addTag");
```
You may be confused by the `fieldDescriptorOf(...)` and `methodDescriptorOf(...)` methods. These are not part of mapping-io, you need to implement them yourself. See [this presentation](https://web.archive.org/web/20221108160338/https://courses.cs.ut.ee/MTAT.05.085/2014_spring/uploads/Main/Bytecode%20with%20ASM.pdf) for more information. Descriptors are needed to ensure your fields' and methods' `"source" <-> "target"` mappings are unambiguous and uniquely assignable - otherwise you wouldn't be able to distinguish between `void myMethod(String string)` and `boolean myMethod(int num)` for example, as both methods have the same name.


#### Method arguments and variables
Here's where things get tricky: Up until now, using the original source names (and sometimes a descriptor) were enough to uniquely identify a mappable element. Method args and vars on the other hand aren't guaranteed to even have names, so we can't rely on that factor anymore. As a solution, a combination of:
- the method arg position
- the local variable index
- and/or the opcode of the variable

are used.

##### Method args
```java
// Make sure you've visited the method you
// intend to add these args to beforehand!

tree.visitMethodArg(argPosition, lvIndex, srcName);
// - argPosition always starts at 0 and gets incremented
//   by 1 for each additional arg.
// - lvIndex starts at 0 for static methods, 1 otherwise.
//   For each additional arg, it gets incremented by 1,
//   or by 2 if it's a primitive long or double.
// - srcName is optional (at least for the Tiny v2 format).

tree.visitMethodArg(0, 0, "argument_1");
tree.visitDstName(MappedElementKind.METHOD_ARG, 0, "tag");
tree.visitMethodArg(1, 1, "argument_2");
tree.visitDstName(MappedElementKind.METHOD_ARG, 0, "overwriteExistingTags");
```

##### Method vars
```java
// Make sure you've visited the method you
// intend to add these vars to beforehand!

tree.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, srcName)
// - lvtRowIndex is the variable's index in the method's LVT
//   (local variable table). It is optional, so you can pass -1 instead.
//   This is the case since LVTs themselves are optional debug information, see
//   https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.13
// - lvIndex is the local variable's index in the current method.
//   For each additional variable, it gets incremented by 1,
//   or by 2 if it's a primitive long or double.
// - startOpIndex is required for cases when lvIndex alone doesn't
//   uniquely identify a local variable. This is the case when vars
//   get re-defined later on, in which case most decompilers opt to
//   not re-define the existing var, but instead generate a new one.
// - srcName is once again optional, we Java bytecode
//   doesn't require local vars to have an associated name

tree.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, "variable_1");
tree.visitDstName(MappedElementKind.METHOD_VAR, 0, "counter");
```

#### Comments
All tokens can be assigned comments to via `visitComment(...)`:
```java
tree.visitField("field_1", fieldDescriptorOf("field_1"));
tree.visitComment(MappedElementKind.FIELD, "I'm a comment");
```


## Exporting to a file
After all your renames have been passed to the tree object, we can finally write everything to a file. For this, create a new `MappingWriter` instance:
```java
tree.visitEnd()
MappingWriter writer = MappingWriter.create(outputFilePath, MappingFormat.TINY_2);
```
You can choose whatever mapping format you like, but note that not all formats support all features. Generally it's best to go with Tiny v2, as it has the largest feature set.

Let's pass this writer to the tree's `accept` method:
```java
tree.accept(writer);
```
This automatically writes all of our mappings to the above specified output path.

The only thing that's left now is to close the writer:
```java
writer.close();
```

Congratulations! You've successfully exported your first set of mappings! :D 🏅 