package gql.playground.models;

import gql.playground.enums.PathType;

public class Fader {
  final String id;
  final String label;
  Boolean isAccessed;
  final PathType type;

  public Fader(String id, String name, PathType type) {
      this.id = id;
      this.label = name;
      this.type = type;
  }

  public String getId() {
    return id;
  }
}
