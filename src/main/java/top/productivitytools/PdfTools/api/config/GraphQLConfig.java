package top.productivitytools.PdfTools.api.config;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(
                GraphQLScalarType.newScalar()
                        .name("Upload")
                        .description("A file part in a multipart request")
                        .coercing(new Coercing<MultipartFile, MultipartFile>() {
                            @Override
                            public MultipartFile serialize(Object dataFetcherResult) {
                                throw new CoercingSerializeException("Upload is an input-only scalar");
                            }

                            // The input value for parseValue is what comes from the variables map
                            // In Spring GraphQL, this is typically the MultipartFile object inserted by the
                            // transport
                            @Override
                            public MultipartFile parseValue(Object input) {
                                if (input instanceof MultipartFile) {
                                    return (MultipartFile) input;
                                }
                                // Sometimes it might come as null or something else depending on transport
                                // quirks
                                if (input == null) {
                                    return null;
                                }
                                throw new CoercingParseValueException(
                                        "Expected a MultipartFile, but got " + input.getClass().getName());
                            }

                            @Override
                            public MultipartFile parseLiteral(Object input) {
                                throw new CoercingParseLiteralException("Upload cannot be parsed from a literal");
                            }
                        })
                        .build());
    }
}
