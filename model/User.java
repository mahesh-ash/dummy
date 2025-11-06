package model;

public class User {

     private int id;
	 private String name;
	 private String mobile;
	 private String email;
	 private String address;
	 private int pinCode;
	 private String password;
	 private int status ;
	 
	 public User() {
		 
	 }

	public User( String name, String mobile, String email, String address, int pinCode, String password) {
		this.name = name;
		this.mobile = mobile;
		this.email = email;
		this.address = address;
		this.pinCode = pinCode;
		this.password = password;
	}
	public User(int id, String name, String mobile, String email, String address, int pinCode, String password) {
		this.id=id;
		this.name = name;
		this.mobile = mobile;
		this.email = email;
		this.address = address;
		this.pinCode = pinCode;
		this.password = password;
	}
	public User(int id, String name, String mobile, String email, String address, int pinCode, String password,int status) {
		this.id=id;
		this.name = name;
		this.mobile = mobile;
		this.email = email;
		this.address = address;
		this.pinCode = pinCode;
		this.password = password;
		this.status=status;
	}






	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPinCode() {
		return pinCode;
	}

	public void setPinCode(int pinCode) {
		this.pinCode = pinCode;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	

	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}



	

}
