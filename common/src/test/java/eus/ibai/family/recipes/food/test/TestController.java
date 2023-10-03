package eus.ibai.family.recipes.food.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping
    public void respondOkRoot() {
        log.debug("Reached root path of the test controller!!");
    }

    @GetMapping("/open")
    public void respondOkOpen() {
        log.debug("Reached open path of the test controller!!");
    }

    @GetMapping("/protected")
    public void respondOkNotOpen() {
        log.debug("Reached not open path of the test controller!!");
    }

    @GetMapping("/admin")
    public void respondOkAdmin() {
        log.debug("Reached admin path of the test controller!!");
    }
}
