# m3u-parser

[![GitHub (pre-)release](https://img.shields.io/github/release/BjoernPetersen/m3u-parser/all.svg)](https://github.com/BjoernPetersen/m3u-parser/releases) [![GitHub license](https://img.shields.io/github/license/BjoernPetersen/m3u-parser.svg)](https://github.com/BjoernPetersen/m3u-parser/blob/master/LICENSE) [![CircleCI branch](https://img.shields.io/circleci/project/github/BjoernPetersen/m3u-parser/master.svg)](https://circleci.com/gh/BjoernPetersen/m3u-parser/tree/master) [![codebeat badge](https://codebeat.co/badges/e6d9054c-0d1a-4798-9b7b-165c104bd708)](https://codebeat.co/projects/github-com-bjoernpetersen-m3u-parser-master) [![codecov](https://codecov.io/gh/BjoernPetersen/m3u-parser/branch/master/graph/badge.svg)](https://codecov.io/gh/BjoernPetersen/m3u-parser)

A parser for simple and extended M3U playlist files written in Kotlin.

This parser can also be used in Java projects.

Due to the underspecified nature of the M3U format, the parser tries to accept all files it gets,
as weird as they may are.
This especially includes extended M3U files with missing "`#EXTM3U`" headers and
mixed extended/simple files.

## Dependency configuration

The library is available in Maven Central (and therefore also JCenter).
Java 8 or higher is required to use it.

### Gradle

#### Kotlin DSL

`build.gradle.kts`

```kotlin
dependencies {
    implementation("com.github.bjoernpetersen:musicbot:${Lib.M3U_PARSER}")
    // or
    implementation(
        group = "com.github.bjoernpetersen",
        name = "m3u-parser",
        version = Lib.M3U_PARSER
    )
}
```

#### Groovy DSL

`build.gradle`

```groovy
dependencies {
    implementation "com.github.bjoernpetersen:m3u-parser:$m3uParserVersion"
}
```

### Maven

`pom.xml`

```xml
<dependency>
    <groupId>com.github.bjoernpetersen</groupId>
    <artifactId>m3u-parser</artifactId>
    <version>${m3uParser.version}</version>
</dependency>
```

## Usage

```kotlin
// Simply pass in a file
val m3uFile = Paths.get("myplaylist.m3u")
val fileEntries: List<M3uEntry> = M3uParser.parse(m3uFile)

// You may also pass in an InputStreamReader
val m3uStream: InputStream = javaClass.getResourceAsStream("myplaylist.m3u")
val m3uReader: InputStreamReader = m3uStream.reader()
val streamEntries: List<M3uEntry> = M3uParser.parse(m3uReader)

// Passing in the content of an M3U file as a String also works
val someApi = TODO("Not a real API object")
// Returns a playlist in the M3U format
val m3uContent: String = someApi.getPlaylist("Best of Willy Astor")
val entries: List<M3uEntry> = M3uParser.parse(m3uContent)
```

### Nested playlists

If your M3U file contains the path of another playlist file

`MyPlaylist.m3u`

```m3u
AnotherPlayList.m3u
```

then you'll get a `M3uEntry` with a `MediaPath` location for that file.
To get the contents of that playlist, you'll need to pass it to the parser again:

```kotlin
val nestedM3uLocation: MediaPath = TODO("...")
M3uParser.parse(nestedM3uLocation.path)
```

You can use the `M3uParser.resolveNestedPlaylists()` method to recursively parse all nested playlist file references:

```kotlin
val entries: List<M3uEntry> = M3uParser.parse(Paths.get("playlist.m3u"))
// Doesn't contain any .m3u file references anymore
val resolvedEntries: List<M3uEntry> = M3uParser.resolveNestedPlaylists(entries)
```

## License

This project is released under the MIT License. That includes every file in this repository,
unless explicitly stated otherwise at the top of a file.
A copy of the license text can be found in the [LICENSE file](LICENSE).
