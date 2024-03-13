package domain

import spray.json._

case class Product(name: String, category: String, price: BigDecimal)

trait ShopJsonProtocol extends DefaultJsonProtocol {
  implicit val productsFormat: JsonFormat[Product] = jsonFormat3(Product)
}
