# Changelog
All notable changes to this project will be documented in this file.<br>
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
- Added `MappingFormat#hasWriter` boolean

## [0.5.1] - 2023-11-30
- Improved documentation
- Fixed ProGuard writer producing invalid files when missing destination names
- Fixed Enigma reader throwing incorrect error message
- Fixed NPE in `MemoryMappingTree`
- Fixed TSRG2 reader not handling multiple passes correctly

## [0.5.0] - 2023-11-15
- Actually marked `HierarchyInfoProvider` as experimental
- Added changelog
- Added SRG and XSRG writer

## [0.5.0-beta.3] - 2023-10-06
- Optimized Enigma directory reader
- Fixed reader being closed when detecting format

## [0.5.0-beta.2] - 2023-10-02
- Added readers for XSRG and CSRG formats
- Added `MappingTreeRemapper`, an ASM remapper wrapper around a `MappingTreeView`
- Added `@Nullable` annotations where applicable; everything not annotated can be assumed null-hostile by default
- Deferred existing Enigma directory deletion to writer visit pass start
- Made `MappingWriter#create` return null instead of throwing an exception
- Moved `MappingTreeRemapper`, `TinyRemapperHierarchyProvider` and `ClassAnalysisDescCompleter` to new `mapping-io-extras` publication
- Fixed TSRG2 reporting itself as not supporting field descriptors
- Fixed regular <-> flat visitor adapter methods

## [0.5.0-beta.1] - 2023-10-09
- Added `VisitableMappingTree` interface
- Added experimental hierarchy propagation support
- Added `endOpIdx` for vars
- Added reader and writer for single Enigma files
- Added some documentation with links to specs for mapping formats
- Added automatic flushing of all writers in `visitEnd`
- Improved `MemoryMappingTree` merging flexibility
- Changed to better error when trying to use a reader on a directory based format
- Prevented instantiation of mapping reader and util classes
- Allowed null destination names for parameters in Enigma format
- `MappingFormat` entries were renamed for consistency, they now end in either `_FILE` or `_DIR`
- Mapping reader and writer classes have been renamed accordingly, and all got moved into new subpackages under `net.fabricmc.mappingio.format`
- Fixed `MemoryMappingTree` methods returning private types
- Fixed handling of absent nested class destination names in Enigma directory reader
- Fixed handling of multiple metadata entries with equal keys
- Fixed SRG reader source descriptor validation
- Fixed crash when trying to read empty TSRG file
- Fixed Enigma and ProGuard mapping file extensions

## [0.4.2] - 2023-05-12
- Published to Maven Central

## [0.4.1] - 2023-05-12
- Added support for ordered `MappingTreeView#accept`

## [0.4.0] - 2023-05-05
- Added support for equal source and destination namespaces in `MemoryMappingTree`'s visitor
- Added ProGuard writer
- Treated `<init>` and `<clinit>` as non-missing in `MappingSourceNsSwitch`
- Fixed `MappingWriter#create` crashing for Tiny v1
- Fixed Tiny v2 serialization of missing lvt row indices

## [0.3.0] - 2021-09-24
- Added Tiny v1 writer
- Added `NEEDS_HEADER_METADATA` flag
- Made `ClassAnalysisDescCompleter`'s ASM version configurable

## [0.2.1] - 2021-09-11
- Allowed args/vars with missing source names in `MappingSourceNsSwitch`

## [0.2.0] - 2021-08-22
- Added `MappingTreeView` (read-only `MappingTree`)
- Added format detecting overloads to `MappingReader`
- Added method to change member source descriptors directly
- Added `MissingDescFilter` and `ClassAnalysisDescCompleter`
- Added support for dropping missing new source names in `MappingSourceNsSwitch` instead of keeping the old source name
- Improved `MemoryMappingTree` merging behavior
- Allowed parameter-only descriptors
- Fixed `MemoryMappingTree#getMethod` using the incorrect flags value

## [0.1.8] - 2021-07-28
- Fixed reader crashes

## [0.1.7] - 2021-07-22
- Fixed dynamic re-initialization with `MemoryMappingTree#setIndexByDstNames`

## [0.1.6] - 2021-07-21
- Fixed Enigma directory writer sometimes emitting extra class declarations

## [0.1.5] - 2021-07-01
- Fixed handling of mappings with zero namespaces
- Fixed matching of arg/var names being too aggressive

## [0.1.4] - 2021-06-19
- Added Enigma directory writer
- Added `throws IOException` to visitor methods

## [0.1.3] - 2021-06-14
- Added option to add missing namespaces in `MappingNsCompleter`
- Fixed `MemoryMappingTree` not saving visited arg/var source names

## [0.1.2] - 2021-06-13
- Added `MappingVisitor` documentation
- Made mapping readers of formats without namespace support use fallback namespaces
- Fixed `MappingNsCompleter` not working correctly

## [0.1.1] - 2021-06-09
- Added `MappingDstNsReorder` adapter
- Added Enigma directory and SRG readers

## [0.1.0] - 2021-06-08
Initial release
