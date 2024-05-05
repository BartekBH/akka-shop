package routes

import actors.PersistentShoppingCart.Command._
import actors.PersistentShoppingCart.Response._
import actors.Shop._
import domain._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._

import scala.util.{Failure, Success}

class HighLevelRestAPI(shopActor: ActorRef, implicit val system: ActorSystem) extends ShopJsonProtocol {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(5 seconds)
  import system.dispatcher

  /*
    - POST /api/shop/shopping-cart

    - GET  /api/shop/balance
    - GET  /api/shop/products/id | ?id=x

    - PUT  /api/shop/products +entity
    - PUT  /api/shop/buy/id | ?id=x
   */

  val shopServerRoute = {
    pathPrefix("api" / "shop") {
      (post & path("shopping-cart")) {
        val shoppingCartCreatedFuture: Future[ShoppingCartCreatedResponse] = (shopActor ? CreateShoppingCart).mapTo[ShoppingCartCreatedResponse]

        onComplete(shoppingCartCreatedFuture) {
          case Success(shoppingCartCreated) =>
            complete(
              HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "New shopping cart created with id: " + shoppingCartCreated.id
              )
            )
          case Failure(ex) =>
            failWith(ex)
        }
      } ~
      get {
        path("balance") {
          val balanceResponseFuture: Future[GetBalanceResponse] = (shopActor ? GetBalance).mapTo[GetBalanceResponse]

          onComplete(balanceResponseFuture) {
            case Success(balanceResponse) =>
              complete(
                HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  "Current balance: " + balanceResponse.balance
                )
              )
            case Failure(ex) =>
              failWith(ex)
          }
        } ~
        path("products") {
          (path(Remaining) | parameter(Symbol("id"))) { id =>
            val getProductsFuture: Future[GetProductsResponse] = (shopActor ? GetProducts(id)).mapTo[GetProductsResponse]

            onComplete(getProductsFuture) {
              case Success(getProducts) =>
                complete(
                  HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    getProducts.products.map(_._1)
                      .toList
                      .toJson
                      .prettyPrint
                  )
                )
              case Failure(ex) =>
                failWith(ex)
            }
          }
        }
      } ~
      put {
        path("products") {
          ((path(Remaining / IntNumber) | (parameter(Symbol("id")) & parameter(Symbol("quantity").as[Int]))) & extractRequestEntity) { (id, quantity, entity) =>
            val strictEntityFuture = entity.toStrict(3 seconds)
            val productFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[Product])

            onComplete(productFuture) {
              case Success(product) =>
                complete(
                  (shopActor ? AddProduct(id, product, quantity))
                    .map(_ => StatusCodes.OK)
                    .recover {
                      case _ => StatusCodes.InternalServerError
                    }
                )
              case Failure(ex) =>
                failWith(ex)
            }
          }
        } ~
        path("buy") {
          (path(Remaining) | parameter(Symbol("id"))) { id =>
            val buyProductsFuture: Future[ProductsBoughtResponse] = (shopActor ? BuyProducts(id)).mapTo[ProductsBoughtResponse]

            onComplete(buyProductsFuture) {
              case Success(_) =>
                complete(
                  HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    "Shopping cart's inventory is bought"
                  )
                )
              case Failure(ex) =>
                failWith(ex)
            }
          }
        }
      } ~
      pathEndOrSingleSlash {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "Welcome in our shop!"
          )
        )
       }
    }
  }
}



