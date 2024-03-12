package actors

import actors.ShoppingCart.{AddProduct, Buy, DeleteProduct}
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
  // messages
  case object CreateShoppingCart
  case class CreateProduct(product: Product)
  case class BuySC(shoppingCartId: String)
  case class FindProduct(productId: String)
  case object FindAllProducts
  case class AddProductToSC(shoppingCartId: String, productId: String, quantity: Int)
  case class DeleteProductFromSC(shoppingCartId: String, productId: String, quantity: Int)

  trait Response
  case class ShoppingCartCreated(shoppingCartId: String) extends Response
  case class ProductCreated(productId: String) extends Response
  case class ProductAdded(quantity: Int) extends Response
  case class ProductDeleted(quantity: Int) extends Response
  case class Bought(amount: BigDecimal) extends Response
}

class Shop extends Actor with ActorLogging {
  import Shop._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(2 seconds)


  var shoppingCarts: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
  var balance: BigDecimal = BigDecimal("0")
  var products: Map[String, Product] = Map()

  override def receive: Receive = {
    case CreateShoppingCart =>
      val id = UUID.randomUUID().toString
      val newShoppingCart = context.actorOf(Props[ShoppingCart], id)
      shoppingCarts += (id -> newShoppingCart)
      sender() ! ShoppingCartCreated(id)
      log.info(s"Shopping cart created with id: $id")

    case CreateProduct(product) =>
      val id = UUID.randomUUID().toString
      products += (id -> product)
      sender() ! ProductCreated(id)
      log.info(s"Product created with id: $id")

    case BuySC(shoppingCartId) =>
      val replyTo = sender
      val currentBalance = balance
      val buyFuture: Future[mutable.Map[String, Int]] = (shoppingCarts(shoppingCartId) ? Buy).mapTo[mutable.Map[String, Int]]
      buyFuture.onComplete {
        case Success(v) =>
          v.foreach { p: (String, Int) =>
            val productId: String = p._1

            val quantity: Int = p._2
            val product: Option[Product] = products.get(productId)
            val newProduct: Option[Product] = product.map {
              case Product(name, category, price, q) => Product(name, category, price, q + quantity)
            }
            newProduct.foreach(product => products = products + (productId -> product))

            val price: Option[BigDecimal] = product.map {
              case Product(_, _, price, _) => price
            }

            val amount: BigDecimal = price.get * quantity
            balance += amount
          }
          replyTo ! Bought(balance - currentBalance)
          log.info("Buying finished")
        case Failure(ex) =>
          log.warning("Buying failed")
      }


    case FindProduct(id) =>
      val product: Option[Product] = products.get(id)
      sender() ! product
      log.info(s"Product with id $id found")

    case FindAllProducts =>
      val productsList: List[Product] = products.values.toList
      sender() ! productsList

    case AddProductToSC(shoppingCartId, productId, quantity) =>
      val replyTo = sender
      val addFuture: Future[Int] = (shoppingCarts(shoppingCartId) ? AddProduct(productId, quantity)).mapTo[Int]
      addFuture.onComplete {
        case Success(q) =>
          log.info(s"Product added successfully to shopping cart with id: $shoppingCartId")
          replyTo ! ProductAdded(q)

        case Failure(ex) =>
          log.warning("Adding product failed")
      }

    case DeleteProductFromSC(shoppingCartId, productId, quantity) =>
      val replyTo = sender
      val deleteFuture: Future[Int] = (shoppingCarts(shoppingCartId) ? DeleteProduct(productId, quantity)).mapTo[Int]
      deleteFuture.onComplete {
        case Success(q) =>
          log.info(s"Product deleted successfully from shopping cart with id: $shoppingCartId")
          replyTo ! ProductDeleted(q)

        case Failure(ex) =>
          log.warning("Deleting product failed")
      }
  }
}
