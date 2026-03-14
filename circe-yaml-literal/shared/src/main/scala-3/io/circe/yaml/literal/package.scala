package io.circe.yaml.literal

import io.circe.Json

extension (inline sc: StringContext)
  inline def yaml(inline args: Any*): Json =
    ${ YamlLiteralMacros.yamlImpl('sc, 'args) }
