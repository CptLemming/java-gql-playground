package gql.playground.models;

import gql.playground.enums.ProductType;

public class Product {
  final String id;
  final String name;
  Float cost;
  Float tax;
  final ProductType type;

  public Product(String id, String name, ProductType type) {
      this.id = id;
      this.name = name;
      this.type = type;
  }

  public String getId() {
    return id;
  }
}
