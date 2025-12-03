package top.productivitytools.PdfTools.api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/debug")
    public String Hello() {
        return "Hello World";
    }
}
