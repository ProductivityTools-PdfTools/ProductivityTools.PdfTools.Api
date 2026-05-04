package top.productivitytools.PdfTools.api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {
    @GetMapping("/hello-world")
    public HelloResponse helloQuery() {
        return new HelloResponse("Hello World");
    }

    @PostMapping("/hello")
    public HelloResponse Hello(@RequestBody HelloRequest request) {
        return new HelloResponse("Hello " + request.name());
    }

    public record HelloRequest(String name) {}
    public record HelloResponse(String message) {}
}
