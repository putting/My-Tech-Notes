# Simple Java Questions

Debug this code which has some simple syntax errors.
Perf question on querying a list of products rather than individually

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

## Other Questions

Write a routine to return 2nd largest number
int[] anArray = new int[] {10, 52, 35, 42, 18, 23, 33, 22, 2, 88, 36, 78};


### Hashmap


### SQL Design
How would you design the rlationships for the following entities:
Customer, Order, Item, Product
```
