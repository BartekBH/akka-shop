package actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import domain._

import scala.collection.mutable

object PersistentShoppingCart {

  // commands = messages
  trait Command
  object Command {
    case class AddProduct(id: String, product: Product, quantity: Int) extends Command
    case class BuyProducts(id: String) extends Command
    case class GetProducts(id: String) extends Command
  }

  /*
  // events (to persist to Cassandra)
  trait Event
  case class ProductAdded(product: Product, quantity: Int) extends Event
  case object ProductsBought
   */

  // responses
  trait Response
  object Response {
    case class ProductAddedResponse(quantity: Int) extends Response
    case class ProductsBoughtResponse(products: mutable.Map[Product, Int]) extends Response
    case class GetProductsResponse(products: mutable.Map[Product, Int]) extends Response
  }
}

class PersistentShoppingCart extends Actor with ActorLogging {
  import PersistentShoppingCart.Command._
  import PersistentShoppingCart.Response._

  val products: mutable.Map[Product, Int] = mutable.Map[Product, Int]()

  override def receive: Receive = {
    case AddProduct(_, product, quantity) =>
      val replyTo: ActorRef = sender
      val maybeQuantity: Option[Int] = products.get(product)
      maybeQuantity match {
        case Some(q) =>
          val newQuantity = q + quantity
          products += (product -> newQuantity)

          log.info(s"Product ${product.name} added. Current quantity: $newQuantity")
          replyTo ! ProductAddedResponse(newQuantity)

        case None =>
          products += (product -> quantity)

          log.info(s"Product ${product.name} added. Current quantity: $quantity")
          replyTo ! ProductAddedResponse(quantity)
      }

    case BuyProducts(_) =>
      val replyTo: ActorRef = sender
      if (products.isEmpty) {
        log.info("Shopping cart is empty")
        replyTo ! ProductsBoughtResponse(products)
      } else {
        val currentProducts = products
        currentProducts.foreach { p =>
          products.remove(p._1)
        }

        log.info("All products was bought")
        replyTo ! ProductsBoughtResponse(currentProducts)
      }

    case GetProducts(_) =>
      val replyTo: ActorRef = sender

      log.info("Getting products from shopping cart")
      replyTo ! GetProductsResponse(products)
  }
}
