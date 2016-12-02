package io.circe.yaml.snake

import io.circe.Json

/**
  * Converts objects constructed by [[org.yaml.snakeyaml.constructor.BaseConstructor]]
  * into [[Json]].
  *
  * @note You should use [[CirceJsonConverter.default]] if you are using [[CirceJsonConstructor]].
  */
trait CirceJsonConverter {

  /**
    * Convert the constructed value to [[Json]].
    */
  def convert(obj: Any): Json
}

object CirceJsonConverter {

  /**
    * Default converter to be used with [[CirceJsonConverter]].
    */
  val default: CirceJsonConverter = new CirceJsonConverter {
    override def convert(obj: Any): Json = obj.asInstanceOf[Json]
  }
}
