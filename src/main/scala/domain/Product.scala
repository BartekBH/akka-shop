package domain

import spray.json._

case class Product(name: String, category: String, price: BigDecimal, quantity: Int)

trait ShopJsonProtocol extends DefaultJsonProtocol {
  implicit val productFormat: JsonFormat[Product] = jsonFormat4(Product)
}
