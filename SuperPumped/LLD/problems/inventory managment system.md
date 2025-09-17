
class product 
string sku, name,
int price, quantity, threshold
productcategory pg


warehouse

```
private int id;
    private String name;
    private String location;
    private Map<String, Product> products; // SKU -> Product	
```



challenges 
handling multiple inventory and large number of products
multiple simultanous operations 
accurate inventory product  count
Handling product returns, damaged inventory, and seasonal demand fluctuations.

singleton pattern for inventory manager
observer design pattern for alerts 
factory for product creation 
strategy for replishment
state managment with enums 

##### FACTORY

public abstract class Product {
    private String sku;
    private String name;
    private double price;
    private int quantity;
    private int threshold;
    private ProductCategory category;

    public Product(String sku, String name, double price, int quantity, int threshold, ProductCategory category) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.threshold = threshold;
        this.category = category;
    }

    }
    public class ClothingProduct extends Product {
    private String size;
    private String color;

    public ClothingProduct(String sku, String name, double price, int quantity, int threshold, String size, String color) {
        super(sku, name, price, quantity, threshold, ProductCategory.CLOTHING);
        this.size = size;
        this.color = color;
    }

    // Getters and setters...
}

public class produxtfactory{{
	public product createproduct(Productcategoru pg){
		switch(category){
			case Elec:
				return new elcprod("""----all values tobeset""""); 
			}
	}
}}

##### STARTEGY 

public interface replishmenty stractegy {
	void replenish(Product p){
	}
}

```
public class JustInTimeStrategy implements ReplenishmentStrategy {
    @Override
    public void replenish(Product product) {
        // Implement Just-In-Time replenishment logic
        System.out.println("Applying Just-In-Time replenishment for " + product.getName());
        // Calculate optimal order quantity based on demand rate
    }
}
```




##### SINGLETON

public class inventorymanager{

List<warehouse> warehouses;
private ProductFactory productFactory;
private ReplenishmentStrategy replenishmentStrategy;


	private inventorymanager(){
	}
	private static inventorymanager invmanager ;
	
	 public static  instancemanager getmanager(){
	 if instance == null{
	 synchronzed(Inventorymanager.class){
	 if(instance == null){instyance = new instancemanager ()}
	 }
	  
	 }
	
	 return instance;
	 }
}


