package routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import scala.concurrent.Future

class LowLevelRestAPI {

  implicit val system = ActorSystem("LowLevelServerAPI")
  implicit val metarializer = Materializer
  import system.dispatcher

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      Future(HttpResponse(
        // default status 200 OK
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "Hello world"
        )
      ))

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound, // 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "OOPS! The resource can't be found."
        )
      ))
  }

}
