package actors

import akka.actor.{Actor, ActorLogging}

import scala.collection.mutable

object ShoppingCart {
  // messages
  case class AddProduct(productId: String, quantity: Int)
  case class DeleteProduct(productId: String, quantity: Int)
  case object Buy

//  trait Response
//  case object ProductAdded extends Response
//  case object ProductDeleted extends Response
}

class ShoppingCart extends Actor with ActorLogging {
  import ShoppingCart._

  var products: mutable.Map[String, Int] = mutable.Map[String, Int]() // [productId, quantity]

  override def receive: Receive = {
    case AddProduct(productId, quantity) =>
      if (products.contains(productId)) {
        val newProductQuantity = products(productId) + quantity
        products(productId) = newProductQuantity
      } else {
        products += (productId -> quantity)
      }
      sender() ! quantity

    case DeleteProduct(productId, quantity) =>
      // TODO prevent deleting more than exists
      val crrQuantity: Option[Int] = products.get(productId)
      val newQuantity: Option[Int] = crrQuantity.map(_ - quantity)

      newQuantity.foreach(q => products = products + (productId -> q))
      sender() ! newQuantity


    case Buy =>
      sender() ! products
  }
}
