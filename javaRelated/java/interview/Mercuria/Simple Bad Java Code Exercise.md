# Bad Code Exercise

```
import java.time.LocalTime;
import java.util.logging.Logger;

public class Customer {

    private static Logger logger = Logger.getLogger(Customer.class.getName());

    public void placeOrder(String orderReference, OrderedProductData[] orderedProductData) {

        if (orderReference == "") {
            throw new RuntimeException();
        }

        orderReference.trim();

        Order newOrder = new Order(orderReference);

        newOrder.customer = this;

        int i = 0;

        for (OrderedProductData dataItem : orderedProductData) {
            Product product = SqlConnection.findProduct(dataItem.productId);
            newOrder.addOrderItem(product);
            ++i;
        }

        logger.info("A new order has been placed: " + orderReference + " at " + LocalTime.now());

        CommunicationService.sendMail("New Order!",
                "A new order has been placed: " + orderReference + " at " + LocalTime.now(),
                "ordernotifications@mycompany.com", "orders@mycompany.com");
    }

}
```
