package gql.playground;

import java.io.InputStream;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class Schema {
  final GraphQLSchema schema;

  public Schema(RuntimeWiring runtimeWiring) {
    this.schema = build(runtimeWiring);
  }

  private GraphQLSchema build(RuntimeWiring runtimeWiring) {
    InputStream schema = App.class.getClassLoader().getResourceAsStream("schema.graphql");

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
  }

  public GraphQLSchema getSchema() {
    return schema;
  }
}
