package ai.aiminder.aiminderserver.common.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
class MapToJsonConverter(private val objectMapper: ObjectMapper) : Converter<Map<String, String>, Json> {
  override fun convert(source: Map<String, String>): Json = Json.of(objectMapper.writeValueAsString(source))
}

@Component
@ReadingConverter
class JsonToMapConverter(private val objectMapper: ObjectMapper) : Converter<Json, Map<String, String>> {
  override fun convert(source: Json): Map<String, String> =
    objectMapper.readValue(source.asString(), object : TypeReference<Map<String, String>>() {})
}
