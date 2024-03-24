package actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.postfixOps

class ShopSpec extends TestKit(ActorSystem("ShopSpec", ConfigFactory.load().getConfig("cassandra")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import Shop._
  "A shop actor" should {
    val shop = system.actorOf(Props[Shop])

    "create new shopping cart" in {
      shop ! CreateShoppingCart

      expectMsgType[ShoppingCartCreatedResponse]
    }
    "add product to shopping cart" in {
      ???
    }
    "reply with cost of shopping cart's inventory" in {
      ???
    }
    "reply with map of products" in {
      ???
    }
    "reply with current balance" in {
      ???
    }
  }

  /*
    CreateShoppingCart
    - persist
    - create new PersistentShoppingCart actor and add to map
    - reply with id of new actor

    AddProduct:
    - pass command to child
    - response to API on success
    - log warning on failure
    - send response if shopping cart actor don't exists

    BuyProducts:
    - pass command to child
    - change balance amount
    - response with cost of shopping cart's inventory on success
    - log warning on failure
    - send response if shopping cart actor don't exists

    GetProducts:
    - pass command to child
    - get response with map of products (or log warning on failure)
    - reply with map of products
    - reply if shopping cart actor don't exists

    GetBalance:
    - reply with current balance
   */

}
