package io.bluebank.braid.corda.rest.docs

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.v3.oas.annotations.media.Schema

const val MESSAGE_DESC = "the error message"
const val TYPE_DESC = "the type of error"

/**
 * A proxy type for Throwables - to work around the insanity of swaggers resolve() algorithm
 */
@ApiModel(value = "Error")
@Schema(name = "Error")
data class BraidSwaggerError(
  @Schema(description = MESSAGE_DESC)
  @ApiModelProperty(MESSAGE_DESC)
  val message: String,

  @Schema(description = TYPE_DESC)
  @ApiModelProperty(TYPE_DESC)
  val type: String
)