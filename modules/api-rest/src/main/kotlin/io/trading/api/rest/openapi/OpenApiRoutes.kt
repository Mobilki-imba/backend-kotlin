package io.trading.api.rest.openapi

import io.ktor.http.ContentType
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.utils.io.charsets.Charsets

fun Route.openApiRoutes() {
    get("/docs/openapi.yaml") {
        val resource = this::class.java.classLoader
            .getResourceAsStream("openapi/openapi.yaml")
            ?: return@get call.respondText("not found", status = io.ktor.http.HttpStatusCode.NotFound)
        val bytes = resource.use { it.readBytes() }
        call.respondBytes(bytes, contentType = ContentType.parse("application/yaml"))
    }
    get("/docs") {
        call.respondText(SWAGGER_HTML, contentType = ContentType.Text.Html)
    }
}

private val SWAGGER_HTML = """
<!DOCTYPE html>
<html>
<head>
  <title>Trading BFF API</title>
  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css"/>
</head>
<body>
  <div id="swagger-ui"></div>
  <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
  <script>
    window.onload = () => {
      SwaggerUIBundle({ url: '/docs/openapi.yaml', dom_id: '#swagger-ui' });
    };
  </script>
</body>
</html>
""".trimIndent()
