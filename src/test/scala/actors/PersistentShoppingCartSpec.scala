package actors

import actors.PersistentShoppingCart.Command.GetProducts
import actors.PersistentShoppingCart.Response.GetProductsResponse
import domain.Product
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable

class PersistentShoppingCartSpec extends TestKit(ActorSystem("PersistentShoppingCartSpec", ConfigFactory.load().getConfig("cassandra")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import PersistentShoppingCart._
  "A shopping cart actor" should {
    val shoppingCart = system.actorOf(Props(new PersistentShoppingCart("testActor")))
    "add new product" in {
      ???
    }
    "change quantity of existing product" in {
      ???
    }
    "reply with map of products" in {
      val message = GetProducts("")
      val products: mutable.Map[Product, Int] = mutable.Map[Product, Int]()

      shoppingCart ! message

      expectMsg(GetProductsResponse(products))
    }
    "reply with inventory and clear it" in {

    }
  }

  /*
    AddProduct:
    - persist
    - add new product or change quantity if product exists
    - reply with current quantity

    BuyProducts:
    - persist
    - clear inventory
    - reply with map of products

    GetProducts:
    - reply with map of products
   */
}
