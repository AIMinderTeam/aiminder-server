package ai.aiminder.aiminderserver.common.exception

import ai.aiminder.aiminderserver.common.error.CommonError.InternalServerError
import ai.aiminder.aiminderserver.common.error.CommonError.InvalidMediaType
import ai.aiminder.aiminderserver.common.error.CommonError.InvalidMethod
import ai.aiminder.aiminderserver.common.error.CommonError.InvalidRequest
import ai.aiminder.aiminderserver.common.error.CommonError.NoResourceFound
import ai.aiminder.aiminderserver.common.error.ServiceError
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.MethodNotAllowedException
import org.springframework.web.server.MissingRequestValueException
import org.springframework.web.server.ServerErrorException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import reactor.core.publisher.Mono

@Component
@Order(-2)
class GlobalExceptionHandler(
  globalErrorAttributes: DefaultErrorAttributes,
  applicationContext: ApplicationContext,
  serverCodecConfigurer: ServerCodecConfigurer,
) : AbstractErrorWebExceptionHandler(globalErrorAttributes, WebProperties.Resources(), applicationContext) {
  init {
    super.setMessageReaders(serverCodecConfigurer.readers)
    super.setMessageWriters(serverCodecConfigurer.writers)
  }

  private val logger = logger()

  override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse?>? =
    RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)

  private fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
    val response: Mono<ServerResponse> =
      when (val error = getError(request)) {
        is ServiceError -> {
          logger.debug("ServiceError occurred: ${error.code} - ${error.message}", error)
          createServerResponse(error)
        }

        is WebExchangeBindException -> {
          logger.debug("Binding error occurred: ${handleBindingErrors(error)}", error)
          createServerResponse(InvalidRequest(handleBindingErrors(error)))
        }

        is MissingRequestValueException -> {
          logger.debug("Missing request value: ${error.reason}", error)
          createServerResponse(InvalidRequest(error.reason))
        }

        is ServerWebInputException -> {
          logger.debug("Invalid input: ${error.cause?.message}", error)
          createServerResponse(InvalidRequest(error.cause?.message))
        }

        is NoResourceFoundException -> {
          logger.debug("Resource not found", error)
          createServerResponse(NoResourceFound())
        }

        is MethodNotAllowedException -> {
          logger.debug("Method not allowed: ${error.cause?.message}", error)
          createServerResponse(InvalidMethod(error.cause?.message))
        }

        is UnsupportedMediaTypeStatusException -> {
          logger.debug("Unsupported media type: ${error.cause?.message}", error)
          createServerResponse(InvalidMediaType(error.cause?.message))
        }

        is ServerErrorException -> {
          logger.error("Server error occurred", error)
          createServerResponse(InternalServerError(error.cause?.message))
        }

        else -> {
          logger.error("Unknown error occurred", error)
          createServerResponse(InternalServerError(error?.cause?.message))
        }
      }

    return response
  }

  private fun handleBindingErrors(error: WebExchangeBindException): String =
    error
      .bindingResult
      .allErrors
      .map { "${it.codes?.first() ?: "request"} : ${it.defaultMessage}" }
      .toString()

  private fun createServerResponse(serviceError: ServiceError) =
    ServerResponse
      .status(serviceError.status)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(ServiceResponse.from<Unit>(serviceError))
}
