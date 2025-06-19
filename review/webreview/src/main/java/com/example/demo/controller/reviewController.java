package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.Entity.review;
import com.example.demo.repository.reviewRepository;


@Controller
public class reviewController {

    private final reviewRepository reviewRepository;

    public reviewController(reviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/reviews")
    public String showReviewList(Model model) {
        List<review> reviews = reviewRepository.findAll();
        model.addAttribute("reviews", reviews); // ← ここが重要！
        return "reviewList";
    }
}