package io.circe.yaml

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.BeanAccess

object parser extends Parser(new Yaml())
