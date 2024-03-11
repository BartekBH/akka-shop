package actors

import actors.ShoppingCart.{AddProduct, DeleteProduct}
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
  case class CreateProduct(product: Product, price: BigDecimal)
  case class Buy(shoppingCartId: String)
  case class FindProduct(productId: String)
  case object FindAllProducts
  case class AddProductToSC(shoppingCartId: String, productId: String, quantity: Int)
  case class DeleteProductFromSC(shoppingCartId: String, productId: String, quantity: Int)

  trait Response
  case class ShoppingCartCreated(shoppingCartId: String) extends Response
  case class ProductCreated(productId: String) extends Response
  case class ProductFound(product: Product) extends Response
  case class ProductsFound(products: List[Product]) extends Response
  case object ProductAdded extends Response
  case object ProductDeleted extends Response
  case object Bought extends Response
}

class Shop extends Actor with ActorLogging {
  import Shop._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(2 seconds)


  var shoppingCarts: Map[String, ActorRef] = Map()
  var balance: BigDecimal = BigDecimal("0")
  var products: Map[String, Product] = Map()

  override def receive: Receive = {
    case CreateShoppingCart =>
      val id = UUID.randomUUID().toString
      val newShoppingCart = context.actorOf(Props[ShoppingCart], id)
      shoppingCarts + (id -> newShoppingCart)
      sender() ! ShoppingCartCreated(id)
      log.info(s"Shopping cart created with id: $id")

    case CreateProduct(product, price) =>
      val id = UUID.randomUUID().toString
      products += (id -> product)
      sender() ! ProductCreated(id)
      log.info(s"Product created with id: $id")

    case Buy(shoppingCartId) =>
      val buyFuture: Future[Map[String, Int]] = (shoppingCarts(shoppingCartId) ? Buy).mapTo[Map[String, Int]]
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

            sender() ! Bought
            log.info("Buying finished")
          }
        case Failure(ex) =>
          log.warning("Buying failed")
      }


    case FindProduct(id) =>
      val product: Product = products(id)
      sender() ! ProductFound(product)
      log.info(s"Product with id $id found")

    case FindAllProducts =>
      val productsList: List[Product] = products.values.toList
      sender() ! ProductsFound(productsList)

    case AddProductToSC(shoppingCartId, productId, quantity) =>
      val addFuture: Future[Int] = (shoppingCarts(shoppingCartId) ? AddProduct(productId, quantity)).mapTo[Int]
      addFuture.onComplete {
        case Success(q) =>
          log.info(s"Product added successfully to shopping card with od: $shoppingCartId")
          sender() ! ProductAdded

        case Failure(ex) =>
          log.warning("Buying failed")
      }

    case DeleteProductFromSC(shoppingCartId, productId, quantity) =>
      val deleteFuture: Future[Int] = (shoppingCarts(shoppingCartId) ? DeleteProduct(productId, quantity)).mapTo[Int]
      deleteFuture.onComplete {
        case Success(q) =>
          log.info(s"Product deleted successfully from shopping card with od: $shoppingCartId")
          sender() ! ProductDeleted

        case Failure(ex) =>
          log.warning("Buying failed")
      }
  }
}
