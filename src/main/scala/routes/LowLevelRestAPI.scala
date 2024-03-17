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

class LowLevelRestAPI(shopActor: ActorRef, implicit val system: ActorSystem) extends ShopJsonProtocol {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(5 seconds)
  import system.dispatcher

  val requestHandler: HttpRequest => Future[HttpResponse] = {

    // CreateShoppingCart
    case HttpRequest(HttpMethods.POST, Uri.Path("/shop/shopping-cart"), _, _, _) =>
      val shoppingCartCreatedFuture: Future[ShoppingCartCreatedResponse] = (shopActor ? CreateShoppingCart).mapTo[ShoppingCartCreatedResponse]
      shoppingCartCreatedFuture.map { shoppingCartCreated =>
        HttpResponse(
          // default status 200 OK
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "New shopping cart created with id: " + shoppingCartCreated.id
          )
        )
      }

    // AddProduct - required: query with shopping cart's id required; entity with product
    case HttpRequest(HttpMethods.PUT, uri@Uri.Path("/shopping-cart/products"), _, entity, _) =>
      val query = uri.query()
      val shoppingCartId: Option[String] = query.get("id")
      val productQuantity: Option[Int] = query.get("quantity").map(_.toInt)

      val addProductResponseFuture: Option[Future[HttpResponse]] = for {
        id <- shoppingCartId
        quantity <- productQuantity
      } yield {
        val strictEntityFuture = entity.toStrict(3 seconds)
        strictEntityFuture.flatMap { strictEntity =>
          val productJsonString = strictEntity.data.utf8String
          val product = productJsonString.parseJson.convertTo[Product]

          val productAddedFuture = (shopActor ? AddProduct(id, product, quantity))
          productAddedFuture.map {
            case ProductAddedResponse =>
              HttpResponse(
                // default status 200 OK
                entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  "Product added to shopping cart"
                )
              )
            case ShoppingCartNotExistsResponse =>
              HttpResponse(
                StatusCodes.NotFound, // 404
                entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  "Shopping cart not found"
                )
              )
          }
        }
      }
      addProductResponseFuture.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))

    // BuyProducts - required: query with shopping cart's id required
    case HttpRequest(HttpMethods.PUT, uri@Uri.Path("/shopping-cart/buy"), _, _, _) =>
      val query = uri.query()
      val maybeId = query.get("id")

      maybeId match {
        case None => Future(HttpResponse(StatusCodes.BadRequest))
        case Some(id) =>
          val buyProductsFuture: Future[ProductsBoughtResponse] = (shopActor ? BuyProducts(id)).mapTo[ProductsBoughtResponse]
          buyProductsFuture.map { _ =>
            HttpResponse(
              // default status 200 OK
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "Shopping cart's inventory is bought"
              )
            )
          }
      }

    // GetProducts - required: query with shopping cart's id required
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/shopping-cart/products"), _, _, _) =>
      val query = uri.query()
      val maybeId = query.get("id")

      maybeId match {
        case None => Future(HttpResponse(StatusCodes.BadRequest))
        case Some(id) =>
          val getProductsFuture: Future[GetProductsResponse] = (shopActor ? GetProducts(id)).mapTo[GetProductsResponse]
          getProductsFuture.map { getProductsResponse =>
            HttpResponse(
              // default status 200 OK
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                getProductsResponse.products.map(_._1)
                  .toList
                  .toJson
                  .prettyPrint
              )
            )
          }
      }

    // GetBalance
    case HttpRequest(HttpMethods.GET, Uri.Path("/shop/balance"), _, _, _) =>
      val balanceResponseFuture: Future[GetBalanceResponse] = (shopActor ? GetBalance).mapTo[GetBalanceResponse]
      balanceResponseFuture.map { getBalance =>
        HttpResponse(
          // default status 200 OK
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "Current balance: " + getBalance.balance
          )
        )
      }

    // home page
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      Future(HttpResponse(
        // default status 200 OK
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "Welcome in our shop!"
        )
      ))

    // page not found
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
