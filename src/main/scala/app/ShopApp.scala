package app

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import routes.LowLevelRestAPI

import scala.util.{Failure, Success}

object ShopApp {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("ShopApp")
    import system.dispatcher

    val api = new LowLevelRestAPI

    val httpBindingFuture = Http().newServerAt("localhost", 8080).bind(api.requestHandler)
    httpBindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        system.log.error(s"Failed to bind HTTP server, because: $ex")
        system.terminate()
    }
  }

}
