package app

import actors.Shop
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import routes.LowLevelRestAPI
import scala.util.{Failure, Success}

object ShopApp {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("ShopApp")
    import system.dispatcher

    val shopActor = system.actorOf(Props[Shop], "shop")
    val api = new LowLevelRestAPI(shopActor, system)

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
