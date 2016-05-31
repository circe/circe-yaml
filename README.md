# circe-yaml

This is a small library which translates [SnakeYAML](https://bitbucket.org/asomov/snakeyaml)'s AST into 
[circe](https://github.com/travisbrown/circe)'s AST.  It enables parsing [YAML](https://yaml.org) 1.1 documents
into circe's `Json` AST.

## Why?

YAML is a useful data format for many purposes in which a more readable, less verbose document is desired.  My use
case, for example, is configuration files.

However, you might find circe's way of marshalling into a Scala ADT preferable -- using compile-time specification or
derivation rather than runtime reflection.  This enables you to parse YAML into `Json`, and use your existing (or
circe's generic) `Decoder`s to perform the ADT marshalling.  You can also use circe's `Encoder` to obtain a JSON, and
print that to YAML using this library (though that feature doesn't currently have any tests, as I'm not sure what
the appropriate specification should be).

## Usage

The artifact is hosted on Bintray:

```scala
resolvers += Resolver.bintrayRepo("jeremyrsmith", "maven")
libraryDependencies += "io.github.jeremyrsmith" %% "circe-yaml" % "0.1.0"
```

For better or worse, I simply placed the necessary classes under `io.circe.yaml`:

### Parsing

```scala
import io.circe.yaml.parser.Parser
import io.circe.generic.auto._

case class Nested(one: String, two: BigDecimal)
case class Foo(foo: String, bar: Nested, baz: List[String])

val json = Parser.parse("""
foo: Hello, World
bar:
    one: One Third
    two: 33.333333
baz:
    - Hello
    - World""")
    
println(json.flatMap(_.as[Foo]).valueOr(throw _))
```

Other features of YAML are supported:

* Multiple documents - use `parseDocuments` rather than `parse` to obtain `Stream[Xor[ParsingFailure, Json]]`
* Streaming - use `parse(reader: Reader)` or `parseDocuments(reader: Reader)` to parse from a stream.  Not sure what
  you'll get out of it.
* References / aliases - The reference will be replaced with the complete structure of the alias

### Printing (untested)

```scala
import io.circe.yaml.printer._

val json = io.circe.parser.parse("""{"foo":"bar"}""").valueOr(throw _)

println(json.asYaml)
```

### Limitations

Only JSON-compatible YAML can be used, for obvious reasons:

- Complex keys are not supported (only `String` keys)
- Unlike YAML collections, a JSON array is not the same as a JSON object with integral keys (given the above, it would
  be impossible).  So, a YAML mapping with integral keys will still be a JSON object, and the keys will be strings.

## License

This is released under the Apache 2.0 license, as specified in [the LICENSE file](LICENSE).  It depends on both
circe and SnakeYAML, which each has its own license.  Consult those projects to learn about their licenses.

This library is neither affiliated with, nor endorsed by circe or SnakeYAML.

## Contributing

This library is not heavily tested, as it's a relatively simple transformation.  More tests are welcome, as is any other
contribution.  Simply make a pull request, if you want to.

## Name

I thought about naming this `Medea` after the sorceress who helped Jason defeat the snake who guarded the Golden Fleece
(to bring it in line with Argonaut and Circe - get it? SnakeYAML?).  But, it's really such a small library that it
doesn't warrant its own name.