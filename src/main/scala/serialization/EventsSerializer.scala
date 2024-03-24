package serialization

import actors.PersistentShoppingCart.Event._
import actors.Shop._
import akka.serialization.SerializerWithStringManifest
import domain.Product

trait Serializable

class EventsSerializer extends SerializerWithStringManifest {

  val SEPARATOR = "//"
  val ProductAddedManifest = "ProductAdded"
  val ProductsBoughtManifest = "ProductsBought"
  val ShoppingCartCreatedManifest = "ShoppingCartCreated"
  override def identifier: Int = 101

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case ProductAdded(product, quantity) => s"[${product.name}$SEPARATOR${product.category}$SEPARATOR${product.price}$SEPARATOR$quantity$SEPARATOR]".getBytes()
      case ProductsBought(products) => s"[$products]".getBytes()
      case ShoppingCartCreated(id) => s"[$id]".getBytes()
      case _ => throw new IllegalArgumentException("Not supported event")
    }
  }

  override def manifest(o: AnyRef): String = {
    o match {
      case ProductAdded => ProductAddedManifest
      case ProductsBought => ProductsBoughtManifest
      case ShoppingCartCreated => ShoppingCartCreatedManifest
      case _ => throw new IllegalArgumentException("Not supported event")
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef ={
    val string = new String(bytes)
    val values = string.substring(1, string.length() - 1).split(SEPARATOR)

    manifest match {
      case ProductAddedManifest =>
        val name = values(0)
        val category = values(1)
        val price = values(2)
        val quantity = values(3).toInt
        val product = Product(name, category, BigDecimal(price))
        ProductAdded(product, quantity)

      case ShoppingCartCreatedManifest =>
        val id = values(0)
        ShoppingCartCreated(id)

      // TODO case ProductsBoughtManifest => ???
    }
  }
}
