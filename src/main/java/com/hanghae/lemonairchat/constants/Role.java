package com.hanghae.lemonairchat.constants;

public enum Role {
	NOT_LOGIN(RoleString.NOT_LOGIN),
	MEMBER(RoleString.MEMBER),
	ADMIN(RoleString.ADMIN);

	private final String role;

	Role(String role) {
		this.role = role;
	}
	public String getRole(){
		return this.role;
	}
	public static class RoleString{
		public static final String NOT_LOGIN = "not_login";
		public static final String MEMBER = "member";
		public static final String ADMIN = "admin";
	}

}
