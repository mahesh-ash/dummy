// model/Paymentrequest.java
package model;

import java.util.List;

public class Paymentrequest {
    private Double amount;
    private Double originalAmount;
    private List<Integer> selectedItems;
    private String method;
    private String couponCode;
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public Double getOriginalAmount() {
		return originalAmount;
	}
	public void setOriginalAmount(Double originalAmount) {
		this.originalAmount = originalAmount;
	}
	public List<Integer> getSelectedItems() {
		return selectedItems;
	}
	public void setSelectedItems(List<Integer> selectedItems) {
		this.selectedItems = selectedItems;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getCouponCode() {
		return couponCode;
	}
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}
  
    
    
}
