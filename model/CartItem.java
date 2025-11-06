package model;


public class CartItem {
    public int productId;
    public String name;
    public double price;
    public String image;
    public int qty;

    public CartItem() {
    	
    }

    public CartItem(int productId, String name, double price, String image, int qty) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.image = image;
        this.qty = qty;
    }

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public int getQty() {
		return qty;
	}

	public void setQty(int qty) {
		this.qty = qty;
	}
}
