package nl.bldn.housedata

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class UiController {
    @GetMapping
    fun showGraphPage(): String = "show-graph"
}