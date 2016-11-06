package io.circe.yaml.snake

import org.yaml.snakeyaml.constructor.BaseConstructor
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver

object SnakeYamlConfigs {

  /**
    * The default configs for SnakeYAML.
    *
    * @note Since SnakeYAML configs are not thread-safe, we generate them anew each time.
    */
  implicit def default: SnakeYamlConfigs = new SnakeYamlConfigs(
    new CirceJsonConstructor,
    new CirceJsonRepresenter,
    new Resolver(),
    CirceJsonConverter.default
  )
}

/**
  * A container for all the configs required to parse Yaml using SnakeYAML.
  *
  * @note This class is not immutable or thread-safe.
  */
class SnakeYamlConfigs(
  val constructor: BaseConstructor,
  val representer: Representer,
  val resolver: Resolver,
  val converter: CirceJsonConverter
)
