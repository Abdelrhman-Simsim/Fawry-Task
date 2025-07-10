import java.util.*;

// ============================ Product ============================
abstract class Product {
    protected String name;
    protected double price;
    protected int quantity;

    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public abstract boolean isExpired();
    public abstract boolean requiresShipping();
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void reduceQuantity(int qty) { this.quantity -= qty; }
}

// ============================ Shippable ============================
interface Shippable {
    String getName();
    double getWeight();
}

// ============================ Cheese (Expirable + Shippable) ============================
class Cheese extends Product implements Shippable {
    private Date expiryDate;
    private double weightPerUnit;

    public Cheese(String name, double price, int quantity, Date expiryDate, double weightPerUnit) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
        this.weightPerUnit = weightPerUnit;
    }

    @Override
    public boolean isExpired() {
        return new Date().after(expiryDate);
    }

    @Override
    public boolean requiresShipping() {
        return true;
    }

    @Override
    public double getWeight() {
        return weightPerUnit;
    }
}

// ============================ Biscuits ============================
class Biscuits extends Cheese {
    public Biscuits(String name, double price, int quantity, Date expiryDate, double weightPerUnit) {
        super(name, price, quantity, expiryDate, weightPerUnit);
    }
}

// ============================ TV (Non-Expirable + Shippable) ============================
class TV extends Product implements Shippable {
    private double weight;

    public TV(String name, double price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }

    @Override
    public boolean isExpired() { return false; }
    @Override
    public boolean requiresShipping() { return true; }
    @Override
    public double getWeight() { return weight; }
}

// ============================ Mobile Scratch Card ============================
class MobileScratchCard extends Product {
    public MobileScratchCard(String name, double price, int quantity) {
        super(name, price, quantity);
    }

    @Override
    public boolean isExpired() { return false; }
    @Override
    public boolean requiresShipping() { return false; }
}

// ============================ Customer ============================
class Customer {
    private String name;
    private double balance;

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public double getBalance() { return balance; }
    public void deduct(double amount) { balance -= amount; }
}

// ============================ Cart ============================
class Cart {
    static class CartItem {
        Product product;
        int quantity;
        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }

    private List<CartItem> items = new ArrayList<>();

    public void add(Product product, int qty) {
        if (qty > product.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock for " + product.getName());
        }
        items.add(new CartItem(product, qty));
    }

    public List<CartItem> getItems() { return items; }
    public boolean isEmpty() { return items.isEmpty(); }
}

// ============================ Shipping Service ============================
class ShippingService {
    public static void ship(List<Shippable> items, List<Integer> quantities) {
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        for (int i = 0; i < items.size(); i++) {
            Shippable s = items.get(i);
            double weight = s.getWeight() * quantities.get(i);
            totalWeight += weight;
            System.out.printf("%dx %-10s %.0fg\n", quantities.get(i), s.getName(), weight * 1000);
        }
        System.out.printf("Total package weight %.1fkg\n", totalWeight);
        System.out.println();
    }
}

// ============================ Checkout ============================
class CheckoutService {
    private static final double SHIPPING_COST = 30;

    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            System.err.println("Error: Cart is empty.");
            return;
        }

        double subtotal = 0;
        double totalShipping = 0;
        List<Shippable> shippingItems = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (Cart.CartItem item : cart.getItems()) {
            Product product = item.product;
            int qty = item.quantity;

            if (product.isExpired()) {
                System.err.println("Error: Product " + product.getName() + " is expired.");
                return;
            }

            if (qty > product.getQuantity()) {
                System.err.println("Error: Not enough quantity for " + product.getName());
                return;
            }

            subtotal += product.getPrice() * qty;

            if (product.requiresShipping() && product instanceof Shippable) {
                shippingItems.add((Shippable) product);
                quantities.add(qty);
            }
        }

        totalShipping = shippingItems.isEmpty() ? 0 : SHIPPING_COST;
        double total = subtotal + totalShipping;

        if (customer.getBalance() < total) {
            System.err.println("Error: Insufficient balance.");
            return;
        }

        // Reduce product quantities
        for (Cart.CartItem item : cart.getItems()) {
            item.product.reduceQuantity(item.quantity);
        }

        // Deduct from customer
        customer.deduct(total);

        // Ship
        if (!shippingItems.isEmpty()) {
            ShippingService.ship(shippingItems, quantities);
        }

        // Print receipt
        System.out.println("** Checkout receipt **");
        for (Cart.CartItem item : cart.getItems()) {
            System.out.printf("%dx %-10s %.0f\n", item.quantity, item.product.getName(), item.product.getPrice() * item.quantity);
        }
        System.out.println("----------------------");
        System.out.printf("Subtotal         %.0f\n", subtotal);
        System.out.printf("Shipping         %.0f\n", totalShipping);
        System.out.printf("Amount           %.0f\n", total);
        System.out.printf("Balance left     %.0f\n", customer.getBalance());
    }
}
