package actors

import actors.PersistentShoppingCart.Response.ProductAddedResponse
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import domain.Product

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Shop {

  // commands = messages
  trait Command
  import actors.PersistentShoppingCart.Command._
  case object CreateShoppingCart extends Command
  case object GetBalance extends Command

  /*
  // events (to persist to Cassandra)
  trait Event
  case class ShoppingCartCreated(id: String) extends Event
   */

  // responses
  trait Response

  import actors.PersistentShoppingCart.Response._
  case class ShoppingCartCreatedResponse(id: String) extends Response
  case class GetBalanceResponse(balance: BigDecimal) extends Response
  case class ShoppingCartNotExistsResponse(id: String) extends Response
}

class Shop extends Actor with ActorLogging {
  import Shop._
  import PersistentShoppingCart.Command._
  import PersistentShoppingCart.Response._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(2 seconds)


  val shoppingCarts: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  var balance: BigDecimal = BigDecimal("0")

  override def receive: Receive = {
    case CreateShoppingCart =>
      val replyTo: ActorRef = sender
      val id: String = UUID.randomUUID().toString
      val newShoppingCart = context.actorOf(Props[PersistentShoppingCart], id)
      shoppingCarts += (id -> newShoppingCart)

      log.info(s"Shopping cart created with id: $id")
      replyTo ! ShoppingCartCreatedResponse(id)

    case command@AddProduct(id, _, _) =>
      val replyTo: ActorRef = sender
      val maybeShoppingCart: Option[ActorRef] = shoppingCarts.get(id)
      maybeShoppingCart match {
        case None =>
          log.info(s"Shopping cart with id: $id don't exists")
          replyTo ! ShoppingCartNotExistsResponse
        case Some(shoppingCart) =>
          val addProductFuture: Future[ProductAddedResponse] = (shoppingCart ? command).mapTo[ProductAddedResponse]
          addProductFuture.onComplete {
            case Success(response) =>
              log.info(s"Product added to shopping cart with id: $id")
              replyTo ! response
            case Failure(ex) =>
              log.warning(s"Adding product failed: $ex")
          }
      }

    case command@BuyProducts(id) =>
      val replyTo: ActorRef = sender
      val maybeShoppingCart: Option[ActorRef] = shoppingCarts.get(id)
      maybeShoppingCart match {
        case None =>
          log.info(s"Shopping cart with id: $id don't exists")
          replyTo ! ShoppingCartNotExistsResponse
        case Some(shoppingCart) =>
          val buyProductsFuture: Future[ProductsBoughtResponse] = (shoppingCart ? command).mapTo[ProductsBoughtResponse]
          buyProductsFuture.onComplete {
            case Success(response) =>
              val currentBalance = balance
              response.products.foreach { p =>
                val price = p._1.price
                val quantity = p._2
                balance += price * quantity
              }
              log.info(s"Inventory of shopping cart $id was bought for ${balance - currentBalance}")
              replyTo ! response
            case Failure(ex) =>
              log.warning(s"Buying products failed: $ex")
          }
      }

    case command@GetProducts(id) =>
      val replyTo: ActorRef = sender
      val maybeShoppingCart: Option[ActorRef] = shoppingCarts.get(id)
      maybeShoppingCart match {
        case None =>
          log.info(s"Shopping cart with id: $id don't exists")
          replyTo ! ShoppingCartNotExistsResponse
        case Some(shoppingCart) =>
          val getProductsFuture: Future[GetProductsResponse] = (shoppingCart ? command).mapTo[GetProductsResponse]
          getProductsFuture.onComplete {
            case Success(response) =>
              log.info(s"Getting products from shopping cart with id: $id")
              replyTo ! response
            case Failure(ex) =>
              log.warning(s"Getting products failed: $ex")
          }
      }

    case GetBalance =>
      val replyTo: ActorRef = sender
      replyTo ! GetBalanceResponse(balance)

  }
}
