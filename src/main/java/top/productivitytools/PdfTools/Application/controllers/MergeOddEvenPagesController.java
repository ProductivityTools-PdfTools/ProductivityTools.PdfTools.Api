package top.productivitytools.PdfTools.Application.controllers;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MergeOddEvenPagesController {

    @QueryMapping
    public String helloQuery() {
        return "Hello World";
    }

    @MutationMapping
    public String Hello(@Argument("name") String name) {
        var response = "Hello " + name;
        return response;
    }
}
