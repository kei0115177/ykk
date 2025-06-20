package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.Entity.reviewEntity;
import com.example.demo.repository.reviewRepository;

@Controller
public class reviewController {

    private final reviewRepository reviewRepository;

    public reviewController(reviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/reviews")
    public String showReviewList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        boolean isSearched = category != null || sentiment != null || fromDate != null || toDate != null;
        model.addAttribute("isSearched", isSearched);

        if (!isSearched) {
            model.addAttribute("reviews", null);
            return "reviewList";
        }

        List<reviewEntity> reviews = reviewRepository.findAll();

        if (StringUtils.hasText(category)) {
            reviews = reviews.stream()
                    .filter(r -> category.equalsIgnoreCase(r.getCategory()))
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(sentiment)) {
            reviews = reviews.stream()
                    .filter(r -> sentiment.equalsIgnoreCase(r.getSentiment()))
                    .collect(Collectors.toList());
        }

        if (fromDate != null) {
            reviews = reviews.stream()
                    .filter(r -> !r.getDate().isBefore(fromDate))
                    .collect(Collectors.toList());
        }

        if (toDate != null) {
            reviews = reviews.stream()
                    .filter(r -> !r.getDate().isAfter(toDate))
                    .collect(Collectors.toList());
        }

        model.addAttribute("reviews", reviews);
        return "reviewList";
    }

    @PostMapping("/reviews/delete/{id}")
    public String deleteReview(@PathVariable Long id) {
        reviewRepository.deleteById(id);
        return "redirect:/reviews";
    }

    @GetMapping("/reviewForm")
    public String showForm(Model model) {
        model.addAttribute("review", new reviewEntity());
        return "reviewForm";
    }

    @PostMapping("/reviewForm")
    public String submitForm(@ModelAttribute reviewEntity review) {

        review.setSentiment("");           // 将来Azureで埋める
        review.setCategory("");            // 将来自動分類で埋める
        review.setDate(LocalDate.now());   // 現在日付を設定

        reviewRepository.save(review);
        return "reviewForm"; 
    }
    
}
