# Mapping IO
`mapping-io` is a library for working with deobfuscation mapping files. It can read and write numerous formats, such as:
- `Tiny` ([example](https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/1.18.tiny)),
- `Tiny v2` ([example + spec](https://github.com/FabricMC/tiny-remapper/issues/9)),
- `Enigma` ([example](https://github.com/FabricMC/yarn/blob/1.19-pre4/mappings/net/minecraft/block/Blocks.mapping))
- `Proguard` ([example + spec](https://www.guardsquare.com/manual/tools/retrace#specifications)).

A list of all currently supported formats can be observed [here](./src/main/java/net/fabricmc/mappingio/format).

## Docs
🧾 If you wish to read and work with existing mapping files, head over to [Reading Mappings](./docs/reading-mappings.md).<br>
✍️ If you wish to write mappings out to a new file, please visit [Writing Mappings](./docs/writing-mappings.md).