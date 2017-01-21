# circe-yaml

[![Build status](https://travis-ci.org/circe/circe-yaml.svg?branch=master)](https://travis-ci.org/circe/circe-yaml)
[![Codecov status](https://codecov.io/gh/circe/circe-yaml/branch/master/graph/badge.svg)](https://codecov.io/gh/circe/circe-yaml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-yaml_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-yaml_2.12)

This is a small library which translates [SnakeYAML](https://bitbucket.org/asomov/snakeyaml)'s AST into 
[circe](https://github.com/circe/circe)'s AST.  It enables parsing [YAML](https://yaml.org) 1.1 documents into circe's
`Json` AST.

## Why?

YAML is a useful data format for many purposes in which a more readable, less verbose document is desired.  One use
case, for example, is human-readable configuration files.

SnakeYAML provides a Java API for parsing YAML and marshalling its structures into JVM classes. However, you might find 
circe's way of marshalling into a Scala ADT preferable -- using compile-time specification or derivation rather than runtime 
reflection.  This enables you to parse YAML into `Json`, and use your existing (or circe's generic) `Decoder`s to perform 
the ADT marshalling.  You can also use circe's `Encoder` to obtain a `Json`, and print that to YAML using this library.

## Usage

The artifact is hosted by Sonatype, and release versions are synced to Maven Central:

```scala
libraryDependencies += "io.circe" %% "circe-yaml" % "0.4.0"
```

Snapshot versions are available by adding the Sonatype Snapshots resolver:

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
```

### Parsing
Parsing is accomplished through the `io.circe.yaml.parser` package; its API is similar to that of `circe-parser`:

```scala
import io.circe.yaml.parser
val json: Either[ParsingFailure, Json] = parser.parse(yamlString)
```

Additionally, there is a function for parsing multiple YAML documents from a single string:

```scala
val jsons: Stream[Either[ParsingFailure, Json]] = parser.parseDocuments(multiDocumentString)
```

Both of these methods also support a "streaming" parse from a `java.io.Reader` – this is different from the behavior of 
`circe-streaming` (which supports fully asynchronous streaming parsing with iteratees) but does provide a convenient way to 
retrieve YAML from Java inputs:

```scala
val config = getClass.getClassLoader.getResourcesAsStream("config.yml")
val json = parser.parse(new InputStreamReader(config))

val configs = getClass.getClassLoader.getResourceAsStream("configs.yml")
val jsons = parser.parseDocuments(new InputStreamReader(configs))
```

Once you've parsed to `Json`, usage is the same as circe. For example, if you have `circe-generic`, you can do:

```scala
import cats.syntax.either._
import io.circe._
import io.circe.generic.auto._
import io.circe.yaml._

case class Nested(one: String, two: BigDecimal)
case class Foo(foo: String, bar: Nested, baz: List[String])

val json = parser.parse("""
foo: Hello, World
bar:
    one: One Third
    two: 33.333333
baz:
    - Hello
    - World
""")

val foo = json
  .leftMap(err => err: Error)
  .flatMap(_.as[Foo])
  .valueOr(throw _)
```

Other features of YAML are supported:

* Multiple documents - use `parseDocuments` rather than `parse` to obtain `Stream[Xor[ParsingFailure, Json]]`
* Streaming - use `parse(reader: Reader)` or `parseDocuments(reader: Reader)` to parse from a stream.  Not sure what
  you'll get out of it.
* References / aliases - The reference will be replaced with the complete structure of the alias
* Explicit tags (on scalar values only) are handled by converting the tag/scalar pair into a singleton json object:
  ```yaml
  example: !foo bar
  ```
  becomes
  ```json
  { "example": { "foo": "bar" } }
  ```

### Printing
The package `io.circe.yaml.syntax` provides an enrichment to `Json` which supports easily serializing to YAML using common
options:

```scala
import cats.syntax.either._
import io.circe.yaml._
import io.circe.yaml.syntax._

val json = io.circe.parser.parse("""{"foo":"bar"}""").valueOr(throw _)

println(json.asYaml.spaces2) // 2 spaces for each indent level
println(json.asYaml.spaces4) // 4 spaces for each indent level
```

Additionally, there is a class `io.circe.yaml.Printer` which (in similar fashion to circe's `Printer`) can be configured 
with many options which control the `String` output. Its `pretty` method produces a `String` using the configured options:

```
io.circe.yaml.Printer(dropNullKeys = true, mappingStyle = Printer.FlowStyle.Block)
  .pretty(json)
```

### Limitations

Only JSON-compatible YAML can be used, for obvious reasons:

- Complex keys are not supported (only `String` keys)
- Unlike YAML collections, a JSON array is not the same as a JSON object with integral keys (given the above, it would
  be impossible).  So, a YAML mapping with integral keys will still be a JSON object, and the keys will be strings.

## License

This is released under the Apache 2.0 license, as specified in [the LICENSE file](LICENSE).  It depends on both
circe and SnakeYAML, which each has its own license.  Consult those projects to learn about their licenses.

This library is neither endorsed by, nor affiliated with, SnakeYAML.

## Contributing
As part of the [circe](https://github.com/circe/circe) community, circe-yaml supports the [Typelevel](http://typelevel.org/) [code of conduct](http://typelevel.org/conduct.html) and wants all of its channels (Gitter, GitHub, etc.) to be welcoming environments for everyone.

Please read the [circe Contributor's Guide](https://github.com/circe/circe/blob/master/CONTRIBUTING.md) for information about how to submit a pull request.

This circe community module is currently maintained by [Jeremy Smith](https://github.com/jeremyrsmith) and [Jeff May](https://github.com/jeffmay), with guidance from [Travis Brown](https://github.com/travisbrown). It strives to conform as closely as possible to the style of circe itself.
