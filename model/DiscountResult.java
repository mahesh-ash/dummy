package model;

public class DiscountResult {
    private boolean valid;
    private String message;
    private double discountAmount;
    private double newAmount;
    private int couponId;

    public DiscountResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getNewAmount() { return newAmount; }
    public void setNewAmount(double newAmount) { this.newAmount = newAmount; }

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }
}
