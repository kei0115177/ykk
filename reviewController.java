package com.example.demo.controller;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.Entity.review;
import com.example.demo.repository.reviewRepository;
import com.example.demo.service.ProductService;

/**
 * レビュー管理用コントローラー。
 * レビュー一覧の表示、検索、削除などの機能を提供する。
 */
@Controller
public class reviewController {

    private final reviewRepository reviewRepository;
    private final ProductService productService;


    /**
     * コンストラクタによる依存性注入。
     *
     * @param reviewRepository レビューリポジトリ
     * @param productService 商品サービス
     */

    public reviewController(reviewRepository reviewRepository, ProductService productService) {
        this.reviewRepository = reviewRepository;
        this.productService = productService;
    }
    
    
    /**
     * メソッド名:showSearchForm
     *初期表示ページの表示（検索リセット後の状態）。
     * @param model モデル
     * @return レビュー一覧画面（検索前）
     */
    @GetMapping("/reviewListf")
    public String showSearchForm(Model model) {
        // カテゴリなどを渡したければここで渡す
        return "reviewListf";
    }

    
    /**
     * レビュー一覧の表示と検索機能。
     * 検索条件（カテゴリ、商品、感情、キーワード、日付範囲）を指定可能。
     * またページングおよび統計データ（感情×性別、感情×年代）を含む。
     *
     * @param category   商品カテゴリ
     * @param product    商品名（複数）
     * @param sentiment  感情（positive, neutral, negative）
     * @param keyword    レビュー内検索キーワード
     * @param page       ページ番号（0から）
     * @param size       1ページあたりの表示件数
     * @param fromDate   開始日（ISO形式）
     * @param toDate     終了日（ISO形式）
     * @param model      モデル
     * @return レビュー一覧ページ（検索後）
     */
	@GetMapping("/reviews")
	public String showReviewList(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) List<String> product,
			@RequestParam(required = false) String sentiment,
			@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, // ★編集点★
            @RequestParam(defaultValue = "10") int size, // ★編集点★
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			Model model) {

		//条件なしで検索ボタンを押すと全件取得
		boolean isSearched = (StringUtils.hasText(category) && !category.equals("all"))
			    || (product != null && !product.isEmpty())
			    || (StringUtils.hasText(sentiment) && !sentiment.equals("all"))
			    || StringUtils.hasText(keyword)
			    || fromDate != null
			    || toDate != null;



			// 全件表示のため、条件が空でもisSearchedをtrueにする
			if (!isSearched) {
			    isSearched = true; // 全件表示として処理
			}

			model.addAttribute("isSearched", isSearched);

		// 全レビュー取得　グラフ用　※ページングは別処理
        List<review> reviews = reviewRepository.findAll();
        
        
        
		// フィルター処理
        if (StringUtils.hasText(category) && !category.equals("all")) {
        	reviews = reviews.stream()
        			.filter(r -> category.equalsIgnoreCase(r.getCategory()))
        			.collect(Collectors.toList());

		}
		if (product != null && !product.isEmpty()) {
		    reviews = reviews.stream()
		        .filter(r -> product.contains(r.getProduct()))
		        .collect(Collectors.toList());
		}

		
		if (StringUtils.hasText(sentiment) && !sentiment.equals("all")) {
			reviews = reviews.stream()
					.filter(r -> sentiment.equalsIgnoreCase(r.getSentiment()))
					.collect(Collectors.toList());
		}

		if (fromDate != null) {
			reviews = reviews.stream()
					.filter(r -> r.getDate() != null && !r.getDate().isBefore(fromDate))
					.collect(Collectors.toList());
		}

		if (toDate != null) {
			reviews = reviews.stream()
					.filter(r -> r.getDate() != null && !r.getDate().isAfter(toDate))
					.collect(Collectors.toList());
		}

		if (StringUtils.hasText(keyword)) {
			String lowerKeyword = keyword.toLowerCase();
			reviews = reviews.stream()
					.filter(r -> r.getReview() != null && r.getReview().toLowerCase().contains(lowerKeyword))
					.collect(Collectors.toList());
		}
		
        // ★編集点★ ページングに切り替え
        int start = page * size;
        int end = Math.min(start + size, reviews.size());
        List<review> subList = (start < end) ? reviews.subList(start, end) : List.of();
        Page<review> reviewPage = new PageImpl<>(subList, PageRequest.of(page, size),reviews.size());
		model.addAttribute("reviews", subList);
		model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("currentPage", page); // ★編集点★
        model.addAttribute("totalPages", (reviews.size() + size - 1) / size); // ★編集点★
        model.addAttribute("page", reviewPage); // ★変更
        model.addAttribute("reviews", reviewPage.getContent());
        
        int maxDisplayPages = 5;
        int totalPages = reviewPage.getTotalPages();
        int currentPage = reviewPage.getNumber();

        int startPage = Math.max(0, currentPage - maxDisplayPages / 2);
        int endPage = Math.min(totalPages - 1, startPage + maxDisplayPages - 1);
        if (endPage - startPage < maxDisplayPages - 1) {
            startPage = Math.max(0, endPage - maxDisplayPages + 1);
        }

        List<Integer> displayPages = new ArrayList<>();
        for (int i = startPage; i <= endPage; i++) {
            displayPages.add(i);
        }

        model.addAttribute("displayPages", displayPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

		// グラフ用：感情を小文字に正規化して件数集計
		Map<String, Long> sentimentCountsMap = reviews.stream()
				.filter(r -> r.getSentiment() != null)
				.collect(Collectors.groupingBy(
						r -> r.getSentiment().toLowerCase(),
						Collectors.counting()));

		List<Long> sentimentCountList = List.of(
				sentimentCountsMap.getOrDefault("positive", 0L),
				sentimentCountsMap.getOrDefault("neutral", 0L),
				sentimentCountsMap.getOrDefault("negative", 0L));
		model.addAttribute("sentimentCounts", sentimentCountList);

		// 性別ごとの感情件数（昇順）
		Map<String, Map<String, Long>> genderSentimentMap = reviews.stream()
				.filter(r -> r.getSentiment() != null)
				.collect(Collectors.groupingBy(
						r -> r.getGender() != null ? r.getGender().toLowerCase() : "unknown",
						TreeMap::new,
						Collectors.groupingBy(
								r -> r.getSentiment().toLowerCase(),
								Collectors.counting())));

		// 年代順定義
		Map<String, Integer> ageOrder = Map.of(
				"10代以下", 1,
				"20代", 2,
				"30代", 3,
				"40代", 4,
				"50代", 5,
				"60代", 6,
				"70代", 7,
				"80代以上", 8);

		// 年代ごとの感情件数（年代昇順に並べる）
		Map<String, Map<String, Long>> tempAgeMap = reviews.stream()
				.filter(r -> r.getSentiment() != null)
				.collect(Collectors.groupingBy(
						r -> {
							int age = r.getAge() != null ? r.getAge() : 0;
							if (age < 20)
								return "10代以下";
							else if (age < 30)
								return "20代";
							else if (age < 40)
								return "30代";
							else if (age < 50)
								return "40代";
							else if (age < 60)
								return "50代";
							else if (age < 70)
								return "60代";
							else if (age < 80)
								return "70代";
							else
								return "80代以上";
						},
						Collectors.groupingBy(
								r -> r.getSentiment().toLowerCase(),
								Collectors.counting())));

		Map<String, Map<String, Long>> ageGroupSentimentMap = new LinkedHashMap<>();
		tempAgeMap.entrySet().stream()
				.sorted(Map.Entry.comparingByKey((a, b) -> ageOrder.getOrDefault(a, 99) - ageOrder.getOrDefault(b, 99)))
				.forEachOrdered(e -> ageGroupSentimentMap.put(e.getKey(), e.getValue()));

		model.addAttribute("genderSentimentMap", genderSentimentMap);
		model.addAttribute("ageGroupSentimentMap", ageGroupSentimentMap);

		return "reviewList";
	}


    /**
     *メソッド名:deleteSelected
     * チェックボックスで選択されたレビューを一括削除。
     * @param ids 削除対象のレビューIDリスト（null の場合は削除しない）
     * @return 初期レビュー一覧画面へのリダイレクト
     */
	@PostMapping("/reviews/delete")
	public String deleteSelected(@RequestParam(value = "selectedIds", required = false) List<Long> ids) {
		if (ids != null && !ids.isEmpty()) {
			ids.forEach(reviewRepository::deleteById);
		}
		return "redirect:/reviewListf";
	}
}