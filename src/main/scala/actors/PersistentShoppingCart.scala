package actors

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.PersistentActor
import domain._

import java.util.UUID
import scala.collection.mutable

object PersistentShoppingCart {

  // commands = messages
  trait Command
  object Command {
    case class AddProduct(id: String, product: Product, quantity: Int) extends Command
    case class BuyProducts(id: String) extends Command
    case class GetProducts(id: String) extends Command
  }

  // events (to persist to Cassandra)
  trait Event
  object Event {
    case class ProductAdded(product: Product, quantity: Int) extends Event
    case class ProductsBought(products: mutable.Map[Product, Int])
  }

  // responses
  trait Response
  object Response {
    case class ProductAddedResponse(quantity: Int) extends Response
    case class ProductsBoughtResponse(products: mutable.Map[Product, Int]) extends Response
    case class GetProductsResponse(products: mutable.Map[Product, Int]) extends Response
  }
}

class PersistentShoppingCart(id: String) extends PersistentActor with ActorLogging {
  import PersistentShoppingCart.Command._
  import PersistentShoppingCart.Response._
  import actors.PersistentShoppingCart.Event._

  val products: mutable.Map[Product, Int] = mutable.Map[Product, Int]()

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case AddProduct(_, product, quantity) =>
      val replyTo: ActorRef = sender

      persist(ProductAdded(product, quantity)) { e =>
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
      }

    case BuyProducts(_) =>
      val replyTo: ActorRef = sender

      persist(ProductsBought(products)) { e =>
        if (products.isEmpty) {
          log.info("Shopping cart is empty")
          replyTo ! ProductsBoughtResponse(products)
        } else {
          val currentProducts = products.clone()
          products.foreach { p =>
            products.remove(p._1)
          }

          log.info("The inventory of shopping cart bought")
          replyTo ! ProductsBoughtResponse(currentProducts)
        }
      }

    case GetProducts(_) =>
      val replyTo: ActorRef = sender

      log.info("Getting products from shopping cart")
      replyTo ! GetProductsResponse(products)
  }

  override def receiveRecover: Receive = {
    case ProductAdded(product, quantity) =>
      val maybeQuantity: Option[Int] = products.get(product)
      maybeQuantity match {
        case Some(q) =>
          val newQuantity = q + quantity
          products += (product -> newQuantity)
          log.info(s"Recovered ${product.name} with quantity $quantity")

        case None =>
          products += (product -> quantity)
          log.info(s"Recovered ${product.name} with quantity $quantity")
      }
    case ProductsBought(_) =>
      products.foreach { p =>
        products.remove(p._1)
      }
      log.info("Recovered buying products")
  }
}
