package com.example.starter;

import java.util.HashMap;

public class Country
{
	  private String name;
	  private HashMap<String, Integer> cityes = new HashMap<String, Integer>();
	  
	  Country(String name)
	  {
		  this.name = name;
	  }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HashMap<String, Integer> getCityes() {
		return cityes;
	}

	public void setCityes(HashMap<String, Integer> cityes) {
		this.cityes = cityes;
	}
}
