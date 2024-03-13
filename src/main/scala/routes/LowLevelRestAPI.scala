package routes

import actors.PersistentShoppingCart.Command.{AddProduct, BuyProducts, GetProducts}
import actors.PersistentShoppingCart.Response.{GetProductsResponse, ProductAddedResponse, ProductsBoughtResponse}
import actors.Shop._
import domain._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import spray.json._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math.BigDecimal

class LowLevelRestAPI(shopActor: ActorRef, implicit val system: ActorSystem) extends ShopJsonProtocol {

  implicit val metarializer = Materializer
  implicit val timeout = Timeout(5 seconds)
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


    // AddProduct ?id=&quantity=   +entity
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

          val productAddedFuture: Future[ProductAddedResponse] = (shopActor ? AddProduct(id, product, quantity)).mapTo[ProductAddedResponse]
          productAddedFuture.map { _ =>
            HttpResponse(
              // default status 200 OK
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "Product added to shopping cart"
              )
            )
          }
        }
      }

      addProductResponseFuture.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))

    // BuyProducts ?id=
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


    // GetProducts ?id=
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


    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      Future(HttpResponse(
        // default status 200 OK
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "Welcome in our shop!"
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
