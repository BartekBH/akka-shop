package routes

import actors.Shop._
import domain._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.math.BigDecimal

class LowLevelRestAPI(shopActor: ActorRef, implicit val system: ActorSystem) extends ShopJsonProtocol {

  /**
   * Test data
   */

  val productList = List(
    Product("Steering wheel", "Car parts", BigDecimal("370.00"), 5),
    Product("Blender", "Electronics", BigDecimal("199.99"), 13),
    Product("Laptop", "Electronics", BigDecimal("1750.00"), 3),
    Product("Stool", "Home furnishings", BigDecimal("22.20"), 20)
  )
  productList.foreach(p => shopActor ! CreateProduct(p))

//  implicit val system = ActorSystem("LowLevelServerAPI")
  implicit val metarializer = Materializer
  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/product"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        val productsFuture: Future[List[Product]] = (shopActor ? FindAllProducts).mapTo[List[Product]]
        productsFuture.map { products =>
          HttpResponse(
            // default status 200 OK
            entity = HttpEntity(
              ContentTypes.`application/json`,
              products.toJson.prettyPrint
            )
          )
        }
      }
      else {
        val productId: Option[String] = query.get("id")

        productId match {
          case None => Future(HttpResponse(StatusCodes.NotFound))
          case Some(id: String) =>
            val productFuture: Future[Option[Product]] = (shopActor ? FindProduct(id)).mapTo[Option[Product]]
            productFuture.map {
              case None => HttpResponse(StatusCodes.NotFound)
              case Some(product) =>
                HttpResponse(
                  // default status 200 OK
                  entity = HttpEntity(
                    ContentTypes.`application/json`,
                    product.toJson.prettyPrint
                  )
                )
            }

        }
      }

    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/product"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>
        val productJsonString = strictEntity.data.utf8String
        val product = productJsonString.parseJson.convertTo[Product]

        val productCreatedFuture: Future[ProductCreated] = (shopActor ? CreateProduct(product)).mapTo[ProductCreated]
        productCreatedFuture.map { productCreated =>
          HttpResponse(
          // default status 200 OK
            entity = HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              "New product created with id: " + productCreated.productId
            )
          )
        }
      }

    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/shopping-cart"), _, _, _) =>
      val shoppingCartCreatedFuture: Future[ShoppingCartCreated] = (shopActor ? CreateShoppingCart).mapTo[ShoppingCartCreated]
      shoppingCartCreatedFuture.map { shoppingCartCreated =>
        HttpResponse(
          // default status 200 OK
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "New shopping cart created with id: " + shoppingCartCreated.shoppingCartId
          )
        )
      }

    case request@HttpRequest(HttpMethods.PUT, uri@Uri.Path("/shopping-cart"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        request.discardEntityBytes()
        Future(HttpResponse(
          StatusCodes.NotFound, // 404
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "OOPS! The resource can't be found."
          )
        ))
      } else {
        val shoppingCartId: Option[String] = query.get("shoppingCartId")

        shoppingCartId match {
          case None => Future(HttpResponse(StatusCodes.NotFound))
          case Some(id) =>
            val boughtFuture: Future[Bought] = (shopActor ? BuySC(id)).mapTo[Bought]
            boughtFuture.map { bought =>
              HttpResponse(
                // default status 200 OK
                entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  s"Shopping cart's inventory was bought for ${bought.amount} PLN"
                )
              )
            }
        }
        }


    //shoppingCartId&productId&quantity
    case request@HttpRequest(HttpMethods.PUT, uri@Uri.Path("/shopping-cart/inventory"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        // TODO - show shopping cart's inventory
        request.discardEntityBytes()
        Future(HttpResponse(
          StatusCodes.NotFound, // 404
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "OOPS! The resource can't be found."
          )
        ))
      } else {
        val shoppingCartId: Option[String] = query.get("shoppingCartId")
        val productId: Option[String] = query.get("productId")
        val quantity: Option[Int] = query.get("quantity").map(_.toInt)

        shoppingCartId match {
          case None => Future(HttpResponse(StatusCodes.NotFound))
          case Some(scId: String) => productId match {
            case None => Future(HttpResponse(StatusCodes.NotFound))
            case Some(pId: String) => quantity match {
              case None => Future(HttpResponse(StatusCodes.NotFound))
              case Some(q: Int) =>
                val productAddedFuture: Future[ProductAdded] = (shopActor ? AddProductToSC(scId, pId, q)).mapTo[ProductAdded]
                productAddedFuture.map { productAdded =>
                  HttpResponse(
                    // default status 200 OK
                    entity = HttpEntity(
                      ContentTypes.`text/html(UTF-8)`,
                      s"Product added to shopping cart. Current quantity: " + productAdded.quantity
                    )
                  )
                }
            }
          }
        }
      }

    //shoppingCartId&productId&quantity
    case request@HttpRequest(HttpMethods.DELETE, uri@Uri.Path("/shopping-cart/inventory"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        // TODO - show shopping cart's inventory
        request.discardEntityBytes()
        Future(HttpResponse(
          StatusCodes.NotFound, // 404
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "OOPS! The resource can't be found."
          )
        ))
      } else {
        val shoppingCartId: Option[String] = query.get("shoppingCartId")
        val productId: Option[String] = query.get("productId")
        val quantity: Option[Int] = query.get("quantity").map(_.toInt)

        shoppingCartId match {
          case None => Future(HttpResponse(StatusCodes.NotFound))
          case Some(scId: String) => productId match {
            case None => Future(HttpResponse(StatusCodes.NotFound))
            case Some(pId: String) => quantity match {
              case None => Future(HttpResponse(StatusCodes.NotFound))
              case Some(q: Int) =>
                val productDeletedFuture: Future[ProductDeleted] = (shopActor ? AddProductToSC(scId, pId, q)).mapTo[ProductDeleted]
                productDeletedFuture.map { productAdded =>
                  HttpResponse(
                    // default status 200 OK
                    entity = HttpEntity(
                      ContentTypes.`text/html(UTF-8)`,
                      s"Product deleted from shopping cart. Current quantity: " + productAdded.quantity
                    )
                  )
                }
            }
          }
        }
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
