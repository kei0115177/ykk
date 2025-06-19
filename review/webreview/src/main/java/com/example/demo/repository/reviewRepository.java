package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.review;

public interface reviewRepository extends JpaRepository<review, Long> {
}
