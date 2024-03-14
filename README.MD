### Available Routes

#### Creating new shopping cart
```shell
curl -v -X POST "http://localhost:8080/shop/shopping-cart"
```
### Adding product to shopping cart
```shell
curl -v -X PUT "http://localhost:8080/shopping-cart/products?id=d634b8df-5e2b-4e1b-b788-ab00f3fefd98&quantity=5"\
    -H 'Content-Type: application/json'\
    -d '{"name":"Steering wheel", "category":"Car parts", "price":"370.00"}'
```
### Buying shopping cart's inventory
```shell
curl -v -X PUT "http://localhost:8080/shopping-cart/buy?id=d634b8df-5e2b-4e1b-b788-ab00f3fefd98"
```
### Getting shopping cart's inventory
```shell
curl -v "http://localhost:8080/shopping-cart/products?id=d634b8df-5e2b-4e1b-b788-ab00f3fefd98"
```
### Getting balance
```shell
curl -v "http://localhost:8080/shop/balance"

```
