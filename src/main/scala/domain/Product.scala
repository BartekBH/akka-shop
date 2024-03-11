package domain

import spray.json.DefaultJsonProtocol

case class Product(name: String, category: String, price: BigDecimal, quantity: Int)

trait ProductStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val productFormat = jsonFormat4(Product)
}
