package com.challenge.controller;

import com.challenge.dto.ComparisonDTO;
import com.challenge.dto.ComparisonDiffDTO;
import com.challenge.dto.ComparisonRequestDTO;
import com.challenge.service.ComparisonService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping
    public ComparisonDTO compare(@Valid @RequestBody ComparisonRequestDTO request) {
        return comparisonService.compare(request);
    }

    @PostMapping("/diff")
    public ComparisonDiffDTO diff(@Valid @RequestBody ComparisonRequestDTO request) {
        return comparisonService.diff(request);
    }
}
